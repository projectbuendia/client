// Copyright 2015 The Project Buendia Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at: http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distrib-
// uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
// specific language governing permissions and limitations under the License.

package org.projectbuendia.client.updater;

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
import android.os.Environment;

import com.android.volley.NoConnectionError;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.projectbuendia.client.AppSettings;
import org.projectbuendia.client.events.UpdateAvailableEvent;
import org.projectbuendia.client.events.UpdateNotAvailableEvent;
import org.projectbuendia.client.events.UpdateReadyToInstallEvent;
import org.projectbuendia.client.json.JsonUpdateInfo;
import org.projectbuendia.client.utils.LexicographicVersion;
import org.projectbuendia.client.utils.Logger;

import java.io.File;
import java.util.List;

import de.greenrobot.event.EventBus;

/**
 * An object that manages auto-updating of the application from a configurable package server.
 * <p/>
 * <p>This class requires that all methods be called from the main thread.
 */
public class UpdateManager {

    /**
     * The minimal version number.
     * <p/>
     * <p>This value is smaller than any other version. If the current application has this version,
     * any non-minimal update will be installed over it. If an update has this version, it will
     * never be installed over any the current application.
     */
    public static final LexicographicVersion MINIMAL_VERSION = LexicographicVersion.parse("0");
    private static final Logger LOG = Logger.create();
    /**
     * The update manager's module name for updates to this app.  A name of "foo"
     * means the updates are saved as "foo-1.2.apk", "foo-1.3.apk" on disk.
     */
    private static final String MODULE_NAME = "buendia-client";
    private static final IntentFilter sDownloadCompleteIntentFilter =
        new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);

    private final Object mLock = new Object();

    private final Application mApplication;
    private final PackageServer mServer;

    private final PackageManager mPackageManager;
    private final LexicographicVersion mCurrentVersion;
    private final DownloadManager mDownloadManager;
    private final AppSettings mSettings;

    private Instant mLastCheckForUpdateTime = new Instant(0 /*instant*/);
    private AvailableUpdateInfo mLastAvailableUpdateInfo = null;

    // TODO: Consider caching this in SharedPreferences OR standardizing the location of it
    // so that we can check for it on application launch.
    private DownloadedUpdateInfo mLastDownloadedUpdateInfo = null;

    private final Object mDownloadLock = new Object();

    // ID of the currently running download, or -1 if no download is underway.
    private long mDownloadId = -1;

    /**
     * Ensures that a check for available updates has been initiated within the
     * last update interval period {@see AppSettings#getApkUpdateInterval()},
     * or initiates one.  May post events that update the UI even if no new
     * server check is initiated.  The check proceeds asynchronously in the
     * background and eventually posts the relevant events {@see postEvent()}.
     * Clients should call this method and then check for two sticky events:
     * UpdateAvailableEvent and UpdateReadyToInstallEvent.
     */
    public void checkForUpdate() {
        Instant now = Instant.now();
        if (now.isBefore(mLastCheckForUpdateTime.plus(
            new Duration(mSettings.getApkUpdateInterval() * 1000)))) {
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

    /** Returns true if a download is in progress. */
    public boolean isDownloadInProgress() {
        return mDownloadId >= 0;
    }

    /**
     * Posts events notifying of whether a file is available to be downloaded, or a
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
     * @return whether a new download was started; {@code false} if the download failed to start.
     */
    public boolean startDownload(AvailableUpdateInfo availableUpdateInfo) {
        // TODO(ping): 2019-09-18 - For some reason, this starts the
        // download but Android never reports completion.
        // So, for now, send the user to the /client webpage instead.
        if (2 > 1) {
            Uri uri = Uri.parse("http://" + mSettings.getServer() + "/client");
            mApplication.startActivity(new Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return true;
        }

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
                String filename = MODULE_NAME + "-"
                    + availableUpdateInfo.availableVersion + ".apk";
                DownloadManager.Request request =
                    new DownloadManager.Request(availableUpdateInfo.updateUri)
                        .setDestinationInExternalPublicDir(dir, filename)
                        .setNotificationVisibility(
                            DownloadManager.Request.VISIBILITY_VISIBLE);
                mDownloadId = mDownloadManager.enqueue(request);
                LOG.i("Starting download: " + availableUpdateInfo.updateUri
                    + " -> " + filename + " in " + dir);
                return true;
            } catch (Exception e) {
                LOG.e(e, "Failed to download application update from "
                    + availableUpdateInfo.updateUri);
                return false;
            }
        }
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

    /** Installs the last downloaded update. */
    public void installUpdate(DownloadedUpdateInfo updateInfo) {
        Uri apkUri = Uri.parse(updateInfo.path);
        Intent installIntent = new Intent(Intent.ACTION_VIEW)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .setDataAndType(apkUri, "application/vnd.android.package-archive");
        mApplication.startActivity(installIntent);
    }

    UpdateManager(Application application, PackageServer packageServer, AppSettings settings) {
        mApplication = application;
        mServer = packageServer;
        mSettings = settings;
        mPackageManager = application.getPackageManager();
        mDownloadManager =
            (DownloadManager) application.getSystemService(Context.DOWNLOAD_SERVICE);
        mCurrentVersion = getCurrentVersion();
        mLastAvailableUpdateInfo = AvailableUpdateInfo.getInvalid(mCurrentVersion);
        mLastDownloadedUpdateInfo = DownloadedUpdateInfo.getInvalid(mCurrentVersion);
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
            LOG.w("App has an invalid version (or is a dev build): " + packageInfo.versionName);
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

    /** A listener that receives the index of available .apk files from the package server. */
    private class PackageIndexReceivedListener
        implements Response.Listener<List<JsonUpdateInfo>>, Response.ErrorListener {

        @Override public void onResponse(List<JsonUpdateInfo> response) {
            synchronized (mLock) {
                mLastAvailableUpdateInfo =
                    AvailableUpdateInfo.fromResponse(mCurrentVersion, response);
                mLastDownloadedUpdateInfo = getLastDownloadedUpdateInfo();
                LOG.i("received package index; lastAvailableUpdate: " + mLastAvailableUpdateInfo);
                postEvents();
            }
        }

        @Override public void onErrorResponse(VolleyError error) {
            String message = "Server failed; will retry shortly";
            if (error != null && error.networkResponse != null) {
                message = "Server failed (" + error.networkResponse.statusCode + "); will retry shortly";
            }
            if (error instanceof NoConnectionError) {
                LOG.w(message + " - " + error);
            } else {
                LOG.w(error, message);
            }
            // assume no update is available
            EventBus.getDefault().post(new UpdateNotAvailableEvent());
        }
    }

    /**
     * A {@link BroadcastReceiver} that listens for
     * {@code DownloadManager.ACTION_DOWNLOAD_COMPLETED} intents.
     */
    private class DownloadUpdateReceiver extends BroadcastReceiver {

        @Override public void onReceive(Context context, Intent intent) {
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

                final String uriString;
                try (Cursor cursor = mDownloadManager.query(
                    new DownloadManager.Query().setFilterById(receivedDownloadId))) {
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
                        mSettings.getApkUpdateInterval());

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
                        mSettings.getApkUpdateInterval());

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
