package org.msf.records.provider;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by Gil on 21/11/14.
 */
public class PatientContract {

    public PatientContract(){

    }

    /**
     * Content provider authority.
     */
    public static final String CONTENT_AUTHORITY = "org.msf.records";

    /**
     * Base URI. (content://org.msf.records)
     */
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    /**
     * Path component for "patient"-type resources..
     */
    private static final String PATH_PATIENTS = "patients";

    /**
     * Columns supported by "patients" records.
     */
    public static class Patient implements BaseColumns {
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
         * Fully qualified URI for "patient" resources.
         */
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_PATIENTS).build();

        /**
         * Table name where records are stored for "patient" resources.
         */
        public static final String TABLE_NAME = "patients";
        /**
         * Patient ID. (Note: Not to be confused with the database primary key, which is _ID.
         */
        public static final String COLUMN_NAME_PATIENT_ID = "patient_id";
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

    }
}
