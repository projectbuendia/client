package org.msf.records.events.location;

/**
 * An event bus event that indicates that a failure has occurred while syncing locations from the
 * server.
 */
public class LocationsSyncFailedEvent {
    public static final int REASON_UNKNOWN = 0;
    public static final int REASON_TREE_CONSTRUCTION_ERROR = 1;
    public static final int REASON_SERVER_ERROR = 2;

    public final int reason;

    public LocationsSyncFailedEvent(int reason) {
        this.reason = reason;
    }
}
