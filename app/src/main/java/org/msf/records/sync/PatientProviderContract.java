package org.msf.records.sync;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * A collection of static variables used in the ContentProvider interface for Patients.
 */
public class PatientProviderContract {

    /**
     * Collection of static Strings so should not be instantiated.
     */
    private PatientProviderContract(){
    }

    /**
     * MIME type for lists of patients.
     */
    public static final String CONTENT_TYPE =
            ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.records.patients";
    /**
     * MIME type for individual patients.
     */
    public static final String CONTENT_ITEM_TYPE =
            ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.records.patient";

    /**
     * Content provider authority.
     */
    public static final String CONTENT_AUTHORITY = "org.msf.records";

    /**
     * Base URI. (content://org.msf.records)
     */
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    /**
     * Path component for "patient"-type resources. Visible for re-use in PatientProvider.
     */
    static final String PATH_PATIENTS = "patients";

    /**
     * Path component for "zones"-type resources.  Visible for re-use in PatientProvider.
     */
    static final String PATH_PATIENTS_ZONES = "zones";

    /**
     * Path component for "tents"-type resources. Visible for re-use in PatientProvider.
     */
    static final String PATH_PATIENTS_TENTS = "tents";

    /**
     * Fully qualified URI for "patient" resources.
     */
    public static final Uri CONTENT_URI =
            BASE_CONTENT_URI.buildUpon().appendPath(PATH_PATIENTS).build();

    public static final Uri CONTENT_URI_PATIENT_ZONES =
            BASE_CONTENT_URI.buildUpon().appendPath(PATH_PATIENTS_ZONES).build();

    public static final Uri CONTENT_URI_PATIENT_TENTS =
            BASE_CONTENT_URI.buildUpon().appendPath(PATH_PATIENTS_TENTS).build();

    /**
     * Columns supported by "patients" records.
     */
    public static class PatientColumns implements BaseColumns {

        /**
         * Patient admission timestamp
         */
        public static final String COLUMN_NAME_ADMISSION_TIMESTAMP = "admission_timestamp";
        /**
         * Patient family name
         */
        public static final String COLUMN_NAME_FAMILY_NAME = "family_name";
        /**
         * Patient admission timestamp
         */
        public static final String COLUMN_NAME_GIVEN_NAME = "given_name";
        /**
         * Patient Status
         */
        public static final String COLUMN_NAME_STATUS = "status";
        /**
         * Patient uuid
         */
        public static final String COLUMN_NAME_UUID = "uuid";
        /**
         * Patient zone
         */
        public static final String COLUMN_NAME_LOCATION_ZONE = "location_zone";
        /**
         * Patient tent
         */
        public static final String COLUMN_NAME_LOCATION_TENT = "location_tent";
    }
}
