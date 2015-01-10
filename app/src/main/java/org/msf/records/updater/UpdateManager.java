package org.msf.records.updater;

import android.app.Application;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.github.zafarkhaja.semver.ParseException;
import com.github.zafarkhaja.semver.Version;

import org.joda.time.DateTime;
import org.msf.records.App;
import org.msf.records.events.UpdateAvailableEvent;
import org.msf.records.events.UpdateDownloadedEvent;
import org.msf.records.events.UpdateNotAvailableEvent;
import org.msf.records.model.UpdateInfo;
import org.msf.records.utils.Logger;

import java.io.File;

import de.greenrobot.event.EventBus;

/**
 * An object that manages auto-updating of the application from a configurable update server.
 *
 * <p>This class requires that all methods be called from the main thread.
 */
public class UpdateManager {

    private static final Logger LOG = Logger.create();

    /**
     * The frequency with which to check for updates, in hours.
     *
     * <p>Note that if the application is relaunched, an update check will be performed.
     */
    public static final int CHECK_FOR_UPDATE_FREQUENCY_HOURS = 1;

    /**
     * An invalid semantic version.
     *
     * <p>This value is smaller than any other semantic version. If the current application has this
     * version, any valid update will be installed over it. If an update has this version, it will
     * never be installed over any the current application.
     */
    public static final Version INVALID_VERSION = Version.forIntegers(0);

    private static final IntentFilter sDownloadCompleteIntentFilter =
            new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);

    private final Object mLock = new Object();

    private final UpdateServer mServer;
    private final PackageManager mPackageManager;
    private final Version mCurrentVersion;
    private final DownloadManager mDownloadManager;

    private DateTime mLastCheckForUpdateTime = new DateTime(0 /*instant*/);
    private AvailableUpdateInfo mLastAvailableUpdateInfo = null;

    // TODO(dxchen): Consider caching this in SharedPreferences OR standardizing the location of it
    // so that we can check for it on application launch.
    private DownloadedUpdateInfo mLastDownloadedUpdateInfo = null;

    private final Object mDownloadLock = new Object();
    private boolean mIsDownloadInProgress = false;
    private long mDownloadId = -1;

    UpdateManager(Application application, UpdateServer updateServer) {
        mServer = updateServer;

        mPackageManager = application.getPackageManager();
        mDownloadManager =
                (DownloadManager) application.getSystemService(Context.DOWNLOAD_SERVICE);

        mCurrentVersion = getCurrentVersion();
        mLastAvailableUpdateInfo = AvailableUpdateInfo.getInvalid(mCurrentVersion);
        mLastDownloadedUpdateInfo = DownloadedUpdateInfo.getInvalid(mCurrentVersion);
    }

    /**
     * Asynchronously checks for available updates and posts the appropriate event.
     *
     * <p>The following events are posted:
     * <ul>
     *     <li>
     *         {@link UpdateDownloadedEvent} - If an update has been downloaded and is as new as the
     *         latest update available on the server. Note that if the update has not yet been
     *         downloaded, this event is not fired.
     *     </li>
     *     <li>
     *         {@link UpdateAvailableEvent} - If an update is available on the server and either no
     *         update has been downloaded or the available update is newer than the downloaded
     *         update.
     *     </li>
     *     <li>
     *         {@link UpdateNotAvailableEvent} - If no update is available on the server and no
     *         update has been downloaded.
     *     </li>
     * </ul>
     *
     * <p>The result of this method is cached for {@code CHECK_FOR_UPDATE_FREQUENCY_HOURS}.
     */
    public void checkForUpdate() {
        DateTime now = DateTime.now();
        if (now.isBefore(mLastCheckForUpdateTime.plusHours(CHECK_FOR_UPDATE_FREQUENCY_HOURS))) {
            if (!mIsDownloadInProgress && mLastAvailableUpdateInfo.shouldUpdate()) {
                EventBus.getDefault().post(new UpdateAvailableEvent(mLastAvailableUpdateInfo));
            }

            return;
        }

        mLastCheckForUpdateTime = now;

        CheckForUpdateResponseListener listener = new CheckForUpdateResponseListener();
        mServer.getAndroidUpdateInfo(listener, listener);
    }

    /**
     * Asynchronously downloads an available update and posts an event indicating that the update is
     * available.
     *
     * @return whether a download was started. {@code false} if a download is already in progress.
     */
    public boolean downloadUpdate(AvailableUpdateInfo availableUpdateInfo) {
        synchronized (mDownloadLock) {
            if (mIsDownloadInProgress) {
                return false;
            }
            mIsDownloadInProgress = true;

            App.getInstance().registerReceiver(
                    new DownloadUpdateReceiver(), sDownloadCompleteIntentFilter);

            DownloadManager.Request request =
                    new DownloadManager.Request(availableUpdateInfo.updateUri)
                            .setTitle(
                                    "Downloading update v"
                                            + availableUpdateInfo.availableVersion.toString())
                            .setDestinationInExternalFilesDir(
                                    App.getInstance(),
                                    null /*dirType*/,
                                    "androidclient_"
                                            + availableUpdateInfo.availableVersion.toString())
                            .setNotificationVisibility(
                                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            mDownloadId = mDownloadManager.enqueue(request);

            return true;
        }
    }

    /**
     * Installs a downloaded update.
     */
    public void installUpdate(DownloadedUpdateInfo updateInfo) {
        Uri apkUri = Uri.fromFile(new File(updateInfo.path));
        Intent installIntent = new Intent(Intent.ACTION_VIEW)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .setDataAndType(apkUri, "application/vnd.android.package-archive");
        App.getInstance().startActivity(installIntent);
    }

    /**
     * Returns whether a download is in progress.
     */
    public boolean isDownloadInProgress() {
        return mIsDownloadInProgress;
    }

    /**
     * Returns the semantic version of the application.
     */
    private Version getCurrentVersion() {
        PackageInfo packageInfo;
        try {
            packageInfo =
                    mPackageManager.getPackageInfo(App.getInstance().getPackageName(), 0 /*flags*/);
        } catch (PackageManager.NameNotFoundException e) {
            LOG.e(
                    e,
                    "No package found with the name " + App.getInstance().getPackageName() + ". "
                            + "This should never happen.");
            return INVALID_VERSION;
        }

        try {
            return Version.valueOf(packageInfo.versionName);
        } catch (ParseException e) {
            LOG.e(
                    e,
                    "Application has an invalid semantic version: " + packageInfo.versionName + ". "
                            + "Please fix in build.gradle.");
            return INVALID_VERSION;
        }
    }

    /**
     * A listener that handles check-for-update responses.
     */
    private class CheckForUpdateResponseListener
            implements Response.Listener<UpdateInfo>, Response.ErrorListener {

        @Override
        public void onResponse(UpdateInfo response) {
            synchronized (mLock) {
                mLastAvailableUpdateInfo =
                        AvailableUpdateInfo.fromResponse(mCurrentVersion, response);

                if (mLastDownloadedUpdateInfo.shouldInstall()
                        && mLastDownloadedUpdateInfo.downloadedVersion
                                .greaterThanOrEqualTo(mLastAvailableUpdateInfo.availableVersion)) {
                    // If there's already a downloaded update that is as recent as the available
                    // update, post an UpdateDownloadedEvent.
                    EventBus.getDefault()
                            .post(new UpdateDownloadedEvent(mLastDownloadedUpdateInfo));
                } else if (mLastAvailableUpdateInfo.shouldUpdate()) {
                    // Else, if the latest available update is good, post an UpdateAvailableEvent.
                    EventBus.getDefault().post(new UpdateAvailableEvent(mLastAvailableUpdateInfo));
                } else {
                    // Else, post an UpdateNotAvailableEvent.
                    EventBus.getDefault().post(new UpdateNotAvailableEvent());
                }
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
                    "Server failed with " + failure + " while downloading update. Retry will "
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
                if (!mIsDownloadInProgress) {
                    LOG.e(
                            "Received an ACTION_DOWNLOAD_COMPLETED intent when no download was in "
                                    + "progress. This indicates that this receiver was registered "
                                    + "incorrectly. Unregistering receiver.");
                    App.getInstance().unregisterReceiver(this);
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
                mIsDownloadInProgress = false;
                App.getInstance().unregisterReceiver(this);

                Cursor cursor = null;
                final String uriString;
                try {
                    cursor = mDownloadManager.query(
                            new DownloadManager.Query().setFilterById(receivedDownloadId));
                    if (!cursor.moveToFirst()) {
                        LOG.w(
                                "Received download ID " + receivedDownloadId + " does not exist.");
                        // TODO(dxchen): Consider firing an event.
                        return;
                    }

                    int status = cursor.getInt(
                            cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                    if (status != DownloadManager.STATUS_SUCCESSFUL) {
                        LOG.w("Update download failed with status " + status + ".");
                        // TODO(dxchen): Consider firing an event.
                        return;
                    }

                    uriString = cursor.getString(
                            cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                    if (uriString == null) {
                        LOG.w("No path for a downloaded file exists.");
                        // TODO(dxchen): Consider firing an event.
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
                    LOG.w(e, "Path for downloaded file is invalid: " + uriString + ".");
                    // TODO(dxchen): Consider firing an event.
                    return;
                }

                // TODO(dxchen): Extract path from the local URI.
                mLastDownloadedUpdateInfo =
                        DownloadedUpdateInfo.fromPath(mCurrentVersion, uriString);
            }
        }
    }
}
