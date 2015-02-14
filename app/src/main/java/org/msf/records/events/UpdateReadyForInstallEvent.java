package org.msf.records.events;

import org.msf.records.updater.DownloadedUpdateInfo;

/**
 * A sticky event that indicates that an update has been downloaded and is ready to be
 * installed.  An update is considered suitable for installation if it has a version number
 * higher than the currently running app and at least as high as the highest version
 * available on the package server.
 */
public class UpdateReadyForInstallEvent {

    public final DownloadedUpdateInfo updateInfo;

    public UpdateReadyForInstallEvent(DownloadedUpdateInfo downloadedUpdateInfo) {
        updateInfo = downloadedUpdateInfo;
    }
}
