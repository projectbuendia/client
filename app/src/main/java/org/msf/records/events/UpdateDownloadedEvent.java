package org.msf.records.events;

import org.msf.records.updater.DownloadedUpdateInfo;

/**
 * An event bus event that indicates that an update has been downloaded and is ready to be
 * installed.
 */
public class UpdateDownloadedEvent {

    public final DownloadedUpdateInfo mDownloadedUpdateInfo;

    public UpdateDownloadedEvent(DownloadedUpdateInfo downloadedUpdateInfo) {
        mDownloadedUpdateInfo = downloadedUpdateInfo;
    }
}
