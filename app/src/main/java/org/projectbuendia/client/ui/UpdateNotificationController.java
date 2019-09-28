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

package org.projectbuendia.client.ui;

import org.projectbuendia.client.App;
import org.projectbuendia.client.events.UpdateAvailableEvent;
import org.projectbuendia.client.events.UpdateReadyToInstallEvent;
import org.projectbuendia.client.updater.AvailableUpdateInfo;
import org.projectbuendia.client.updater.DownloadedUpdateInfo;

import de.greenrobot.event.EventBus;

/**
 * A controller for software update notification behaviour.  Activities that
 * want this behaviour should instantiate this controller and call its init()
 * and suspend() methods, in addition to other controllers they may have.
 */
public class UpdateNotificationController {
    Ui mUi;
    AvailableUpdateInfo mAvailableUpdateInfo;
    DownloadedUpdateInfo mDownloadedUpdateInfo;

    public interface Ui {

        void showUpdateAvailableForDownload(AvailableUpdateInfo updateInfo);

        void showUpdateReadyToInstall(DownloadedUpdateInfo updateInfo);

        void hideSoftwareUpdateNotifications();
    }

    public UpdateNotificationController(Ui ui) {
        mUi = ui;
    }

    /** Activate the controller.  Called whenever user enters a new activity. */
    public void init() {
        EventBus.getDefault().register(this);
        App.getUpdateManager().checkForUpdate();
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

    public void suspend() {
        EventBus.getDefault().unregister(this);
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
            App.getUpdateManager().startDownload(mAvailableUpdateInfo);
        }
    }

    /** Installs the last downloaded update. */
    public void onEventMainThread(BaseActivity.InstallationRequestedEvent event) {
        if (mDownloadedUpdateInfo != null) {
            App.getUpdateManager().installUpdate(mDownloadedUpdateInfo);
        }
    }
}
