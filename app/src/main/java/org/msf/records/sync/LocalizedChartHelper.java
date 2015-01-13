package org.msf.records.sync;

import static com.google.common.base.Preconditions.checkNotNull;

import android.content.ContentResolver;
import android.database.Cursor;

import org.msf.records.net.model.Concept;
import org.msf.records.sync.providers.Contracts;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * A simple helper method to get all observations for a patient in a nice java bean format.
 */
public class LocalizedChartHelper {

    public static final String KNOWN_CHART_UUID = "ea43f213-66fb-4af6-8a49-70fd6b9ce5d4";
    public static final String ENGLISH_LOCALE = "en";

    /**
     * A uuid representing when a clinician fills in "Unknown".
     */
    public static final String UNKNOWN_VALUE = "1067AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    /**
     * A set of uuids for concepts that represent an answer indicating everything is normal, and
     * there is no worrying symptom.
     */
    public static final ImmutableSet<String> NO_SYMPTOM_VALUES = ImmutableSet.of(
        "1066AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", // NO
        "159597AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", // Solid food
        "95d50bc3-6281-4661-94ab-1a26455c40a2", // Normal pulse
        "160282AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", // Awake
        "1115AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", // NORMAL
        "db2ac5ad-cc64-4184-b4be-1324730e1882", // Can talk
        "c2a547f7-6329-4273-80c2-eae804897efd", // Can walk
        Concept.NONE_UUID); // None

    /**
     * A simple bean class representing an observation. All names and values have been localized.
     */
    public static final class LocalizedObservation {
        /**
         * The time of the encounter (hence the observation) in milliseconds since epoch.
         */
        public final long encounterTimeMillis;
        /**
         * The localized name to the group/section the observation should be displayed in.
         */
        public final String groupName;
        /**
         * The UUID of the concept, unique and guaranteed to be stable, so suitable as a map key.
         */
        public final String conceptUuid;
        /**
         * The localized name of the concept that was observed.
         */
        public final String conceptName;
        /**
         * The value that was observed non-localized. For a numeric value it will be a number,
         * for a non-numeric value it will be a UUID of the response.
         */
        // TODO(rjlothian): It's not clear in what situations this value can be null.
        @Nullable public final String value;

        /**
         * The value that was observed, converted to a String, and localized in the case of
         * Coded (concept) observations.
         */
        // TODO(rjlothian): It's not clear in what situations this value can be null.
        @Nullable public final String localizedValue;

        public LocalizedObservation(
                long encounterTimeMillis,
                String groupName,
                String conceptUuid,
                String conceptName,
                @Nullable String value,
                @Nullable String localizedValue) {
            this.encounterTimeMillis = encounterTimeMillis;
            this.groupName = checkNotNull(groupName);
            this.conceptUuid = checkNotNull(conceptUuid);
            this.conceptName = checkNotNull(conceptName);
            this.value = value;
            this.localizedValue = localizedValue;
        }

        @Override
        public String toString() {
            return "time=" + encounterTimeMillis
                    + ",group=" + groupName
                    + ",conceptUuid=" + conceptUuid
                    + ",conceptName=" + conceptName
                    + ",value=" + localizedValue;
        }
    }

    private final ContentResolver mContentResolver;

    public LocalizedChartHelper(
            ContentResolver contentResolver) {
        mContentResolver = checkNotNull(contentResolver);
    }
    /**
     * Get all observations for a given patient from the local cache, localized to English.
     */
    public List<LocalizedObservation> getObservations(
            String patientUuid) {
        return getObservations(patientUuid, ENGLISH_LOCALE);
    }

    /**
     * Get all observations for a given patient.
     * @param locale the locale to return the results in, to match the server String
     */
    public List<LocalizedObservation> getObservations(
            String patientUuid,
            String locale) {
        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(
                    Contracts.LocalizedCharts.getLocalizedChartUri(
                            KNOWN_CHART_UUID, patientUuid, locale),
                    null, null, null, null);

            List<LocalizedObservation> result = new ArrayList<>();
            while (cursor.moveToNext()) {
                LocalizedObservation obs = new LocalizedObservation(
                        cursor.getInt(cursor.getColumnIndex("encounter_time")) * 1000L,
                        cursor.getString(cursor.getColumnIndex("group_name")),
                        cursor.getString(cursor.getColumnIndex("concept_uuid")),
                        cursor.getString(cursor.getColumnIndex("concept_name")),
                        cursor.getString(cursor.getColumnIndex("value")),
                        cursor.getString(cursor.getColumnIndex("localized_value"))
                );
                result.add(obs);
            }
            return result;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Get the most recent observations for each concept for a given patient from the local cache,
     * localized to English. Ordering will be by concept uuid, and there are not groups or other
     * chart based configurations.
     */
    public Map<String, LocalizedObservation> getMostRecentObservations(
            String patientUuid) {
        return getMostRecentObservations(patientUuid, ENGLISH_LOCALE);
    }

    /**
     * Get the most recent observations for each concept for a given patient from the local cache,
     * Ordering will be by concept uuid, and there are not groups or other chart-based
     * configurations.
     * @param locale the locale to return the results in, to match the server String
     */
    public Map<String, LocalizedChartHelper.LocalizedObservation> getMostRecentObservations(
            String patientUuid,
            String locale) {
        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(
                    Contracts.MostRecentLocalizedCharts.getMostRecentChartUri(patientUuid, locale),
                    null,
                    null,
                    null,
                    null);

            Map<String, LocalizedChartHelper.LocalizedObservation> result = Maps.newLinkedHashMap();
            while (cursor.moveToNext()) {
                String concept_uuid = cursor.getString(cursor.getColumnIndex("concept_uuid"));

                LocalizedObservation obs = new LocalizedObservation(
                        cursor.getInt(cursor.getColumnIndex("encounter_time")) * 1000L,
                        "", /* no group */
                        concept_uuid,
                        cursor.getString(cursor.getColumnIndex("concept_name")),
                        cursor.getString(cursor.getColumnIndex("value")),
                        cursor.getString(cursor.getColumnIndex("localized_value"))
                );
                result.put(concept_uuid, obs);
            }
            return result;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Get the most recent observations for all concepts for a set of patients from the local
     * cache. Ordering will be by concept uuid, and there are not groups or other chart-based
     * configurations.
     * @param patientUuids the uuids of patients to return data for
     * @param locale the locale to return the results in, to match the server String
     */
    public Map<String, Map<String, LocalizedChartHelper.LocalizedObservation>> getMostRecentObservationsBatch(
            String[] patientUuids,
            String locale) {
        Map<String, Map<String, LocalizedChartHelper.LocalizedObservation>> observations =
                new HashMap<String, Map<String, LocalizedObservation>>();
        for (String patientUuid : patientUuids) {
            observations.put(patientUuid, getMostRecentObservations(patientUuid, locale));
        }

        return observations;
    }


    /**
     * Get all observations for a given patient.
     * @param locale the locale to return the results in, to match the server String
     */
    public List<LocalizedObservation> getEmptyChart(
            String locale) {
        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(
                    Contracts.LocalizedCharts.getEmptyLocalizedChartUri(KNOWN_CHART_UUID, locale),
                    null, null, null, null);

            List<LocalizedObservation> result = new ArrayList<>();
            while (cursor.moveToNext()) {
                LocalizedObservation obs = new LocalizedObservation(
                        0L,
                        cursor.getString(cursor.getColumnIndex("group_name")),
                        cursor.getString(cursor.getColumnIndex("concept_uuid")),
                        cursor.getString(cursor.getColumnIndex("concept_name")),
                        "", // no value
                        "" // no value
                );
                result.add(obs);
            }
            return result;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
