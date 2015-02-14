package org.msf.records.events;

import org.msf.records.updater.AvailableUpdateInfo;

/**
 * A sticky event indicating that an .apk update is available on the server
 * with a version number higher than the currently running version of the app.
 */
public final class UpdateAvailableEvent {

    public final AvailableUpdateInfo updateInfo;

    public UpdateAvailableEvent(AvailableUpdateInfo updateInfo) {
        this.updateInfo = updateInfo;
    }
}
