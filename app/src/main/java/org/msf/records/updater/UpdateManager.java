package org.msf.records.updater;

import android.app.Application;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;

import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.joda.time.DateTime;
import org.msf.records.events.UpdateAvailableEvent;
import org.msf.records.events.UpdateNotAvailableEvent;
import org.msf.records.events.UpdateReadyToInstallEvent;
import org.msf.records.model.UpdateInfo;
import org.msf.records.utils.LexicographicVersion;
import org.msf.records.utils.Logger;

import java.io.File;
import java.util.List;

import de.greenrobot.event.EventBus;

/**
 * An object that manages auto-updating of the application from a configurable update server.
 *
 * <p>This class requires that all methods be called from the main thread.
 */
public class UpdateManager {

    private static final Logger LOG = Logger.create();

    /**
     * The update manager's module name for updates to this app.  A name of "foo"
     * means the updates are saved as "foo-1.2.apk", "foo-1.3.apk" on disk.
     */
    private static final String MODULE_NAME = "buendia-client";

    /**
     * The minimum period between checks for new updates, in seconds.  Repeated calls to
     * checkForUpdate() within this period will not check the server for new updates.
     * <p>Note that if the application is relaunched, an update check will be performed.
     */
    public static final int CHECK_PERIOD_SECONDS = 60 * 60; // Default to 1hr.

    /**
     * The minimal version number.
     *
     * <p>This value is smaller than any other version. If the current application has this version,
     * any non-minimal update will be installed over it. If an update has this version, it will
     * never be installed over any the current application.
     */
    public static final LexicographicVersion MINIMAL_VERSION = LexicographicVersion.parse("0");

    private static final IntentFilter sDownloadCompleteIntentFilter =
            new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);

    private final Object mLock = new Object();

    private final Application mApplication;
    private final UpdateServer mServer;

    private final PackageManager mPackageManager;
    private final LexicographicVersion mCurrentVersion;
    private final DownloadManager mDownloadManager;
    private final SharedPreferences mSharedPreferences;

    private DateTime mLastCheckForUpdateTime = new DateTime(0 /*instant*/);
    private AvailableUpdateInfo mLastAvailableUpdateInfo = null;

    // TODO: Consider caching this in SharedPreferences OR standardizing the location of it
    // so that we can check for it on application launch.
    private DownloadedUpdateInfo mLastDownloadedUpdateInfo = null;

    private final Object mDownloadLock = new Object();

    // ID of the currently running download, or -1 if no download is underway.
    private long mDownloadId = -1;

    UpdateManager(Application application, UpdateServer updateServer,
                  SharedPreferences sharedPreferences) {
        mApplication = application;
        mServer = updateServer;

        mPackageManager = application.getPackageManager();
        mDownloadManager =
                (DownloadManager) application.getSystemService(Context.DOWNLOAD_SERVICE);
        mSharedPreferences = sharedPreferences;
        mCurrentVersion = getCurrentVersion();
        mLastAvailableUpdateInfo = AvailableUpdateInfo.getInvalid(mCurrentVersion);
        mLastDownloadedUpdateInfo = DownloadedUpdateInfo.getInvalid(mCurrentVersion);
    }

    /**
     * Ensures that a check for available updates has been initiated within the last
     * CHECK_PERIOD_SECONDS, or initiates one.  May post events that update the UI
     * even if no new server check is initiated.  The check proceeds asynchronously in
     * the background and eventually posts the relevant events (see @link postEvent()).
     * Clients should call this method and then check for two sticky events:
     * UpdateAvailableEvent and UpdateReadyToInstallEvent.
     */
    public void checkForUpdate() {
        int checkPeriodSeconds = getCheckPeriodSeconds();
        DateTime now = DateTime.now();
        if (now.isBefore(mLastCheckForUpdateTime.plusSeconds(checkPeriodSeconds))) {
            if (!isDownloadInProgress()) {
                // This immediate check just updates the event state to match any current
                // knowledge of an available or downloaded update.  The more interesting
                // calls to postEvents occur below in PackageIndexReceivedListener and
                // DownloadReceiver.
                postEvents();
            }
            return;
        }

        PackageIndexReceivedListener listener = new PackageIndexReceivedListener();
        mServer.getPackageIndex(listener, listener);
        mLastCheckForUpdateTime = now;
    }

    /**
     * Post events notifying of whether a file is available to be downloaded, or a
     * file is downloaded and ready to install.  See {@link UpdateReadyToInstallEvent},
     * {@link UpdateAvailableEvent}, and {@link UpdateNotAvailableEvent} for details.
     */
    protected void postEvents() {
        EventBus bus = EventBus.getDefault();
        if (mLastDownloadedUpdateInfo.shouldInstall()
                && mLastDownloadedUpdateInfo.downloadedVersion.greaterThanOrEqualTo(
                        mLastAvailableUpdateInfo.availableVersion)) {
            bus.postSticky(new UpdateReadyToInstallEvent(mLastDownloadedUpdateInfo));
        } else if (mLastAvailableUpdateInfo.shouldUpdate()) {
            bus.removeStickyEvent(UpdateReadyToInstallEvent.class);
            bus.postSticky(new UpdateAvailableEvent(mLastAvailableUpdateInfo));
        } else {
            bus.removeStickyEvent(UpdateReadyToInstallEvent.class);
            bus.removeStickyEvent(UpdateAvailableEvent.class);
            bus.post(new UpdateNotAvailableEvent());
        }
    }

    /**
     * Starts downloading an available update in the background, registering a
     * DownloadUpdateReceiver to be invoked when the download is complete.
     *
     * @return whether a new download was started; {@code false} if the download failed to start.
     */
    public boolean startDownload(AvailableUpdateInfo availableUpdateInfo) {
        synchronized (mDownloadLock) {
            cancelDownload();

            mApplication.registerReceiver(
                    new DownloadUpdateReceiver(), sDownloadCompleteIntentFilter);

            try {
                String dir = getDownloadDirectory();
                if (dir == null) {
                    LOG.e("no external storage is available, can't start download");
                    return false;
                }
                DownloadManager.Request request =
                        new DownloadManager.Request(availableUpdateInfo.updateUri)
                                .setDestinationInExternalPublicDir(
                                        dir,
                                        MODULE_NAME + availableUpdateInfo.availableVersion + ".apk")
                                .setNotificationVisibility(
                                        DownloadManager.Request.VISIBILITY_VISIBLE);
                mDownloadId = mDownloadManager.enqueue(request);
                return true;
            } catch (Exception e) {
                LOG.e(e, "Failed to download application update from "
                        + availableUpdateInfo.updateUri);
                return false;
            }
        }
    }

    /** Installs the last downloaded update. */
    public void installUpdate(DownloadedUpdateInfo updateInfo) {
        Uri apkUri = Uri.parse(updateInfo.path);
        Intent installIntent = new Intent(Intent.ACTION_VIEW)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .setDataAndType(apkUri, "application/vnd.android.package-archive");
        mApplication.startActivity(installIntent);
    }

    /** Returns true if a download is in progress. */
    public boolean isDownloadInProgress() {
        return mDownloadId >= 0;
    }

    /** Stops any currently running download. */
    public boolean cancelDownload() {
        if (isDownloadInProgress()) {
            mDownloadManager.remove(mDownloadId);
            mDownloadId = -1;
            return true;
        }
        return false;
    }

    /**
     * Returns the relative path to the directory in which updates will be downloaded,
     * or null if storage is unavailable.
     */
    private String getDownloadDirectory() {
        String externalStorageDirectory =
                Environment.getExternalStorageDirectory().getAbsolutePath();
        File externalFilesDir = mApplication.getExternalFilesDir(null);
        if (externalFilesDir == null) {
            return null;
        }
        String downloadDirectory = externalFilesDir.getAbsolutePath();
        if (downloadDirectory.startsWith(externalStorageDirectory)) {
            downloadDirectory = downloadDirectory.substring(externalStorageDirectory.length());
        }
        return downloadDirectory;
    }

    /**
     * Get the time between updates from the shared preferences.
     */
    private int getCheckPeriodSeconds() {
        if (mSharedPreferences == null) {
            return CHECK_PERIOD_SECONDS;
        }
        return mSharedPreferences.getInt("apk_update_interval_secs", CHECK_PERIOD_SECONDS);
    }

    /** Returns the version of the application. */
    private LexicographicVersion getCurrentVersion() {
        PackageInfo packageInfo;
        try {
            packageInfo =
                    mPackageManager.getPackageInfo(mApplication.getPackageName(), 0 /*flags*/);
        } catch (PackageManager.NameNotFoundException e) {
            LOG.e(
                    e,
                    "No package found with the name " + mApplication.getPackageName() + ". "
                            + "This should never happen.");
            return MINIMAL_VERSION;
        }

        try {
            return LexicographicVersion.parse(packageInfo.versionName);
        } catch (IllegalArgumentException e) {
            LOG.e(
                    e,
                    "Application has an invalid semantic version: " + packageInfo.versionName + ". "
                            + "Please fix in build.gradle.");
            return MINIMAL_VERSION;
        }
    }

    private DownloadedUpdateInfo getLastDownloadedUpdateInfo() {
        String dir = getDownloadDirectory();
        if (dir == null) {
            LOG.e("no external storage is available, no download directory for updates");
            return DownloadedUpdateInfo.getInvalid(mCurrentVersion);
        }
        File downloadDirectoryFile =
                new File(Environment.getExternalStorageDirectory(), dir);
        if (!downloadDirectoryFile.exists()) {
            return DownloadedUpdateInfo.getInvalid(mCurrentVersion);
        }
        if (!downloadDirectoryFile.isDirectory()) {
            LOG.e(
                    "The path in which updates are downloaded is not a directory: '%1$s'",
                    downloadDirectoryFile.toString());
            return DownloadedUpdateInfo.getInvalid(mCurrentVersion);
        }

        File[] files = downloadDirectoryFile.listFiles();
        File latestApk = null;
        for (File file : files) {
            if (file.isFile()
                    && file.getName().endsWith(".apk")
                    && (latestApk == null || file.lastModified() > latestApk.lastModified())) {
                latestApk = file;
            }
        }

        if (latestApk == null) {
            return DownloadedUpdateInfo.getInvalid(mCurrentVersion);
        } else {
            return DownloadedUpdateInfo
                    .fromUri(mCurrentVersion, "file://" + latestApk.getAbsolutePath());
        }
    }

    /**
     * A listener that receives the index of available .apk files from the package server.
     */
    private class PackageIndexReceivedListener
            implements Response.Listener<List<UpdateInfo>>, Response.ErrorListener {

        @Override
        public void onResponse(List<UpdateInfo> response) {
            synchronized (mLock) {
                mLastAvailableUpdateInfo =
                        AvailableUpdateInfo.fromResponse(mCurrentVersion, response);
                mLastDownloadedUpdateInfo = getLastDownloadedUpdateInfo();
                LOG.i("received package index; lastAvailableUpdate: " + mLastAvailableUpdateInfo);
                postEvents();
            }
        }

        @Override
        public void onErrorResponse(VolleyError error) {
            String failure;
            if (error == null || error.networkResponse == null) {
                failure = "a network error";
            } else {
                failure = String.valueOf(error.networkResponse.statusCode);
            }

            LOG.w(
                    error,
                    "Server failed with " + failure + " while fetching package index.  Retry will "
                            + "occur shortly.");
            // assume no update is available
            EventBus.getDefault().post(new UpdateNotAvailableEvent());
        }
    }

    /**
     * A {@link BroadcastReceiver} that listens for
     * {@code DownloadManager.ACTION_DOWNLOAD_COMPLETED} intents.
     */
    private class DownloadUpdateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            synchronized (mDownloadLock) {
                if (!isDownloadInProgress()) {
                    LOG.e(
                            "Received an ACTION_DOWNLOAD_COMPLETED intent when no download was in "
                                    + "progress. This indicates that this receiver was registered "
                                    + "incorrectly. Unregistering receiver.");
                    mApplication.unregisterReceiver(this);
                    return;
                }

                long receivedDownloadId =
                        intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (mDownloadId != receivedDownloadId) {
                    LOG.d(
                            "Received an ACTION_DOWNLOAD_COMPLETED intent with download ID "
                                    + receivedDownloadId + " when the expected download ID is "
                                    + mDownloadId + ". Download was probably initiated by another "
                                    + " application.");
                    return;
                }

                // We have received the intent for our download, so we'll call the download finished
                // and unregister the receiver.
                mDownloadId = -1;
                mApplication.unregisterReceiver(this);

                Cursor cursor = null;
                final String uriString;
                try {
                    cursor = mDownloadManager.query(
                            new DownloadManager.Query().setFilterById(receivedDownloadId));
                    if (!cursor.moveToFirst()) {
                        LOG.w(
                                "Received download ID " + receivedDownloadId + " does not exist.");
                        // TODO: Consider firing an event.
                        return;
                    }

                    int status = cursor.getInt(
                            cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                    if (status != DownloadManager.STATUS_SUCCESSFUL) {
                        LOG.w("Update download failed with status " + status + ".");
                        // TODO: Consider firing an event.
                        return;
                    }

                    uriString = cursor.getString(
                            cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                    if (uriString == null) {
                        LOG.w("No path for a downloaded file exists.");
                        // TODO: Consider firing an event.
                        return;
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }

                try {
                    Uri.parse(uriString);
                } catch (IllegalArgumentException e) {
                    LOG.w(e, "Path for downloaded file is invalid: %1$s.", uriString);
                    // TODO: Consider firing an event.
                    return;
                }

                mLastDownloadedUpdateInfo =
                        DownloadedUpdateInfo.fromUri(mCurrentVersion, uriString);
                LOG.i("downloaded update: " + mLastDownloadedUpdateInfo);

                if (!mLastDownloadedUpdateInfo.isValid) {
                    LOG.w(
                            "The last update downloaded from the server is invalid. Update checks "
                                    + "will not occur for the next %1$d seconds.",
                            CHECK_PERIOD_SECONDS);

                    // Set the last available update info to an invalid value so as to prevent
                    // further download attempts.
                    mLastAvailableUpdateInfo = AvailableUpdateInfo.getInvalid(mCurrentVersion);

                    return;
                }

                if (!mLastAvailableUpdateInfo.availableVersion
                        .greaterThanOrEqualTo(mLastDownloadedUpdateInfo.downloadedVersion)) {
                    LOG.w(
                            "The last update downloaded from the server was reported to have "
                                    + "version '%1$s' but actually has version '%2$s'. This "
                                    + "indicates a server configuration problem. Update checks "
                                    + "will not occur for the next %3$d seconds.",
                            mLastAvailableUpdateInfo.availableVersion.toString(),
                            mLastDownloadedUpdateInfo.downloadedVersion.toString(),
                            CHECK_PERIOD_SECONDS);

                    // Set the last available update info to an invalid value so as to prevent
                    // further download attempts.
                    mLastAvailableUpdateInfo = AvailableUpdateInfo.getInvalid(mCurrentVersion);

                    return;
                }

                postEvents();
            }
        }
    }
}
