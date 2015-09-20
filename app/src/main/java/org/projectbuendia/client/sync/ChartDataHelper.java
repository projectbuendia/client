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

import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.util.Pair;

import com.google.common.collect.ImmutableSet;

import org.projectbuendia.client.models.Chart;
import org.projectbuendia.client.models.ChartItem;
import org.projectbuendia.client.models.ChartSection;
import org.projectbuendia.client.models.ChartSectionType;
import org.projectbuendia.client.models.Concepts;
import org.projectbuendia.client.models.Form;
import org.projectbuendia.client.json.ConceptType;
import org.projectbuendia.client.sync.providers.Contracts;
import org.projectbuendia.client.sync.providers.Contracts.ChartItems;
import org.projectbuendia.client.sync.providers.Contracts.ConceptNames;
import org.projectbuendia.client.sync.providers.Contracts.Observations;
import org.projectbuendia.client.sync.providers.Contracts.Orders;
import org.projectbuendia.client.utils.Logger;
import org.projectbuendia.client.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import static com.google.common.base.Preconditions.checkNotNull;

/** A helper class for retrieving and localizing data to show in patient charts. */
public class ChartDataHelper {
    @Deprecated
    public static final String CHART_GRID_UUID = "ea43f213-66fb-4af6-8a49-70fd6b9ce5d4";
    @Deprecated
    public static final String CHART_TILES_UUID = "975afbce-d4e3-4060-a25f-afcd0e5564ef";
    public static final String ENGLISH_LOCALE = "en";

    /** UUIDs for concepts that mean everything is normal; there is no worrying symptom. */
    public static final ImmutableSet<String> NO_SYMPTOM_VALUES = ImmutableSet.of(
        Concepts.NO_UUID, // NO
        Concepts.SOLID_FOOD_UUID, // Solid food
        Concepts.NORMAL_UUID, // NORMAL
        Concepts.NONE_UUID); // None

    private final ContentResolver mContentResolver;

    private static final Logger LOG = Logger.create();

    /** When non-null, sConceptNames and sConceptTypes contain valid data for this locale. */
    private static Object sLoadingLock = new Object();
    private static String sLoadedLocale;

    private static Map<String, String> sConceptNames;
    private static Map<String, String> sConceptTypes;

    public ChartDataHelper(ContentResolver contentResolver) {
        mContentResolver = checkNotNull(contentResolver);
    }

    /** Marks in-memory concept data out of date.  Call this when concepts change in the app db. */
    public static void invalidateLoadedConceptData() {
        sLoadedLocale = null;
    }

    /** Loads concept names and types from the app db into HashMaps in memory. */
    public void loadConceptData(String locale) {
        synchronized (sLoadingLock) {
            if (!locale.equals(sLoadedLocale)) {
                sConceptNames = new HashMap<>();
                try (Cursor c = mContentResolver.query(
                    ConceptNames.CONTENT_URI, new String[] {"concept_uuid", "name"},
                    "locale = ?", new String[] {locale}, null)) {
                    while (c.moveToNext()) {
                        sConceptNames.put(c.getString(0), c.getString(1));
                    }
                }
                sConceptTypes = new HashMap<>();
                try (Cursor c = mContentResolver.query(
                    Contracts.Concepts.CONTENT_URI, new String[] {"_id", "concept_type"},
                    null, null, null)) {
                    while (c.moveToNext()) {
                        sConceptTypes.put(c.getString(0), c.getString(1));
                    }
                }
                sLoadedLocale = locale;
            }
        }
    }

    /** Gets all the orders for a given patient. */
    public List<Order> getOrders(String patientUuid) {
        Cursor c = mContentResolver.query(
            Orders.CONTENT_URI,
            null, "patient_uuid = ?", new String[] {patientUuid}, "start_time");
        List<Order> orders = new ArrayList<>();
        while (c.moveToNext()) {
            orders.add(new Order(
                Utils.getString(c, "uuid", ""),
                Utils.getString(c, "instructions", ""),
                Utils.getLong(c, "start_time", null),
                Utils.getLong(c, "stop_time", null)));
        }
        c.close();
        return orders;
    }

    /** Gets all observations for a given patient from the local cache, localized to English. */
    public List<LocalizedObs> getObservations(String patientUuid) {
        return getObservations(patientUuid, ENGLISH_LOCALE);
    }

    private LocalizedObs obsFromCursor(Cursor c) {
        long id = c.getLong(c.getColumnIndex(BaseColumns._ID));
        long millis = c.getLong(c.getColumnIndex(Observations.ENCOUNTER_MILLIS));
        String conceptUuid = c.getString(c.getColumnIndex(Observations.CONCEPT_UUID));
        String conceptName = sConceptNames.get(conceptUuid);
        String conceptType = sConceptTypes.get(conceptUuid);
        String value = c.getString(c.getColumnIndex(Observations.VALUE));
        String localizedValue = value;
        if (ConceptType.CODED.name().equals(conceptType)) {
            localizedValue = sConceptNames.get(value);
        }
        return new LocalizedObs(id, millis, conceptUuid, conceptName, conceptType, value, localizedValue);
    }

    /** Gets all observations for a given patient, localized for a given locale. */
    public List<LocalizedObs> getObservations(String patientUuid, String locale) {
        loadConceptData(locale);
        List<LocalizedObs> results = new ArrayList<>();
        try (Cursor c = mContentResolver.query(
            Observations.CONTENT_URI, null,
            "patient_uuid = ?", new String[] {patientUuid}, null)) {
            while (c.moveToNext()) {
                results.add(obsFromCursor(c));
            }
        }
        return results;
    }

    /** Gets the latest observation of each concept for a given patient, localized to English. */
    public Map<String, LocalizedObs> getLatestObservations(String patientUuid) {
        return getLatestObservations(patientUuid, ENGLISH_LOCALE);
    }

    /** Gets the latest observation of each concept for a given patient from the app db. */
    public Map<String, LocalizedObs> getLatestObservations(String patientUuid, String locale) {
        Map<String, LocalizedObs> result = new HashMap<>();
        for (LocalizedObs obs : getObservations(patientUuid, locale)) {
            LocalizedObs existing = result.get(obs.conceptUuid);
            if (existing == null || obs.encounterTime.isAfter(existing.encounterTime)) {
                result.put(obs.conceptUuid, obs);
            }
        }
        return result;
    }

    /**
     * Gets the most recent observations for all concepts for a set of patients from the local
     * cache. Ordering will be by concept uuid, and there are not groups or other chart-based
     * configurations.
     */
    public Map<String, Map<String, LocalizedObs>>
    getLatestObservationsForPatients(String[] patientUuids, String locale) {
        Map<String, Map<String, LocalizedObs>> observations = new HashMap<String, Map<String, LocalizedObs>>();
        for (String patientUuid : patientUuids) {
            observations.put(patientUuid, getLatestObservations(patientUuid, locale));
        }
        return observations;
    }

    /** Gets the latest observation of the specified concept for all patients. */
    public Map<String, LocalizedObs> getLatestObservationsForConcept(
        String conceptUuid, String locale) {
        loadConceptData(locale);
        try (Cursor c = mContentResolver.query(
            Observations.CONTENT_URI, null,
            Observations.CONCEPT_UUID + " = ?", new String[] {conceptUuid},
            Observations.ENCOUNTER_MILLIS + " DESC")) {
            Map<String, LocalizedObs> result = new HashMap<>();
            while (c.moveToNext()) {
                String patientUuid = Utils.getString(c, Observations.PATIENT_UUID);
                if (result.containsKey(patientUuid)) continue;
                result.put(patientUuid, obsFromCursor(c));
            }
            return result;
        }
    }

    /** Retrieves and assembles a Chart from the local datastore. */
    public Chart getChart(String uuid) {
        Map<Long, ChartSection> tileGroupsById = new HashMap<>();
        Map<Long, ChartSection> rowGroupsById = new HashMap<>();
        List<ChartSection> tileGroups = new ArrayList<>();
        List<ChartSection> rowGroups = new ArrayList<>();

        try (Cursor c = mContentResolver.query(
            ChartItems.CONTENT_URI, null, "chart_uuid = ?", new String[] {uuid}, "weight")) {
            while (c.moveToNext()) {
                Long id = Utils.getLong(c, ChartItems._ID);
                Long parentId = Utils.getLong(c, ChartItems.PARENT_ID);
                String label = Utils.getString(c, ChartItems.LABEL, "");
                if (parentId == null) {
                    // Add a section.
                    switch (ChartSectionType.valueOf(Utils.getString(c, ChartItems.SECTION_TYPE))) {
                        case TILE_ROW:
                            ChartSection tileGroup = new ChartSection(label);
                            tileGroups.add(tileGroup);
                            tileGroupsById.put(id, tileGroup);
                            break;
                        case GRID_SECTION:
                            ChartSection rowGroup = new ChartSection(label);
                            rowGroups.add(rowGroup);
                            rowGroupsById.put(id, rowGroup);
                            break;
                    }
                } else {
                    // Add a tile to its tile group or a grid row to its row group.
                    ChartItem item = new ChartItem(label,
                        Utils.getString(c, ChartItems.TYPE),
                        Utils.getLong(c, ChartItems.REQUIRED, 0L) > 0L,
                        Utils.getString(c, ChartItems.CONCEPT_UUIDS, "").split(","),
                        Utils.getString(c, ChartItems.FORMAT),
                        Utils.getString(c, ChartItems.CAPTION_FORMAT),
                        Utils.getString(c, ChartItems.CSS_CLASS),
                        Utils.getString(c, ChartItems.CSS_STYLE),
                        Utils.getString(c, ChartItems.SCRIPT));
                    ChartSection section = tileGroupsById.containsKey(parentId)
                        ? tileGroupsById.get(parentId) : rowGroupsById.get(parentId);
                    section.items.add(item);
                }
            }
        }
        return new Chart(uuid, tileGroups, rowGroups);
    }

    /** Gets a list of the concept UUIDs and names to show in the chart tiles. */
    @Deprecated
    public List<Pair<String, String>> getTileConcepts() {
        Map<String, String> conceptNames = new HashMap<>();
        Cursor cursor = mContentResolver.query(Contracts.ConceptNames.CONTENT_URI, null,
            "locale = ?", new String[] {ENGLISH_LOCALE}, null);
        try {
            while (cursor.moveToNext()) {
                conceptNames.put(Utils.getString(cursor, "concept_uuid"),
                    Utils.getString(cursor, "name"));
            }
        } finally {
            cursor.close();
        }
        List<Pair<String, String>> conceptUuidsAndNames = new ArrayList<>();
        cursor = mContentResolver.query(Contracts.ChartItems.CONTENT_URI, null,
            "chart_uuid = ?", new String[] {CHART_TILES_UUID}, "chart_row");
        try {
            while (cursor.moveToNext()) {
                String uuid = Utils.getString(cursor, "concept_uuid");
                conceptUuidsAndNames.add(new Pair<>(uuid, conceptNames.get(uuid)));
            }
        } finally {
            cursor.close();
        }
        return conceptUuidsAndNames;
    }

    /** Gets a list of the concept UUIDs and names to show in the rows of the chart grid. */
    @Deprecated
    public List<Pair<String, String>> getGridRowConcepts() {
        Map<String, String> conceptNames = new HashMap<>();
        Cursor cursor = mContentResolver.query(Contracts.ConceptNames.CONTENT_URI, null,
            "locale = ?", new String[] {ENGLISH_LOCALE}, null);
        try {
            while (cursor.moveToNext()) {
                conceptNames.put(Utils.getString(cursor, "concept_uuid"),
                    Utils.getString(cursor, "name"));
            }
        } finally {
            cursor.close();
        }
        List<Pair<String, String>> conceptUuidsAndNames = new ArrayList<>();
        cursor = mContentResolver.query(Contracts.ChartItems.CONTENT_URI, null,
            "chart_uuid = ?", new String[] {CHART_GRID_UUID}, "chart_row");
        try {
            while (cursor.moveToNext()) {
                String uuid = Utils.getString(cursor, "concept_uuid");
                conceptUuidsAndNames.add(new Pair<>(uuid, conceptNames.get(uuid)));
            }
        } finally {
            cursor.close();
        }
        return conceptUuidsAndNames;
    }

    public List<Form> getForms() {
        Cursor cursor = mContentResolver.query(
            Contracts.Forms.CONTENT_URI, null, null, null, null);
        SortedSet<Form> forms = new TreeSet<>();
        try {
            while (cursor.moveToNext()) {
                forms.add(new Form(
                    Utils.getString(cursor, Contracts.Forms._ID),
                    Utils.getString(cursor, Contracts.Forms.UUID),
                    Utils.getString(cursor, Contracts.Forms.NAME),
                    Utils.getString(cursor, Contracts.Forms.VERSION)));
            }
        } finally {
            cursor.close();
        }
        List<Form> sortedForms = new ArrayList<>();
        sortedForms.addAll(forms);
        return sortedForms;
    }
}
