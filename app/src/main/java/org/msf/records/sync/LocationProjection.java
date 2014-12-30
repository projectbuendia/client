package org.msf.records.sync;

/**
 * Contains database projections for locations and location names.
 */
public class LocationProjection {

    private static final String[] LOCATION_PROJECTION = {
            LocationProviderContract.LocationColumns.LOCATION_UUID,
            LocationProviderContract.LocationColumns.PARENT_UUID
    };

    private static final String[] LOCATION_NAMES_PROJECTION = {
            LocationProviderContract.LocationColumns._ID,
            LocationProviderContract.LocationColumns.LOCATION_UUID,
            LocationProviderContract.LocationColumns.LOCALE,
            LocationProviderContract.LocationColumns.NAME
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
