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

    public void checkForUpdates() {
        mUpdateManager.checkForUpdate();
        updateAvailabilityNotifications();
    }

    public void onEventMainThread(UpdateAvailableEvent event) {
        updateAvailabilityNotifications();
    }

    public void onEventMainThread(UpdateReadyToInstallEvent event) {
        updateAvailabilityNotifications();
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

    /** Starts a download of the last known available update. */
    public void startDownload() {
        if (mAvailableUpdateInfo != null) {
            mUpdateManager.startDownload(mAvailableUpdateInfo);
        }
    }

    /** Installs the last downloaded update. */
    public void installUpdate() {
        if (mDownloadedUpdateInfo != null) {
            mUpdateManager.installUpdate(mDownloadedUpdateInfo);
        }
    }
}
