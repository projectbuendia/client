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

package org.projectbuendia.client.sync;

import static com.google.common.base.Preconditions.checkNotNull;

import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.BaseColumns;

import org.projectbuendia.client.model.Concepts;
import org.projectbuendia.client.sync.providers.Contracts;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** A simple helper class for retrieving and localizing data from patient charts. */
public class LocalizedChartHelper {

    public static final String KNOWN_CHART_UUID = "ea43f213-66fb-4af6-8a49-70fd6b9ce5d4";
    public static final String ENGLISH_LOCALE = "en";

    /** UUIDs for concepts that mean everything is normal; there is no worrying symptom. */
    public static final ImmutableSet<String> NO_SYMPTOM_VALUES = ImmutableSet.of(
            Concepts.NO_UUID, // NO
            Concepts.SOLID_FOOD_UUID, // Solid food
            Concepts.NORMAL_UUID, // NORMAL
            Concepts.NONE_UUID); // None

    private final ContentResolver mContentResolver;

    public LocalizedChartHelper(ContentResolver contentResolver) {
        mContentResolver = checkNotNull(contentResolver);
    }

    public List<Order> getOrders(String patientUuid) {
        Cursor c = mContentResolver.query(
                Contracts.Orders.CONTENT_URI,
                null, "patient_uuid = ?", new String[] {patientUuid}, "start_time");
        List<Order> orders = new ArrayList<>();
        while (c.moveToNext()) {
            orders.add(new Order(
                    c.getString(c.getColumnIndex("uuid")),
                    c.getString(c.getColumnIndex("instructions")),
                    c.getLong(c.getColumnIndex("start_time")),
                    c.getLong(c.getColumnIndex("stop_time"))));
        }
        return orders;
    }

    /** Gets all observations for a given patient from the local cache, localized to English. */
    public List<LocalizedObs> getObservations(String patientUuid) {
        return getObservations(patientUuid, ENGLISH_LOCALE);
    }

    /** Gets all observations for a given patient, localized for a given locale. */
    public List<LocalizedObs> getObservations(String patientUuid, String locale) {
        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(
                    Contracts.LocalizedCharts.getLocalizedChartUri(
                            KNOWN_CHART_UUID, patientUuid, locale),
                    null, null, null, null);

            List<LocalizedObs> result = new ArrayList<>();
            while (cursor.moveToNext()) {
                LocalizedObs obs = new LocalizedObs(
                        cursor.getLong(cursor.getColumnIndex(BaseColumns._ID)),
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
     * Gets the most recent observations for each concept for a given patient from the local cache,
     * localized to English. Ordering will be by concept uuid, and there are not groups or other
     * chart based configurations.
     */
    public Map<String, LocalizedObs> getMostRecentObservations(String patientUuid) {
        return getMostRecentObservations(patientUuid, ENGLISH_LOCALE);
    }

    /**
     * Gets the most recent observations for each concept for a given patient from the local cache,
     * Ordering will be by concept uuid, and there are not groups or other chart-based
     * configurations.
     */
    public Map<String, LocalizedObs> getMostRecentObservations(String patientUuid, String locale) {
        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(
                    Contracts.MostRecentLocalizedCharts.getMostRecentChartUri(patientUuid, locale),
                    null, null, null, null);

            Map<String, LocalizedObs> result = Maps.newLinkedHashMap();
            while (cursor.moveToNext()) {
                String conceptUuid = cursor.getString(cursor.getColumnIndex("concept_uuid"));

                LocalizedObs obs = new LocalizedObs(
                        cursor.getLong(cursor.getColumnIndex(BaseColumns._ID)),
                        cursor.getInt(cursor.getColumnIndex("encounter_time")) * 1000L,
                        "", /* no group */
                        conceptUuid,
                        cursor.getString(cursor.getColumnIndex("concept_name")),
                        cursor.getString(cursor.getColumnIndex("value")),
                        cursor.getString(cursor.getColumnIndex("localized_value"))
                );
                result.put(conceptUuid, obs);
            }
            return result;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Gets the most recent observations for all concepts for a set of patients from the local
     * cache. Ordering will be by concept uuid, and there are not groups or other chart-based
     * configurations.
     */
    public Map<String, Map<String, LocalizedObs>>
            getMostRecentObservationsBatch(String[] patientUuids, String locale) {
        Map<String, Map<String, LocalizedObs>> observations = new HashMap<String, Map<String, LocalizedObs>>();
        for (String patientUuid : patientUuids) {
            observations.put(patientUuid, getMostRecentObservations(patientUuid, locale));
        }
        return observations;
    }

    /** Gets observations for an empty chart.  TODO/cleanup: Not sure this is needed? */
    public List<LocalizedObs> getEmptyChart(String locale) {
        Cursor cursor = null;
        try {
            cursor = mContentResolver.query(
                    Contracts.LocalizedCharts.getEmptyLocalizedChartUri(KNOWN_CHART_UUID, locale),
                    null, null, null, null);

            List<LocalizedObs> result = new ArrayList<>();
            while (cursor.moveToNext()) {
                LocalizedObs obs = new LocalizedObs(
                        cursor.getLong(cursor.getColumnIndex(BaseColumns._ID)),
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
