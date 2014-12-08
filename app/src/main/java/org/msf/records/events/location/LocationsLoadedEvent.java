package org.msf.records.events.location;

import org.msf.records.location.LocationTree;

/**
 * An event bus event that indicates that locations have been successfully loaded from the
 * database.
 */
public class LocationsLoadedEvent {
    public final LocationTree mLocationTree;

    public LocationsLoadedEvent(LocationTree locationTree) {
        mLocationTree = locationTree;
    }
}
