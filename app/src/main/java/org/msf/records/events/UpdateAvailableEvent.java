package org.msf.records.events;

import org.msf.records.updater.AvailableUpdateInfo;

/**
 * An event bus event that indicates that an APK update is available.
 */
public final class UpdateAvailableEvent {

    public final AvailableUpdateInfo mUpdateInfo;

    public UpdateAvailableEvent(AvailableUpdateInfo updateInfo) {
        mUpdateInfo = updateInfo;
    }
}
