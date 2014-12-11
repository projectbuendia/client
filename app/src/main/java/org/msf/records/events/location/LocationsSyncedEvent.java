package org.msf.records.events.location;

/**
 * An event bus event that indicates that locations have successfully been synced to the client
 * database (but have not necessarily been applied to the LocationTree model).
 */

public class LocationsSyncedEvent {
    public LocationsSyncedEvent() {}
}
