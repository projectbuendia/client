// Copyright 2015 The Project Buendia Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at: http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distrib-
// uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
// specific language governing permissions and limitations under the License.

package org.projectbuendia.client.sync.providers;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

import org.projectbuendia.client.BuildConfig;

/** The external contracts for {@link BuendiaProvider}. */
@SuppressWarnings("unused")
public class Contracts {

    public static final String CONTENT_AUTHORITY = BuildConfig.CONTENT_AUTHORITY;
    private static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    private static final String TYPE_PACKAGE_PREFIX = "/vnd.projectbuendia.client.";
    
    public static Uri buildContentUri(String path) {
        return BASE_CONTENT_URI.buildUpon().appendPath(path).build();
    }
    public static String buildGroupType(String name) {
        return ContentResolver.CURSOR_DIR_BASE_TYPE + TYPE_PACKAGE_PREFIX + name;
    }
    public static String buildItemType(String name) {
        return ContentResolver.CURSOR_ITEM_BASE_TYPE + TYPE_PACKAGE_PREFIX + name;
    }

    public interface Tables {
        String PATIENTS = "patients";
        String CONCEPTS = "concepts";
        String CONCEPT_NAMES = "concept_names";
        String LOCATIONS = "locations";
        String LOCATION_NAMES = "location_names";
        String OBSERVATIONS = "observations";
        String ORDERS = "orders";
        String CHARTS = "charts";
        String USERS = "users";
        String MISC = "misc";

        String[] ALL = {
                PATIENTS, CONCEPTS, CONCEPT_NAMES, LOCATIONS,
                LOCATION_NAMES, OBSERVATIONS, ORDERS, CHARTS, USERS, MISC
        };
    }

    /** Columns for localized content. */
    interface LocaleColumns {
        /**
         * Really a language, but as Android does not have LanguageCode, encoded as a
         * java.util.Locale.toString(), eg en_US.
         */
        String LOCALE = "locale";

        /** The name of something in a given locale. */
        String NAME = "name";
    }

    /** Columns for a concept. */
    interface BaseConceptColumns {
        String CONCEPT_UUID = "concept_uuid";
    }

    /** Columns for an XForms concept. */
    interface ConceptColumns {
        /**
         * The id used to represent the concept in xforms (for client side parsing).
         * In reality this is the openmrs ID, but the client doesn't need to know that.
         */
        String XFORM_ID = "xform_id";

        /** Type for a concept like numeric, coded, etc. */
        String CONCEPT_TYPE = "concept_type";
    }

    interface ChartColumns {
        /** UUID for an encounter. */
        String CHART_UUID = "chart_uuid";

        /** Time for an encounter in seconds since epoch. */
        String CHART_ROW = "chart_row";

        /** UUID for a concept representing a group (section) in a chart. */
        String GROUP_UUID = "group_uuid";

        String CONCEPT_UUID = BaseConceptColumns.CONCEPT_UUID;
    }

    public interface ObservationColumns {
        String PATIENT_UUID = "patient_uuid";
        String ENCOUNTER_UUID = "encounter_uuid";
        String ENCOUNTER_TIME = "encounter_time";  // seconds since epoch
        String CONCEPT_UUID = BaseConceptColumns.CONCEPT_UUID;
        String VALUE = "value";  // concept value

        /**
         * Value is either 0 or 1, where 1 means this observation is cached
         * from an XForm that has not yet been sent.
         */
        String TEMP_CACHE = "temp_cache";
    }

    interface OrderColumns {
        String PATIENT_UUID = "patient_uuid";
        String INSTRUCTIONS = "instructions";
        String START_TIME = "start_time";  // seconds since epoch
        String STOP_TIME = "stop_time";  // seconds since epoch
    }

    interface LocationColumns {
        String LOCATION_UUID = "location_uuid";
        String PARENT_UUID = "parent_uuid"; // parent location or null
    }

    interface LocationNameColumns {
        String LOCATION_UUID = LocationColumns.LOCATION_UUID;
    }

    interface PatientColumns {
        String GIVEN_NAME = "given_name";
        String FAMILY_NAME = "family_name";
        String ADMISSION_TIMESTAMP = "admission_timestamp";
        String UUID = "uuid";
        String LOCATION_UUID = LocationColumns.LOCATION_UUID;
        String BIRTHDATE = "birthdate";
        String GENDER = "gender";
    }

    interface PatientCountColumns {
        String LOCATION_UUID = LocationColumns.LOCATION_UUID;

        /** Number of patients in a tent. */
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
        String NAME = LocaleColumns.NAME;

        /** The patient count for a single location, not including child locations. */
        String PATIENT_COUNT = "patient_count";
    }

    public static class Charts implements ChartColumns, BaseColumns {
        public static final Uri CONTENT_URI = buildContentUri("charts");
        public static final String GROUP_CONTENT_TYPE = buildGroupType("chart");
        public static final String ITEM_CONTENT_TYPE = buildItemType("chart");

        private Charts() {}
    }

    interface MiscColumns {
        /**
         * The start time of the last full sync operation, according to the
         * local (client's) clock.  Since sync operations are transactional,
         * this should only be set if this sync was completed successfully.
         *
         * <p>Updated at the very beginning of full sync operations.
         */
        String FULL_SYNC_START_TIME = "full_sync_start_time";

        /**
         * The end time of the last full sync operation, according to the
         * local (client's) clock.  In rare cases, this may correspond to a
         * sync that completed but downloaded incomplete data.
         *
         * <p>Updated at the very end of full sync operations.
         */
        String FULL_SYNC_END_TIME = "full_sync_end_time";

        /**
         * The "snapshot time" of the last observation sync operation, according
         * to the server's clock.  This is used to request an incremental update
         * of observations from the server.
         *
         * <p>Updated after observation sync to the snapshot time reported by the server.
         */
        String OBS_SYNC_TIME = "obs_sync_time";
    }

    public static class ConceptNames implements BaseConceptColumns, LocaleColumns, BaseColumns {
        public static final Uri CONTENT_URI = buildContentUri("concept-names");
        public static final String GROUP_CONTENT_TYPE = buildGroupType("concept-name");
        public static final String ITEM_CONTENT_TYPE = buildItemType("concept-name");

        private ConceptNames() {}
    }

    public static class Concepts implements ConceptColumns, BaseColumns {
        public static final Uri CONTENT_URI = buildContentUri("concepts");
        public static final String GROUP_CONTENT_TYPE = buildGroupType("concept");
        public static final String ITEM_CONTENT_TYPE = buildItemType("concept");

        private Concepts() {}
    }

    public static class Locations implements LocationColumns, BaseColumns {
        public static final Uri CONTENT_URI = buildContentUri("locations");
        public static final String GROUP_CONTENT_TYPE = buildGroupType("location");
        public static final String ITEM_CONTENT_TYPE = buildItemType("location");

        private Locations() {}
    }

    public static class LocationNames implements LocationNameColumns, LocaleColumns, BaseColumns {
        public static final Uri CONTENT_URI = buildContentUri("location-names");
        public static final String GROUP_CONTENT_TYPE = buildGroupType("location-name");
        public static final String ITEM_CONTENT_TYPE = buildItemType("location-name");

        private LocationNames() {}
    }

    public static class Observations implements ObservationColumns, BaseColumns {
        public static final Uri CONTENT_URI = buildContentUri("observations");
        public static final String GROUP_CONTENT_TYPE = buildGroupType("observation");
        public static final String ITEM_CONTENT_TYPE = buildItemType("observation");

        private Observations() {}
    }

    public static class Orders implements OrderColumns, BaseColumns {
        public static final Uri CONTENT_URI = buildContentUri("orders");
        public static final String GROUP_CONTENT_TYPE = buildGroupType("order");
        public static final String ITEM_CONTENT_TYPE = buildItemType("order");

        private Orders() {}
    }

    public static class Patients implements PatientColumns, BaseColumns {
        public static final Uri CONTENT_URI = buildContentUri("patients");
        public static final String GROUP_CONTENT_TYPE = buildGroupType("patient");
        public static final String ITEM_CONTENT_TYPE = buildItemType("patient");

        private Patients() {}
    }

    public static class PatientCounts implements PatientCountColumns {
        public static final Uri CONTENT_URI = buildContentUri("patient-counts");
        public static final String GROUP_CONTENT_TYPE = buildGroupType("patient-count");
        public static final String ITEM_CONTENT_TYPE = buildItemType("patient-count");

        private PatientCounts() {}
    }

    public static class LocalizedCharts implements LocalizedChartColumns {
        public static final Uri CONTENT_URI = buildContentUri("localized-charts");
        public static final String GROUP_CONTENT_TYPE = buildGroupType("localized-chart");
        public static final String ITEM_CONTENT_TYPE = buildItemType("localized-chart");
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
        public static final Uri CONTENT_URI = buildContentUri("most-recent-localized-charts");
        public static final String GROUP_CONTENT_TYPE = buildGroupType("localized-chart");
        public static final String ITEM_CONTENT_TYPE = buildItemType("localized-chart");

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
        public static final Uri CONTENT_URI = buildContentUri("localized-locations");
        public static final String GROUP_CONTENT_TYPE = buildGroupType("localized-location");
        public static final String ITEM_CONTENT_TYPE = buildItemType("localized-location");

        /** Returns the content URL for the localized locations for a given locale. */
        public static Uri getUri(String locale) {
            return CONTENT_URI.buildUpon()
                    .appendPath(locale)
                    .build();
        }

        private LocalizedLocations() {}
    }

    public static class Users implements UserColumns, BaseColumns {
        public static final Uri CONTENT_URI = buildContentUri("users");
        public static final String GROUP_CONTENT_TYPE = buildGroupType("user");
        public static final String ITEM_CONTENT_TYPE = buildItemType("user");

        private Users() {}
    }

    public static class Misc implements MiscColumns, BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath("misc").appendPath("0").build();
        public static final String ITEM_CONTENT_TYPE = buildItemType("misc");

        private Misc() {}
    }

    private Contracts() {}
}
