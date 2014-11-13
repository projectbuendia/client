package org.msf.records.updater;

import android.app.Application;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.squareup.otto.Bus;
import com.squareup.otto.Produce;

import org.joda.time.DateTime;
import org.msf.records.events.UpdateAvailableEvent;
import org.msf.records.events.UpdateDownloadedEvent;
import org.msf.records.events.UpdateNotAvailableEvent;

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

    private final Application mApplication;
    private final PackageManager mPackageManager;
    private final int mCurrentVersionCode;

    private DateTime mLastCheckForUpdateTime = null;
    private AvailableUpdateInfo mLastAvailableUpdateInfo = null;

    private boolean mIsDownloadInProgress = false;

    public UpdateManager(Application application) {
        mApplication = application;
        mPackageManager = mApplication.getPackageManager();
        mCurrentVersionCode = getCurrentVersionCode();
    }

    /**
     * Produces a {@link UpdateDownloadedEvent} if there exists a downloaded APK that has yet to be
     * installed.
     */
    @Produce
    protected UpdateDownloadedEvent produceUpdateDownloadedEvent() {
        // TODO(dxchen): Check the disk for an available update whose version is greater than the
        // current version code. For now, we're faking it.

        DownloadedUpdateInfo downloadedUpdateInfo = new DownloadedUpdateInfo("/fake/path");

        return new UpdateDownloadedEvent(downloadedUpdateInfo);
    }

    /**
     * Asynchronously checks for available updates and, if an update is available, posts an
     * {@link UpdateAvailableEvent} that should be handled by an activity to perform an update as
     * soon as the user is able to do so.
     *
     * The result of this method is cached for {@code CHECK_FOR_UPDATE_FREQUENCY_HOURS}.
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
                    mPackageManager.getPackageInfo(mApplication.getPackageName(), 0 /*flags*/);
        } catch (PackageManager.NameNotFoundException ex) {
            Log.wtf(
                    TAG,
                    "No package found with the name " + mApplication.getPackageName() + ". This "
                            + "should never happen.");
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
                        mCurrentVersionCode, mCurrentVersionCode + 1, Uri.EMPTY);

                return null;
            }

            if (mLastAvailableUpdateInfo != null) {
                // Post an event
                mBus.post(new UpdateAvailableEvent(mLastAvailableUpdateInfo));
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
                DownloadedUpdateInfo updateInfo = new DownloadedUpdateInfo("fake/path");

                mBus.post(new UpdateDownloadedEvent(updateInfo));

                return null;
            } finally {
                mIsDownloadInProgress = false;
            }
        }
    }
}
