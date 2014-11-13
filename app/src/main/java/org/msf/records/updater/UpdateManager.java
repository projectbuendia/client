package org.msf.records.updater;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.squareup.otto.Bus;

import org.joda.time.DateTime;
import org.msf.records.App;
import org.msf.records.events.UpdateAvailableEvent;
import org.msf.records.events.UpdateDownloadedEvent;
import org.msf.records.events.UpdateNotAvailableEvent;

import java.io.File;

/**
 * An object that manages auto-updating of the application from a configurable update server.
 */
public class UpdateManager {

    private static final String TAG = UpdateManager.class.getName();

    /**
     * The frequency with which to check for updates, in hours.
     *
     * <p>Note that if the application is relaunched, an update check will be performed.
     */
    public static final int CHECK_FOR_UPDATE_FREQUENCY_HOURS = 24;

    /**
     * The version code of a downloaded update that is invalid.
     */
    public static final int INVALID_VERSION_CODE = -1;

    private final PackageManager mPackageManager;
    private final int mCurrentVersionCode;

    private DateTime mLastCheckForUpdateTime = new DateTime(0 /*instant*/);
    private AvailableUpdateInfo mLastAvailableUpdateInfo = null;

    // TODO(dxchen): Consider caching this in SharedPreferences OR standardizing the location of it
    // so that we can check for it on application launch.
    private DownloadedUpdateInfo mLastDownloadedUpdateInfo = null;

    private boolean mIsDownloadInProgress = false;

    public UpdateManager() {
        mPackageManager = App.getInstance().getPackageManager();
        mCurrentVersionCode = getCurrentVersionCode();
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
     *
     * @param bus the event bus to which to post update available events. If called from an activity
     *            or a service, this should be an instance of
     *            {@link org.msf.records.events.MainThreadBus}.
     */
    public void checkForUpdate(Bus bus) {
        new CheckForUpdateTask(bus, mCurrentVersionCode).execute();
    }

    /**
     * Asynchronously downloads an available update and posts an event indicating that the update is
     * available.
     *
     * @param bus the event bus to which to post update downloaded events. If called from an
     *            activity or a service, this should be an instance of
     *            {@link org.msf.records.events.MainThreadBus}.
     */
    public void downloadUpdate(Bus bus, AvailableUpdateInfo updateInfo) {
        new DownloadUpdateTask(bus).execute();
    }

    /**
     * Installs a downloaded update.
     */
    public void installUpdate(DownloadedUpdateInfo updateInfo) {
        Uri apkUri = Uri.fromFile(new File(updateInfo.mPath));
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
     * Returns the version code of the application.
     */
    private int getCurrentVersionCode() {
        PackageInfo packageInfo;
        try {
            packageInfo =
                    mPackageManager.getPackageInfo(App.getInstance().getPackageName(), 0 /*flags*/);
        } catch (PackageManager.NameNotFoundException ex) {
            Log.wtf(
                    TAG,
                    "No package found with the name " + App.getInstance().getPackageName() + ". "
                            + "This should never happen.");
            return INVALID_VERSION_CODE;
        }

        return packageInfo.versionCode;
    }

    /**
     * An {@link AsyncTask} that checks for an application update and fires an appropriate event.
     */
    private class CheckForUpdateTask extends AsyncTask<Void, Void, Void> {

        private final Bus mBus;
        private final int mCurrentVersionCode;

        public CheckForUpdateTask(Bus bus, int currentVersionCode) {
            mBus = bus;
            mCurrentVersionCode = currentVersionCode;
        }

        @Override
        protected Void doInBackground(Void... params) {
            DateTime now = DateTime.now();
            if (now.isAfter(mLastCheckForUpdateTime.plusHours(CHECK_FOR_UPDATE_FREQUENCY_HOURS))) {
                mLastCheckForUpdateTime = now;
                // TODO(dxchen): Actually issue an RPC request to check for updates. For now, we
                // will just fake it.
                mLastAvailableUpdateInfo = new AvailableUpdateInfo(
                        mCurrentVersionCode, mCurrentVersionCode, Uri.EMPTY);
            }

            if (mLastAvailableUpdateInfo != null) {
                // If there's already a downloaded update that is as recent as the available update,
                // immediately post an UpdateDownloadedEvent; otherwise, post an
                // UpdateAvailableEvent.
                if (mLastDownloadedUpdateInfo != null
                        && (mLastAvailableUpdateInfo.mAvailableVersionCode
                                <= mLastDownloadedUpdateInfo.mDownloadedVersionCode)) {
                    mBus.post(new UpdateDownloadedEvent(mLastDownloadedUpdateInfo));
                } else {
                    mBus.post(new UpdateAvailableEvent(mLastAvailableUpdateInfo));
                }
            } else {
                mBus.post(new UpdateNotAvailableEvent());
            }

            return null;
        }
    }

    /**
     * An {@link AsyncTask} that downloads an available update.
     */
    private class DownloadUpdateTask extends AsyncTask<Void, Void, Void> {

        private final Bus mBus;

        public DownloadUpdateTask(Bus bus) {
            mBus = bus;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            // If another download is in progress, simply return. Mutates to mIsDownloadInProgress
            // are not thread-safe but our AsyncTasks are configured to execute sequentially, so
            // we're okay.
            if (mIsDownloadInProgress) {
                return null;
            }
            mIsDownloadInProgress = true;

            try {
                // TODO(dxchen): Actually issue an HTTP request to download the update. For now, we
                // will just fake it.
                mLastDownloadedUpdateInfo =
                        new DownloadedUpdateInfo("/sdcard/org.msf.records/app-debug.apk");

                mBus.post(new UpdateDownloadedEvent(mLastDownloadedUpdateInfo));

                return null;
            } finally {
                mIsDownloadInProgress = false;
            }
        }
    }
}
