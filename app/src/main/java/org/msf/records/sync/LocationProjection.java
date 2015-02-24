package org.msf.records.sync;

import org.msf.records.sync.providers.Contracts;

/**
* Contains database projections for locations and location names.
*/
public class LocationProjection {

    private static final String[] LOCATION_PROJECTION = {
            Contracts.Locations.LOCATION_UUID,
            Contracts.Locations.PARENT_UUID
    };

    private static final String[] LOCATION_NAMES_PROJECTION = {
            Contracts.LocationNames._ID,
            Contracts.LocationNames.LOCATION_UUID,
            Contracts.LocationNames.LOCALE,
            Contracts.LocationNames.LOCALIZED_NAME
    };

    public static final int LOCATION_LOCATION_UUID_COLUMN = 0;
    public static final int LOCATION_PARENT_UUID_COLUMN = 1;

    public static final int LOCATION_NAME_ID_COLUMN = 0;
    public static final int LOCATION_NAME_LOCATION_UUID_COLUMN = 1;
    public static final int LOCATION_NAME_LOCALE_COLUMN = 2;
    public static final int LOCATION_NAME_NAME_COLUMN = 3;

    public static String[] getLocationProjection() { return LOCATION_PROJECTION; }
    public static String[] getLocationNamesProjection() { return LOCATION_NAMES_PROJECTION; }

    private LocationProjection() {}
}
