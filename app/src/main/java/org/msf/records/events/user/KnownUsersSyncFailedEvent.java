package org.msf.records.events.user;

/**
 * An event bus event indicating that the set of known users failed to be synced from the server.
 */
public class KnownUsersSyncFailedEvent {

    public static final int REASON_UNKNOWN = 0;

    private final int mReason;

    public KnownUsersSyncFailedEvent(int reason) {
        mReason = reason;
    }
}
