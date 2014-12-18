package org.msf.records.sync;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

import org.msf.records.BuildConfig;

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
    public static final String CONTENT_AUTHORITY = BuildConfig.APPLICATION_ID + ".provider.records";

    /**
     * Base URI. (content://org.msf.records)
     */
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    /**
     * Path component for "patient"-type resources. Visible for re-use in MsfRecordsProvider.
     */
    static final String PATH_PATIENTS = "patients";

    /**
     * Path component for counts of patients per tent. Visible for re-use in MsfRecordsProvider.
     */
    static final String PATH_TENT_PATIENT_COUNTS = "tentpatients";

    /**
     * Fully qualified URI for "patient" resources.
     */
    public static final Uri CONTENT_URI =
            BASE_CONTENT_URI.buildUpon().appendPath(PATH_PATIENTS).build();

    public static final Uri CONTENT_URI_TENT_PATIENT_COUNTS =
            BASE_CONTENT_URI.buildUpon().appendPath(PATH_TENT_PATIENT_COUNTS).build();

    /** Columns supported by "patient" records. */
    public static class PatientColumns implements BaseColumns {
        /** Patient admission timestamp */
        public static final String COLUMN_NAME_ADMISSION_TIMESTAMP = "admission_timestamp";
        /** Patient family name */
        public static final String COLUMN_NAME_FAMILY_NAME = "family_name";
        /** Patient admission timestamp */
        public static final String COLUMN_NAME_GIVEN_NAME = "given_name";
        /*** Patient UUID */
        public static final String COLUMN_NAME_UUID = "uuid";
        /** Patient location UUID */
        public static final String COLUMN_NAME_LOCATION_UUID = "location_uuid";
        /** Patient birthdate (yyyy-mm-dd) */
        public static final String COLUMN_NAME_BIRTHDATE = "birthdate";
        /** Patient gender */
        public static final String COLUMN_NAME_GENDER = "gender";

        // TODO(kpy): The following column does not exist in the patients table;
        // it is used to support queries to /tentpatients/.  Clean this up.
        /** Patient count per tent */
        public static final String COLUMN_NAME_TENT_PATIENT_COUNT = "tent_patient_count";
    }
}
