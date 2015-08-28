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

    public enum Table {
        CHARTS("charts"),
        CONCEPT_NAMES("concept_names"),
        CONCEPTS("concepts"),
        FORMS("forms"),
        LOCATION_NAMES("location_names"),
        LOCATIONS("locations"),
        MISC("misc"),
        OBSERVATIONS("observations"),
        ORDERS("orders"),
        PATIENTS("patients"),
        USERS("users");

        public String name;
        Table(String name) { this.name = name; }
        public String toString() { return name; }
    }

    // Each interface below corresponds to one SQLite table.  The column names
    // defined in the constants should match the schemas defined in Database.java.

    public interface Charts extends BaseColumns {
        Uri CONTENT_URI = buildContentUri("charts");
        String GROUP_CONTENT_TYPE = buildGroupType("chart");
        String ITEM_CONTENT_TYPE = buildItemType("chart");

        String CHART_UUID = "chart_uuid";
        String CHART_ROW = "chart_row";  // 0-based row number in the chart history grid
        String CONCEPT_UUID = "concept_uuid";  // the concept displayed in that row
        String GROUP_UUID = "group_uuid";  // UUID of a concept for a section grouping
    }

    public interface ConceptNames extends BaseColumns {
        Uri CONTENT_URI = buildContentUri("concept-names");
        String GROUP_CONTENT_TYPE = buildGroupType("concept-name");
        String ITEM_CONTENT_TYPE = buildItemType("concept-name");

        String CONCEPT_UUID = "concept_uuid";
        String LOCALE = "locale";  // a language encoded as a java.util.Locale.toString()
        String NAME = "name";
    }

    public interface Concepts extends BaseColumns {
        Uri CONTENT_URI = buildContentUri("concepts");
        String GROUP_CONTENT_TYPE = buildGroupType("concept");
        String ITEM_CONTENT_TYPE = buildItemType("concept");

        String XFORM_ID = "xform_id";  // ID for the concept in XForms (OpenMRS ID)
        String CONCEPT_TYPE = "concept_type";  // data type name, e.g. NUMERIC, TEXT
    }

    public interface Forms extends BaseColumns {
        Uri CONTENT_URI = buildContentUri("forms");
        String GROUP_CONTENT_TYPE = buildGroupType("form");
        String ITEM_CONTENT_TYPE = buildItemType("form");

        String UUID = "uuid";
        String NAME = "name";
        String VERSION = "version";
    }

    public interface LocationNames extends BaseColumns {
        Uri CONTENT_URI = buildContentUri("location-names");
        String GROUP_CONTENT_TYPE = buildGroupType("location-name");
        String ITEM_CONTENT_TYPE = buildItemType("location-name");

        String LOCATION_UUID = "location_uuid";
        String LOCALE = "locale";  // a language encoded as a java.util.Locale.toString()
        String NAME = "name";
    }

    public interface Locations extends BaseColumns {
        Uri CONTENT_URI = buildContentUri("locations");
        String GROUP_CONTENT_TYPE = buildGroupType("location");
        String ITEM_CONTENT_TYPE = buildItemType("location");

        String LOCATION_UUID = "location_uuid";
        String PARENT_UUID = "parent_uuid"; // parent location or null
    }

    public interface Misc extends BaseColumns {
        Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath("misc").appendPath("0").build();
        String ITEM_CONTENT_TYPE = buildItemType("misc");

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

    public interface Observations extends BaseColumns {
        Uri CONTENT_URI = buildContentUri("observations");
        String GROUP_CONTENT_TYPE = buildGroupType("observation");
        String ITEM_CONTENT_TYPE = buildItemType("observation");

        String PATIENT_UUID = "patient_uuid";
        String ENCOUNTER_UUID = "encounter_uuid";
        String ENCOUNTER_TIME = "encounter_time";  // seconds since epoch
        String CONCEPT_UUID = "concept_uuid";
        String VALUE = "value";  // concept value or order UUID

        /**
         * temp_cache is 1 if this observation was written locally as a cached
         * value from a submitted XForm, or 0 if it was loaded from the server.
         */
        String TEMP_CACHE = "temp_cache";
    }

    public interface Orders extends BaseColumns {
        Uri CONTENT_URI = buildContentUri("orders");
        String GROUP_CONTENT_TYPE = buildGroupType("order");
        String ITEM_CONTENT_TYPE = buildItemType("order");

        String UUID = "uuid";
        String PATIENT_UUID = "patient_uuid";
        String INSTRUCTIONS = "instructions";
        String START_TIME = "start_time";  // seconds since epoch
        String STOP_TIME = "stop_time";  // seconds since epoch
    }

    public interface Patients extends BaseColumns {
        Uri CONTENT_URI = buildContentUri("patients");
        String GROUP_CONTENT_TYPE = buildGroupType("patient");
        String ITEM_CONTENT_TYPE = buildItemType("patient");

        String UUID = "uuid";
        String GIVEN_NAME = "given_name";
        String FAMILY_NAME = "family_name";
        String LOCATION_UUID = "location_uuid";
        String BIRTHDATE = "birthdate";
        String GENDER = "gender";
    }

    public interface Users extends BaseColumns {
        Uri CONTENT_URI = buildContentUri("users");
        String GROUP_CONTENT_TYPE = buildGroupType("user");
        String ITEM_CONTENT_TYPE = buildItemType("user");

        String UUID = "uuid";
        String FULL_NAME = "full_name";
    }

    // Each interface below describes a derived view implemented by a custom
    // ProviderDelegate.  The column name constants should match the columns
    // returned by the query() method of the corresponding ProviderDelegate.

    public interface LocalizedLocations extends BaseColumns {
        Uri CONTENT_URI = buildContentUri("localized-locations");
        String GROUP_CONTENT_TYPE = buildGroupType("localized-location");
        String ITEM_CONTENT_TYPE = buildItemType("localized-location");

        String LOCATION_UUID = "location_uuid";
        String PARENT_UUID = "parent_uuid";
        String NAME = "name";
        /** Patient count for a location, not including child locations. */
        String PATIENT_COUNT = "patient_count";
    }

    interface LocalizedChartColumns {
        String ENCOUNTER_TIME = "encounter_time";  // seconds since epoch
        String GROUP_NAME = "group_name";
        String CONCEPT_UUID = "concept_uuid";
        String CONCEPT_NAME = "concept_name";
        String CONCEPT_TYPE = "concept_type";
        String VALUE = "value";
        String LOCALIZED_VALUE = "localized_value";
    }

    // TODO: rename to LocalizedObservations
    public interface LocalizedCharts extends LocalizedChartColumns {
        Uri CONTENT_URI = buildContentUri("localized-charts");
        String GROUP_CONTENT_TYPE = buildGroupType("localized-chart");
        String ITEM_CONTENT_TYPE = buildItemType("localized-chart");

    }

    public interface MostRecentLocalizedCharts extends LocalizedChartColumns {
        Uri CONTENT_URI = buildContentUri("latest-localized-charts");
        String GROUP_CONTENT_TYPE = buildGroupType("localized-chart");
        String ITEM_CONTENT_TYPE = buildItemType("localized-chart");
    }

    public interface PatientCounts extends BaseColumns {
        Uri CONTENT_URI = buildContentUri("patient-counts");
        String GROUP_CONTENT_TYPE = buildGroupType("patient-count");
        String ITEM_CONTENT_TYPE = buildItemType("patient-count");

        String LOCATION_UUID = "location_uuid";
        // BaseColumns defines _COUNT.
        // TODO: just use a column named "count", not a mysterious "_count"
    }

    /** Returns the content URI for the localized locations for a given locale. */
    public static Uri getLocalizedLocationsUri(String locale) {
        return LocalizedLocations.CONTENT_URI.buildUpon()
                .appendPath(locale)
                .build();
    }

    /**
     * Returns the content URI for a localized chart for a given chart UUID, patient UUID, and
     * locale.
     */
    public static Uri getLocalizedChartUri(
            String chartUuid, String patientUuid, String locale) {
        return LocalizedCharts.CONTENT_URI.buildUpon()
                .appendPath(chartUuid)
                .appendPath(locale)
                .appendPath(patientUuid)
                .build();
    }

    /**
     * Returns the content URI for the most recent localized chart for a given patient UUID and
     * locale.
     */
    public static Uri getMostRecentChartUri(String patientUuid, String locale) {
        return MostRecentLocalizedCharts.CONTENT_URI.buildUpon()
                .appendPath(patientUuid)
                .appendPath(locale)
                .build();
    }
}
