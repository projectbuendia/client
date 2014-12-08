package org.msf.records.events.user;

/**
 * An event bus event indicating that the set of known users failed to be loaded from local cache.
 */
public class KnownUsersLoadFailedEvent {

    public static final int REASON_UNKNOWN = 0;
    public static final int REASON_NO_USERS_RETURNED = 1;

    private final int mReason;

    public int getReason() {
        return mReason;
    }

    public KnownUsersLoadFailedEvent(int reason) {
        mReason = reason;
    }
}
