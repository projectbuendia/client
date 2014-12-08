package org.msf.records.events.location;

/**
 * An event bus event that indicates that a failure has occurred while loading locations from the
 * database.
 */
public class LocationsLoadFailedEvent {
    public static final int REASON_UNKNOWN = 0;
    public static final int REASON_TREE_CONSTRUCTION_ERROR = 1;
    public static final int REASON_DB_ERROR = 2;
    public static final int REASON_SERVER_ERROR = 3;

    private final int mReason;

    public LocationsLoadFailedEvent(int reason) {
        mReason = reason;
    }
}
