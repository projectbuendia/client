package org.msf.records.ui;

import org.msf.records.App;
import org.msf.records.events.UpdateAvailableEvent;
import org.msf.records.events.UpdateReadyToInstallEvent;
import org.msf.records.updater.AvailableUpdateInfo;
import org.msf.records.updater.DownloadedUpdateInfo;
import org.msf.records.updater.UpdateManager;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

/**
 * A controller for software update notification behaviour.  Activities that
 * want this behaviour should instantiate this controller and call its init()
 * and suspend() methods, in addition to other controllers they may have.
 */
public class UpdateNotificationController {
    public interface Ui {

        void showUpdateAvailableForDownload(AvailableUpdateInfo updateInfo);

        void showUpdateReadyToInstall(DownloadedUpdateInfo updateInfo);

        void hideSoftwareUpdateNotifications();
    }

    Ui mUi;
    @Inject UpdateManager mUpdateManager;
    AvailableUpdateInfo mAvailableUpdateInfo;
    DownloadedUpdateInfo mDownloadedUpdateInfo;

    public UpdateNotificationController(Ui ui) {
        mUi = ui;
        App.getInstance().inject(this);
    }

    /** Activate the controller.  Called whenever user enters a new activity. */
    public void init() {
        EventBus.getDefault().register(this);
        mUpdateManager.checkForUpdate();
        updateAvailabilityNotifications();
    }

    public void suspend() {
        EventBus.getDefault().unregister(this);
    }

    /**
     * Shows or hides software update notifications in the UI according to the
     * currently posted sticky events (posted by the UpdateManager).
     */
    protected void updateAvailabilityNotifications() {
        EventBus bus = EventBus.getDefault();
        UpdateReadyToInstallEvent readyEvent =
                bus.getStickyEvent(UpdateReadyToInstallEvent.class);
        UpdateAvailableEvent availableEvent =
                bus.getStickyEvent(UpdateAvailableEvent.class);
        if (readyEvent != null) {
            mDownloadedUpdateInfo = readyEvent.updateInfo;
            mUi.showUpdateReadyToInstall(mDownloadedUpdateInfo);
        } else if (availableEvent != null) {
            mDownloadedUpdateInfo = null;
            mAvailableUpdateInfo = availableEvent.updateInfo;
            mUi.showUpdateAvailableForDownload(mAvailableUpdateInfo);
        } else {
            mDownloadedUpdateInfo = null;
            mAvailableUpdateInfo = null;
            mUi.hideSoftwareUpdateNotifications();
        }
    }

    /** Updates the UI in response to updated knowledge of available .apks. */
    public void onEventMainThread(UpdateAvailableEvent event) {
        updateAvailabilityNotifications();
    }

    /** Updates the UI in response to completion of an .apk download. */
    public void onEventMainThread(UpdateReadyToInstallEvent event) {
        updateAvailabilityNotifications();
    }

    /** Starts a download of the last known available update. */
    public void onEventMainThread(BaseActivity.DownloadRequestedEvent event) {
        if (mAvailableUpdateInfo != null) {
            mUpdateManager.startDownload(mAvailableUpdateInfo);
        }
    }

    /** Installs the last downloaded update. */
    public void onEventMainThread(BaseActivity.InstallationRequestedEvent event) {
        if (mDownloadedUpdateInfo != null) {
            mUpdateManager.installUpdate(mDownloadedUpdateInfo);
        }
    }
}