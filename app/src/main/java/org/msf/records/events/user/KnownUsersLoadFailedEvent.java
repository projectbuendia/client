package org.msf.records.events.user;

/**
 * An event bus event indicating that the set of known users failed to be loaded from local cache.
 */
public final class KnownUsersLoadFailedEvent {

    public static final int REASON_UNKNOWN = 0;
    public static final int REASON_NO_USERS_RETURNED = 1;
    public static final int REASON_CANCELLED = 2;

    public final int reason;

    public KnownUsersLoadFailedEvent(int reason) {
        this.reason = reason;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof KnownUsersLoadFailedEvent)) {
            return false;
        }

        KnownUsersLoadFailedEvent other = (KnownUsersLoadFailedEvent) obj;
        return other.reason == reason;
    }

    @Override
    public int hashCode() {
        return reason;
    }

    @Override
    public String toString() {
        return KnownUsersLoadFailedEvent.class.getSimpleName() + "(" + reason + ")";
    }
}
