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
    private ChartProviderContract() {}

    /**
     * Base URI. (content://org.msf.records)
     */
    public static final Uri BASE_CONTENT_URI =
            Uri.parse("content://" + PatientProviderContract.CONTENT_AUTHORITY);

    /**
     * Path component for the concepts table.
     */
    static final String PATH_CONCEPTS = "concepts";
    /**
     * Path component for the concepts table.
     */
    static final String PATH_CONCEPT_NAMES = "concept-names";
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

    private static final String LOCALIZED_CHARTS_PREFIX = "localized-charts";

    /**
     * Make a special URI that will be used most often in the user interface.
     * Get data for a given patient in a given language for a given chart.
     *
     * content://org.msf.records/localizedchart/{chart_uuid}/{locale}/{patient_uuid}
     */
    public static Uri makeLocalizedChartUri(String chartUuid, String patientUuid, String locale) {
        return BASE_CONTENT_URI.buildUpon().appendPath(LOCALIZED_CHARTS_PREFIX)
                .appendPath(chartUuid)
                .appendPath(locale)
                .appendPath(patientUuid)
                .build();
    }

    /**
     * Make a special URI that will be used most often in the user interface.
     * Get data for a given patient in a given language for a given chart.
     *
     * content://org.msf.records/localizedchart/{chart_uuid}/{locale}/{patient_uuid}
     */
    public static Uri makeEmptyLocalizedChartUri(String chartUuid, String locale) {
        return BASE_CONTENT_URI.buildUpon().appendPath(LOCALIZED_CHARTS_PREFIX)
                .appendPath(chartUuid)
                .appendPath(locale)
                .build();
    }

    private static final String MOST_RECENT_CHARTS_PREFIX = "most-recent-localized-charts";

    /**
     * Make a special URI that will be used most often in the user interface.
     * Get data for a given patient in a given language for a given chart.
     *
     * content://org.msf.records/localizedchart/{chart_uuid}/{patient_uuid}/{locale}
     */
    public static Uri makeMostRecentChartUri(String patientUuid, String locale) {
        return BASE_CONTENT_URI.buildUpon().appendPath(MOST_RECENT_CHARTS_PREFIX)
                .appendPath(patientUuid)
                .appendPath(locale)
                .build();
    }

    /**
     * Columns supported by the various patient chart URIs.
     */
    public static class ChartColumns implements BaseColumns {

        /**
         * UUID for a concept.
         */
        public static final String CONCEPT_UUID = "concept_uuid";

        /**
         * The id used to represent the concept in xforms (for client side parsing).
         * In reality this is the openmrs ID, but the client doesn't need to know that.
         */
        public static final String XFORM_ID = "xform_id";
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
        /**
         * A boolean (0 or 1) column indicating 1 if this is an observation cached from an xform
         * that has not yet been sent.
         */
        public static final String TEMP_CACHE = "temp_cache";
    }
}
