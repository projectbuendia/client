package org.msf.records.events.user;

/**
 * An event bus event indicating that the set of known users failed to be loaded from local cache.
 */
public final class KnownUsersLoadFailedEvent {

    public static final int REASON_UNKNOWN = 0;
    public static final int REASON_NO_USERS_RETURNED = 1;

    private final int mReason;

    public KnownUsersLoadFailedEvent(int reason) {
        mReason = reason;
    }

    public int getReason() {
        return mReason;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof KnownUsersLoadFailedEvent)) return false;
        KnownUsersLoadFailedEvent other = (KnownUsersLoadFailedEvent) o;
        return other.mReason == mReason;
    }

    @Override
    public int hashCode() {
        return mReason;
    }

    @Override
    public String toString() {
        return KnownUsersLoadFailedEvent.class.getSimpleName() + "(" + mReason + ")";
    }
}
