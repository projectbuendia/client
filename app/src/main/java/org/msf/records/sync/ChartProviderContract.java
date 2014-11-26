package org.msf.records.sync;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Columns and other ContentProvider related interface Strings for accessing chart (history of
 * patient observations) data.
 */
public class ChartProviderContract {

    /**
     * Collection of static Strings so should not be instantiated.
     */
    private ChartProviderContract(){
    }

    /**
     * MIME type for lists of observations.
     */
    public static final String OBSERVATION_CONTENT_TYPE =
            ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.records.observation";

    /**
     * MIME type for lists of concept names.
     */
    public static final String CONCEPT_CONTENT_TYPE =
            ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.records.concept";

    /**
     * MIME type for lists of concept names.
     */
    public static final String CONCEPT_NAME_CONTENT_TYPE =
            ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.records.conceptname";

    /**
     * MIME type for lists of concept names.
     */
    public static final String CHART_CONTENT_TYPE =
            ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.records.chart";

    /**
     * Base URI. (content://org.msf.records)
     */
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" +
            PatientProviderContract.CONTENT_AUTHORITY);

    /**
     * Path component for the concepts table.
     */
    static final String PATH_CONCEPTS = "concepts";
    /**
     * Path component for the concepts table.
     */
    static final String PATH_CONCEPT_NAMES = "concept_names";
    /**
     * Path component for the concepts table.
     */
    static final String PATH_OBSERVATIONS = "observations";
    /**
     * Path component for the charts table.
     */
    static final String PATH_CHARTS = "charts";

    public static final Uri OBSERVATIONS_CONTENT_URI =
            BASE_CONTENT_URI.buildUpon().appendPath(PATH_OBSERVATIONS).build();
    public static final Uri CONCEPT_NAMES_CONTENT_URI =
            BASE_CONTENT_URI.buildUpon().appendPath(PATH_CONCEPT_NAMES).build();
    public static final Uri CONCEPTS_CONTENT_URI =
            BASE_CONTENT_URI.buildUpon().appendPath(PATH_CONCEPTS).build();
    public static final Uri CHART_CONTENT_URI =
            BASE_CONTENT_URI.buildUpon().appendPath(PATH_CHARTS).build();

    /**
     * Columns supported by "patients" records.
     */
    public static class ChartColumns implements BaseColumns {

        /**
         * UUID for a concept.
         */
        public static final String CONCEPT_UUID = "concept_uuid";

        /**
         * Type for a concept like numeric, coded, etc.
         */
        public static final String CONCEPT_TYPE = "concept_type";
        /**
         * Really a language, but as Android does not have LanguageCode, encoded as a
         * java.util.Locale.toString(), eg en_US.
         */
        public static final String LOCALE = "locale";
        /**
         * The name of a concept in a given locale to display to a user.
         */
        public static final String NAME = "name";
        /**
         * UUID for a patient.
         */
        public static final String PATIENT_UUID = "patient_uuid";
        /**
         * UUID for an encounter.
         */
        public static final String ENCOUNTER_UUID = "encounter_uuid";
        /**
         * Time for an encounter in seconds since epoch.
         */
        public static final String ENCOUNTER_TIME = "encounter_time";
        /**
         * The value of a concept in an encounter.
         */
        public static final String VALUE = "value";
        /**
         * UUID for an encounter.
         */
        public static final String CHART_UUID = "chart_uuid";
        /**
         * Time for an encounter in seconds since epoch.
         */
        public static final String CHART_ROW = "chart_row";
        /**
         * UUID for a concept representing a group (section) in a chart.
         */
        public static final String GROUP_UUID = "group_uuid";
    }
}
