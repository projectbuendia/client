package org.msf.records.sync.providers;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

import org.msf.records.BuildConfig;

import java.util.Locale;

/**
 * The external contracts for {@link MsfRecordsProvider}.
 */
@SuppressWarnings("unused")
public class Contracts {

    // TODO(dxchen): The content authority should be defined somewhere else. It's not strictly
    // limited to ContentProviders.
    public static final String CONTENT_AUTHORITY = BuildConfig.APPLICATION_ID + ".provider.records";

    private static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    private static final String TYPE_PACKAGE_PREFIX = "/vnd.msf.records.";

    /**
     * Columns for localized content.
     */
    interface LocaleColumns {

        /**
         * Really a language, but as Android does not have LanguageCode, encoded as a
         * java.util.Locale.toString(), eg en_US.
         */
        String LOCALE = "locale";

        /**
         * The name of something in a given locale.
         */
        String LOCALIZED_NAME = "name";
    }

    /**
     * Columns for a concept.
     */
    interface BaseConceptColumns {

        /**
         * UUID for a concept.
         */
        String CONCEPT_UUID = "concept_uuid";
    }

    /**
     * Columns for an XForms concept.
     */
    interface ConceptColumns {

        /**
         * The id used to represent the concept in xforms (for client side parsing).
         * In reality this is the openmrs ID, but the client doesn't need to know that.
         */
        String XFORM_ID = "xform_id";

        /**
         * Type for a concept like numeric, coded, etc.
         */
        String CONCEPT_TYPE = "concept_type";
    }

    interface ChartColumns {

        /**
         * UUID for an encounter.
         */
        String CHART_UUID = "chart_uuid";

        /**
         * Time for an encounter in seconds since epoch.
         */
        String CHART_ROW = "chart_row";

        /**
         * UUID for a concept representing a group (section) in a chart.
         */
        String GROUP_UUID = "group_uuid";

        String CONCEPT_UUID = BaseConceptColumns.CONCEPT_UUID;
    }

    public interface ObservationColumns {

        /**
         * UUID for a patient.
         */
        String PATIENT_UUID = "patient_uuid";

        /**
         * UUID for an encounter.
         */
        String ENCOUNTER_UUID = "encounter_uuid";

        /**
         * Time for an encounter in seconds since epoch.
         */
        String ENCOUNTER_TIME = "encounter_time";

        /**
         * The value of a concept in an encounter.
         */
        String VALUE = "value";

        /**
         * A boolean (0 or 1) column indicating 1 if this is an observation cached from an xform
         * that has not yet been sent.
         */
        String TEMP_CACHE = "temp_cache";

        String CONCEPT_UUID = BaseConceptColumns.CONCEPT_UUID;
    }

    interface LocationColumns {

        /**
         * UUID for a location.
         */
        String LOCATION_UUID = "location_uuid";

        /**
         * UUID for a parent location, or null if there is no parent.
         */
        String PARENT_UUID = "parent_uuid";
    }

    interface LocationNameColumns {

        String LOCATION_UUID = LocationColumns.LOCATION_UUID;
    }

    interface PatientColumns {

        /**
         * Admission timestamp.
         */
        String ADMISSION_TIMESTAMP = "admission_timestamp";
        
        /**
         * Family name.
         */
        String FAMILY_NAME = "family_name";
        
        /**
         * Given name.
         */
        String GIVEN_NAME = "given_name";
        
        /**
         * UUID.
         */
        String UUID = "uuid";

        String LOCATION_UUID = LocationColumns.LOCATION_UUID;
        
        /**
         * Birthdate.
         */
        String BIRTHDATE = "birthdate";
        
        /**
         * Gender.
         */
        String GENDER = "gender";
    }
    
    interface PatientCountColumns {

        String LOCATION_UUID = LocationColumns.LOCATION_UUID;

        /**
         * Number of patients in a tent.
         */
        String TENT_PATIENT_COUNT = "tent_patient_count";
    }

    interface LocalizedChartColumns {

        String ENCOUNTER_TIME = ObservationColumns.ENCOUNTER_TIME;

        String GROUP_NAME = "group_name";

        String CONCEPT_UUID = BaseConceptColumns.CONCEPT_UUID;

        String CONCEPT_NAME = "concept_name";

        String VALUE = "value";

        String LOCALIZED_VALUE = "localized_value";
    }

    interface UserColumns {

        String UUID = "uuid";

        String FULL_NAME = "full_name";
    }

    interface LocalizedLocationColumns {

        String LOCATION_UUID = LocationColumns.LOCATION_UUID;

        String PARENT_UUID = LocationColumns.PARENT_UUID;

        String LOCALIZED_NAME = LocaleColumns.LOCALIZED_NAME;

        /** The patient count for a single location, not including child locations. */
        String PATIENT_COUNT = "patient_count";
    }

    public static class Charts implements ChartColumns, BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath("charts").build();

        public static final String GROUP_CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + TYPE_PACKAGE_PREFIX + "chart";

        public static final String ITEM_CONTENT_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + TYPE_PACKAGE_PREFIX + "chart";

        private Charts() {}
    }

    interface MiscColumns {
        /**
         * The start time of the last full sync operation. Since sync operations are transactional,
         * this should only be set if this sync was completed successfully.
         *
         * <p>Updated at the very beginning of full sync operations.
         */
        String FULL_SYNC_START_TIME = "full_sync_start_time";

        /**
         * The end time of the last full sync operation. In rare cases, this may correspond to a
         * sync that completed but downloaded incomplete data.
         *
         * <p>Updated at the very end of full sync operations.
         */
        String FULL_SYNC_END_TIME = "full_sync_end_time";

        /**
         * The encounter time of the last observation sync operation, allowing for an incremental
         * update of observations.
         *
         * <p>Updated after observations are synced to the encounter time of the latest observation.
         */
        String OBS_SYNC_TIME = "obs_sync_time";
    }

    public static class ConceptNames implements BaseConceptColumns, LocaleColumns, BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath("concept-names").build();

        public static final String GROUP_CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + TYPE_PACKAGE_PREFIX + "concept-name";

        public static final String ITEM_CONTENT_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + TYPE_PACKAGE_PREFIX + "concept-name";

        private ConceptNames() {}
    }

    public static class Concepts implements ConceptColumns, BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath("concepts").build();

        public static final String GROUP_CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + TYPE_PACKAGE_PREFIX + "concept";

        public static final String ITEM_CONTENT_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + TYPE_PACKAGE_PREFIX + "concept";

        private Concepts() {}
    }

    public static class Locations implements LocationColumns, BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath("locations").build();

        public static final String GROUP_CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + TYPE_PACKAGE_PREFIX + "location";

        public static final String ITEM_CONTENT_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + TYPE_PACKAGE_PREFIX + "location";

        private Locations() {}
    }

    public static class LocationNames implements LocationNameColumns, LocaleColumns, BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath("location-names").build();

        public static final String GROUP_CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + TYPE_PACKAGE_PREFIX + "location-name";

        public static final String ITEM_CONTENT_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + TYPE_PACKAGE_PREFIX + "location-name";

        private LocationNames() {}
    }

    public static class Observations implements ObservationColumns, BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath("observations").build();

        public static final String GROUP_CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + TYPE_PACKAGE_PREFIX + "observation";

        public static final String ITEM_CONTENT_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + TYPE_PACKAGE_PREFIX + "observation";

        private Observations() {}
    }

    public static class Patients implements PatientColumns, BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath("patients").build();

        public static final String GROUP_CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + TYPE_PACKAGE_PREFIX + "patient";

        public static final String ITEM_CONTENT_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + TYPE_PACKAGE_PREFIX + "patient";

        private Patients() {}
    }

    public static class PatientCounts implements PatientCountColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath("patient-counts").build();

        public static final String GROUP_CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + TYPE_PACKAGE_PREFIX + "patient-count";

        public static final String ITEM_CONTENT_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + TYPE_PACKAGE_PREFIX + "patient-count";

        private PatientCounts() {}
    }

    public static class LocalizedCharts implements LocalizedChartColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath("localized-charts").build();

        public static final String GROUP_CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + TYPE_PACKAGE_PREFIX + "localized-chart";

        public static final String ITEM_CONTENT_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + TYPE_PACKAGE_PREFIX + "localized-chart";
        private static final String FAKE_PATIENT_UUID = "fake-patient";

        /**
         * Returns the content URI for a localized chart for a given chart UUID, patient UUID, and
         * locale.
         */
        public static Uri getLocalizedChartUri(
                String chartUuid, String patientUuid, String locale) {
            return CONTENT_URI.buildUpon()
                    .appendPath(chartUuid)
                    .appendPath(locale)
                    .appendPath(patientUuid)
                    .build();
        }

        /**
         * Returns the content URI for an empty localized chart for a given chart UUID and locale.
         */
        public static Uri getEmptyLocalizedChartUri(
                String chartUuid, String locale) {
            return CONTENT_URI.buildUpon()
                    .appendPath(chartUuid)
                    .appendPath(locale)
                    .appendPath(FAKE_PATIENT_UUID) // Don't expect to match any real observations.
                    .build();
        }

        private LocalizedCharts() {}
    }

    public static class MostRecentLocalizedCharts implements LocalizedChartColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath("most-recent-localized-charts").build();

        public static final String GROUP_CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + TYPE_PACKAGE_PREFIX + "localized-chart";

        public static final String ITEM_CONTENT_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + TYPE_PACKAGE_PREFIX + "localized-chart";

        /**
         * Returns the content URI for the most recent localized chart for a given patient UUID and
         * locale.
         */
        public static Uri getMostRecentChartUri(String patientUuid, String locale) {
            return CONTENT_URI.buildUpon()
                    .appendPath(patientUuid)
                    .appendPath(locale)
                    .build();
        }

        private MostRecentLocalizedCharts() {}
    }

    public static class LocalizedLocations implements LocalizedLocationColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath("localized-locations").build();

        public static final String GROUP_CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + TYPE_PACKAGE_PREFIX + "localized-location";

        public static final String ITEM_CONTENT_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + TYPE_PACKAGE_PREFIX + "localized-location";

        /**
         * Returns the content URL for the localized locations for a given locale.
         */
        public static Uri getUri(String locale) {
            return CONTENT_URI.buildUpon()
                    .appendPath(locale)
                    .build();
        }

        private LocalizedLocations() {}
    }

    public static class Users implements UserColumns, BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath("users").build();

        public static final String GROUP_CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + TYPE_PACKAGE_PREFIX + "user";

        public static final String ITEM_CONTENT_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + TYPE_PACKAGE_PREFIX + "user";

        private Users() {}
    }

    public static class Misc implements MiscColumns, BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath("misc").appendPath("0").build();

        public static final String ITEM_CONTENT_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + TYPE_PACKAGE_PREFIX + "misc";

        private Misc() {}
    }

    private Contracts() {}
}
