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

import org.projectbuendia.client.json.ConceptType;
import org.projectbuendia.client.models.Chart;
import org.projectbuendia.client.models.ChartItem;
import org.projectbuendia.client.models.ChartSection;
import org.projectbuendia.client.models.ConceptUuids;
import org.projectbuendia.client.models.Form;
import org.projectbuendia.client.models.Obs;
import org.projectbuendia.client.models.ObsRow;
import org.projectbuendia.client.models.Order;
import org.projectbuendia.client.providers.Contracts;
import org.projectbuendia.client.providers.Contracts.ChartItems;
import org.projectbuendia.client.providers.Contracts.ConceptNames;
import org.projectbuendia.client.providers.Contracts.Concepts;
import org.projectbuendia.client.providers.Contracts.Observations;
import org.projectbuendia.client.providers.Contracts.Orders;
import org.projectbuendia.client.utils.Logger;
import org.projectbuendia.client.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/** A helper class for retrieving and localizing data to show in patient charts. */
public class ChartDataHelper {
    @Deprecated
    public static final String CHART_GRID_UUID = "ea43f213-66fb-4af6-8a49-70fd6b9ce5d4";
    @Deprecated
    public static final String CHART_TILES_UUID = "975afbce-d4e3-4060-a25f-afcd0e5564ef";
    public static final String ENGLISH_LOCALE = "en";

    private final ContentResolver mContentResolver;

    private static final Logger LOG = Logger.create();

    /** When non-null, sConceptNames and sConceptTypes contain valid data for this locale. */
    private static final Object sLoadingLock = new Object();
    private static String sLoadedLocale;

    private static Map<String, String> sConceptNames;
    private static Map<String, ConceptType> sConceptTypes;

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
                    ConceptNames.URI, new String[] {ConceptNames.CONCEPT_UUID, ConceptNames.NAME},
                    ConceptNames.LOCALE + " = ?", new String[] {locale}, null)) {
                    while (c.moveToNext()) {
                        sConceptNames.put(c.getString(0), c.getString(1));
                    }
                }
                sConceptTypes = new HashMap<>();
                try (Cursor c = mContentResolver.query(
                    Concepts.URI, new String[] {Concepts.UUID, Concepts.CONCEPT_TYPE},
                    null, null, null)) {
                    while (c.moveToNext()) {
                        try {
                            sConceptTypes.put(c.getString(0), ConceptType.valueOf(c.getString(1)));
                        } catch (IllegalArgumentException e) { /* bad concept type name */ }
                    }
                }
                // Special case: we know this is a date even if it's not in any forms or charts.
                sConceptTypes.put(ConceptUuids.ADMISSION_DATE_UUID, ConceptType.DATE);
                sLoadedLocale = locale;
            }
        }
    }

    /** Gets all the orders for a given patient. */
    public List<Order> getOrders(String patientUuid) {
        Cursor c = mContentResolver.query(
            Orders.URI, null,
            Orders.PATIENT_UUID + " = ?", new String[] {patientUuid},
            Orders.START_MILLIS);
        List<Order> orders = new ArrayList<>();
        while (c.moveToNext()) {
            orders.add(new Order(
                Utils.getString(c, Orders.UUID, ""),
                patientUuid,
                Utils.getString(c, Orders.INSTRUCTIONS, ""),
                Utils.getLong(c, Orders.START_MILLIS, null),
                Utils.getLong(c, Orders.STOP_MILLIS, null)));
        }
        c.close();
        return orders;
    }

    /** Gets all observations for a given patient from the local cache, localized to English. */
    // TODO/cleanup: Consider returning a SortedSet<Obs> or a Map<String, SortedSet<ObsPoint>>.
    public List<Obs> getObservations(String patientUuid) {
        return getObservations(patientUuid, ENGLISH_LOCALE);
    }

    private Obs obsFromCursor(Cursor c) {
        long millis = c.getLong(c.getColumnIndex(Observations.ENCOUNTER_MILLIS));
        String conceptUuid = c.getString(c.getColumnIndex(Observations.CONCEPT_UUID));
        ConceptType conceptType = sConceptTypes.get(conceptUuid);
        String value = c.getString(c.getColumnIndex(Observations.VALUE));
        String localizedValue = value;
        if (ConceptType.CODED.equals(conceptType)) {
            localizedValue = sConceptNames.get(value);
        }
        return new Obs(millis, conceptUuid, conceptType, value, localizedValue);
    }

    private @Nullable ObsRow obsrowFromCursor(Cursor c) {
        String uuid = c.getString(c.getColumnIndex(Observations.UUID));
        long millis = c.getLong(c.getColumnIndex(Observations.ENCOUNTER_MILLIS));
        String conceptUuid = c.getString(c.getColumnIndex(Observations.CONCEPT_UUID));
        ConceptType conceptType = sConceptTypes.get(conceptUuid);
        String value = c.getString(c.getColumnIndex(Observations.VALUE));
        String localizedValue = value;
        if (ConceptType.CODED.equals(conceptType)) {
            localizedValue = sConceptNames.get(value);
        }
        String conceptName = sConceptNames.get(conceptUuid);
        if (conceptName == null) {
            return null;
        }
        else {
            return new ObsRow(uuid, millis, conceptName, conceptUuid, value, localizedValue);
        }
    }


    /** Gets all observations for a given patient, localized for a given locale. */
    // TODO/cleanup: Consider returning a SortedSet<Obs> or a Map<String, SortedSet<ObsPoint>>.
    public List<Obs> getObservations(String patientUuid, String locale) {
        loadConceptData(locale);
        List<Obs> results = new ArrayList<>();
        try (Cursor c = mContentResolver.query(
            Observations.URI, null,
            Observations.PATIENT_UUID + " = ? and "
                    + Observations.VOIDED + " IS NOT ?",
            new String[] {patientUuid,"1"},null)) {
            while (c.moveToNext()) {
                results.add(obsFromCursor(c));
            }
        }
        return results;
    }

    public ArrayList<ObsRow> getPatientObservationsByConcept(String patientUuid, String... conceptUuids) {
        loadConceptData(ENGLISH_LOCALE);

        String[] args = new String[conceptUuids.length + 1];
        String conceptSet = "";
        int i = 0;
        while (i < conceptUuids.length) {
            if (i > 0) conceptSet += ", ";
            conceptSet += "?";
            args[i] = conceptUuids[i];
            i++;
        }
        args[i++] = patientUuid;

        ArrayList<ObsRow> results = new ArrayList<>();
        try (Cursor c = mContentResolver.query(
            Observations.URI,
            null,
            Observations.CONCEPT_UUID + " in (" + conceptSet + ") and "
                + Observations.PATIENT_UUID + " = ? and "
                + Observations.VOIDED + " IS NOT 1",
            args,
            Observations.ENCOUNTER_MILLIS + " ASC"
        )) {
            while (c.moveToNext()) {
                ObsRow row = obsrowFromCursor(c);
                if (row !=null) results.add(row);
            }
        }
        return results;
    }

    public ArrayList<ObsRow> getPatientObservationsByMillis(String patientUuid, String startMillis,String stopMillis) {
        loadConceptData(ENGLISH_LOCALE);
        ArrayList<ObsRow> results = new ArrayList<>();
        String conditions = Observations.VOIDED + " IS NOT ? and "
                + Observations.PATIENT_UUID + " = ? and "
                + Observations.ENCOUNTER_MILLIS + " >= ? and "
                + Observations.ENCOUNTER_MILLIS + " <= ?";

        String[] values = new String[]{"1",patientUuid, startMillis,stopMillis};
        String order = Observations.ENCOUNTER_MILLIS + " ASC";

        try(Cursor c = mContentResolver.query(Observations.URI,null,conditions,values, order))
        {
            while (c.moveToNext()) {
                ObsRow row = obsrowFromCursor(c);
                if (row !=null){results.add(row);}
            }
        }
        return results;
    }

    public ArrayList<ObsRow> getPatientObservationsByConceptMillis(String patientUuid, String conceptUuid, String StartMillis, String StopMillis) {
        loadConceptData(ENGLISH_LOCALE);
        ArrayList<ObsRow> results = new ArrayList<>();
        String conditions = Observations.VOIDED + " IS NOT ? and "
                + Observations.PATIENT_UUID + " = ? and "
                + Observations.CONCEPT_UUID + " = ? and "
                + Observations.ENCOUNTER_MILLIS + " >= ? and "
                + Observations.ENCOUNTER_MILLIS + " <= ?";

        String[] values = new String[]{"1",patientUuid, conceptUuid, StartMillis,StopMillis};
        String order = Observations.ENCOUNTER_MILLIS + " ASC";

        try(Cursor c = mContentResolver.query(Observations.URI,null,conditions,values, order))
        {
            while (c.moveToNext()) {
                ObsRow row = obsrowFromCursor(c);
                if (row !=null){results.add(row);}
            }
        }
        return results;
    }

    /** Gets the latest observation of each concept for a given patient, localized to English. */
    // TODO/cleanup: Have this return a Map<String, ObsPoint>.
    public Map<String, Obs> getLatestObservations(String patientUuid) {
        // TODO: i18n
        return getLatestObservations(patientUuid, ENGLISH_LOCALE);
    }

    /** Gets the latest observation of each concept for a given patient from the app db. */
    // TODO/cleanup: Have this return a Map<String, ObsPoint>.
    public Map<String, Obs> getLatestObservations(String patientUuid, String locale) {
        Map<String, Obs> result = new HashMap<>();
        for (Obs obs : getObservations(patientUuid, locale)) {
            Obs existing = result.get(obs.conceptUuid);
            if (existing == null || obs.time.isAfter(existing.time)) {
                result.put(obs.conceptUuid, obs);
            }
        }
        return result;
    }

    /** Gets the latest observation of the specified concept for all patients. */
    // TODO/cleanup: Have this return a Map<String, ObsPoint>.
    public Map<String, Obs> getLatestObservationsForConcept(
        String conceptUuid, String locale) {
        loadConceptData(locale);
        try (Cursor c = mContentResolver.query(
            Observations.URI, null,
                Observations.VOIDED + " IS NOT ? and "
                    + Observations.CONCEPT_UUID + " = ?",
                new String[] {"1",conceptUuid},
            Observations.ENCOUNTER_MILLIS + " DESC")) {
            Map<String, Obs> result = new HashMap<>();
            while (c.moveToNext()) {
                String patientUuid = Utils.getString(c, Observations.PATIENT_UUID);
                if (result.containsKey(patientUuid)) continue;
                result.put(patientUuid, obsFromCursor(c));
            }
            return result;
        }
    }

    /** Retrieves and assembles a Chart from the local datastore. */
    public List<Chart> getCharts(String uuid) {
        Map<Long, ChartSection> tileGroupsById = new HashMap<>();
        Map<Long, ChartSection> rowGroupsById = new HashMap<>();
        List<Chart> Charts = new ArrayList<>();
        Chart currentChart = null;

        try (Cursor c = mContentResolver.query(
            ChartItems.URI, null,
            ChartItems.CHART_UUID + " = ?", new String[] {uuid}, "weight")) {
            while (c.moveToNext()) {
                Long rowid = Utils.getLong(c, ChartItems.ROWID);
                Long parentRowid = Utils.getLong(c, ChartItems.PARENT_ROWID);
                String label = Utils.getString(c, ChartItems.LABEL, "");
                if (parentRowid == null) {
                    // Add a section.
                    String SectionType = Utils.getString(c, ChartItems.SECTION_TYPE);
                    if (SectionType != null) {
                        switch (SectionType) {
                            case "CHART_DIVIDER":
                                if ((currentChart != null) &&
                                    ((currentChart.tileGroups.size() != 0)
                                    || (currentChart.rowGroups.size() != 0))) {
                                    Charts.add(currentChart);
                                }
                                break;
                            case "TILE_ROW":
                                ChartSection tileGroup = new ChartSection(label);
                                currentChart.tileGroups.add(tileGroup);
                                tileGroupsById.put(rowid, tileGroup);
                                break;
                            case "GRID_SECTION":
                                ChartSection rowGroup = new ChartSection(label);
                                currentChart.rowGroups.add(rowGroup);
                                rowGroupsById.put(rowid, rowGroup);
                                break;
                        }
                    }
                } else {
                    // Add a tile to its tile group or a grid row to its row group.
                    ChartSection section = tileGroupsById.containsKey(parentRowid)
                        ? tileGroupsById.get(parentRowid) : rowGroupsById.get(parentRowid);
                    if (section != null) {
                        ChartItem item = new ChartItem(label,
                            Utils.getString(c, ChartItems.TYPE),
                            Utils.getLong(c, ChartItems.REQUIRED, 0L) > 0L,
                            Utils.getString(c, ChartItems.CONCEPT_UUIDS, "").split(","),
                            Utils.getString(c, ChartItems.FORMAT),
                            Utils.getString(c, ChartItems.CAPTION_FORMAT),
                            Utils.getString(c, ChartItems.CSS_CLASS),
                            Utils.getString(c, ChartItems.CSS_STYLE),
                            Utils.getString(c, ChartItems.SCRIPT));
                        section.items.add(item);
                    } else {
                        String type = Utils.getString(c, ChartItems.TYPE);
                        if ((type != null) && (type.equals("CHART_DIVIDER"))) {
                            currentChart = new Chart(uuid, label);
                        }
                    }
                }
            }
        }
        Charts.add(currentChart);
        return Charts;
    }

    public List<Form> getForms() {
        SortedSet<Form> forms = new TreeSet<>();
        try (Cursor cursor = mContentResolver.query(
            Contracts.Forms.URI, null, null, null, null)) {
            while (cursor.moveToNext()) {
                forms.add(new Form(
                    Utils.getString(cursor, Contracts.Forms.UUID),
                    Utils.getString(cursor, Contracts.Forms.NAME),
                    Utils.getString(cursor, Contracts.Forms.VERSION)));
            }
        }
        List<Form> sortedForms = new ArrayList<>();
        sortedForms.addAll(forms);
        return sortedForms;
    }
}
