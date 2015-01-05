package org.msf.records.sync;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Columns and other ContentProvider related interface Strings for accessing location data.
 */
public class LocationProviderContract {

    /**
     * Collection of static Strings so should not be instantiated.
     */
    private LocationProviderContract(){
    }

    /**
     * MIME type for lists of locations
     */
    public static final String LOCATION_CONTENT_TYPE =
            ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.records.location";

    /**
     * MIME type for lists of location names.
     */
    public static final String LOCATION_NAME_CONTENT_TYPE =
            ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.records.locationname";

    /**
     * Base URI. (content://org.msf.records)
     */
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" +
            PatientProviderContract.CONTENT_AUTHORITY);

    /**
     * Path component for a locations subtree rooted at a specific location.
     */
    static final String PATH_SUBLOCATIONS = "sublocations";

    /**
     * Path component for the locations table.
     */
    static final String PATH_LOCATIONS = "locations";

    /**
     * Path component for localized locations.
     */
    static final String PATH_LOCALIZED_LOCATIONS = "localizedlocations";

    /**
     * Path component for the location names table.
     */
    static final String PATH_LOCATION_NAMES = "location_names";

    public static final Uri LOCATION_NAMES_CONTENT_URI =
            BASE_CONTENT_URI.buildUpon().appendPath(PATH_LOCATION_NAMES).build();
    public static final Uri LOCATIONS_CONTENT_URI =
            BASE_CONTENT_URI.buildUpon().appendPath(PATH_LOCATIONS).build();

    // TODO(dxchen): Implement localized location fetching.
    public static final Uri LOCALIZED_LOCATIONS_CONTENT_URI =
            BASE_CONTENT_URI.buildUpon().appendPath(PATH_LOCALIZED_LOCATIONS).build();

    /**
     * Columns supported by the various location URIs.
     */
    public static class LocationColumns implements BaseColumns {
        /**
         * UUID for a location.
         */
        public static final String LOCATION_UUID = "location_uuid";

        /**
         * UUID for a parent location, or null if there is no parent.
         */
        public static final String PARENT_UUID = "parent_uuid";

        /**
         * Really a language, but as Android does not have LanguageCode, encoded as a
         * java.util.Locale.toString(), eg en_US.
         */
        public static final String LOCALE = "locale";

        /**
         * The name of a concept in a given locale to display to a user.
         */
        public static final String NAME = "name";
    }
}
