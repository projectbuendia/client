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

import com.google.common.base.Joiner;

import org.joda.time.DateTime;
import org.projectbuendia.client.App;
import org.projectbuendia.client.json.Datatype;
import org.projectbuendia.client.models.Chart;
import org.projectbuendia.client.models.ChartItem;
import org.projectbuendia.client.models.ChartSection;
import org.projectbuendia.client.models.ConceptUuids;
import org.projectbuendia.client.models.Form;
import org.projectbuendia.client.models.Obs;
import org.projectbuendia.client.models.Order;
import org.projectbuendia.client.providers.Contracts;
import org.projectbuendia.client.providers.Contracts.ChartItems;
import org.projectbuendia.client.providers.Contracts.Observations;
import org.projectbuendia.client.providers.Contracts.Orders;
import org.projectbuendia.client.utils.Loc;
import org.projectbuendia.client.utils.Logger;
import org.projectbuendia.client.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.projectbuendia.client.utils.Utils.eq;

/** A helper class for retrieving and localizing data to show in patient charts. */
public class ChartDataHelper {
    private static final Logger LOG = Logger.create();
    private static final String[] UUIDS_TO_OMIT = {
        ConceptUuids.ORDER_EXECUTED_UUID,
        ConceptUuids.PLACEMENT_UUID
    };

    private final ContentResolver mContentResolver;

    public ChartDataHelper(ContentResolver contentResolver) {
        mContentResolver = checkNotNull(contentResolver);
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
                Utils.getString(c, Orders.UUID),
                patientUuid,
                Utils.getString(c, Orders.PROVIDER_UUID , ""),
                Utils.getString(c, Orders.INSTRUCTIONS, ""),
                Utils.getLong(c, Orders.START_MILLIS),
                Utils.getLong(c, Orders.STOP_MILLIS)));
        }
        c.close();
        return orders;
    }

    private static String formatInstant(long millis) {
        return Utils.formatUtc8601(new DateTime(millis)).replace('T', ' ');
    }
    private static Obs loadObs(Cursor c, Locale locale, ConceptService concepts) {
        String uuid = Utils.getString(c, Observations.UUID);
        String providerUuid = Utils.getString(c, Observations.PROVIDER_UUID);
        String encounterUuid = Utils.getString(c, Observations.ENCOUNTER_UUID);
        String patientUuid = Utils.getString(c, Observations.PATIENT_UUID);
        String conceptUuid = Utils.getString(c, Observations.CONCEPT_UUID, "");
        String orderUuid = Utils.getString(c, Observations.ORDER_UUID);
        DateTime time = Utils.getDateTime(c, Observations.MILLIS);
        Datatype type = Datatype.valueOf(Utils.getString(c, Observations.TYPE));
        String value = Utils.getString(c, Observations.VALUE, "");
        String valueName = type == Datatype.CODED ? concepts.getName(value, locale)
            : type == Datatype.DATETIME ? formatInstant(Long.valueOf(value))
            : value;
        return new Obs(uuid, encounterUuid, patientUuid, providerUuid,
            conceptUuid, type, time, orderUuid, value, valueName);
    }

    /** Gets all observations for a given patient in chronological order. */
    // TODO/cleanup: Consider returning a SortedSet<Obs> or a Map<String, SortedSet<ObsPoint>>.
    public List<Obs> getObservations(String patientUuid) {
        return getObservations(patientUuid, App.getSettings().getLocale());
    }

    /** Gets all observations for a given patient in chronological order. */
    private List<Obs> getObservations(String patientUuid, Locale locale) {
        ConceptService concepts = App.getConceptService();
        List<Obs> results = new ArrayList<>();
        try (Cursor c = mContentResolver.query(
            Observations.URI, null,
            Observations.PATIENT_UUID + " = ? and "
                + Observations.VOIDED + " IS NOT 1",
            new String[] {patientUuid},
            Observations.MILLIS
        )) {
            while (c.moveToNext()) {
                results.add(loadObs(c, locale, concepts));
            }
        }
        return results;
    }

    /** Gets localized observations, filtered by optional concept and time bounds. */
    public List<Obs> getPatientObservations(String patientUuid, String[] conceptUuids, Long startMillis, Long stopMillis) {
        ConceptService concepts = App.getConceptService();
        Locale locale = App.getSettings().getLocale();
        List<String> args = new ArrayList<>();

        String query = Observations.VOIDED + " IS NOT 1";

        query += " AND " + Observations.PATIENT_UUID + " = ?";
        args.add(patientUuid);

        if (Utils.hasItems(conceptUuids)) {
            query += " AND " + Observations.CONCEPT_UUID + " IN " + makeSqlPlaceholderSet(conceptUuids);
            args.addAll(Arrays.asList(conceptUuids));
        } else {
            query += " AND " + Observations.CONCEPT_UUID + " NOT IN " + makeSqlPlaceholderSet(UUIDS_TO_OMIT);
            args.addAll(Arrays.asList(UUIDS_TO_OMIT));
        }
        if (startMillis != null) {
            query += " AND " + Observations.MILLIS + " >= ?";
            args.add("" + startMillis);
        }
        if (stopMillis != null) {
            query += " AND " + Observations.MILLIS + " < ?";
            args.add("" + stopMillis);
        }
        String[] argArray = args.toArray(new String[0]);

        String order = Observations.MILLIS + " ASC";

        List<Obs> results = new ArrayList<>();
        try (Cursor c = mContentResolver.query(Observations.URI, null, query, argArray, order)) {
            while (c.moveToNext()) {
                results.add(loadObs(c, locale, concepts));
            }
        }
        return results;
    }

    private String makeSqlPlaceholderSet(String[] items) {
        String[] placeholders = new String[items.length];
        Arrays.fill(placeholders, "?");
        return "(" + Joiner.on(", ").join(placeholders) + ")";
    }

    /** Gets the latest observation of each concept for a given patient from the app db. */
    // TODO/cleanup: Have this return a Map<String, ObsPoint>.
    public Map<String, Obs> getLatestObservations(String patientUuid) {
        ConceptService concepts = App.getConceptService();
        Locale locale = App.getSettings().getLocale();
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
    public Map<String, Obs> getLatestObservationsForConcept(String conceptUuid) {
        ConceptService concepts = App.getConceptService();
        Locale locale = App.getSettings().getLocale();
        try (Cursor c = mContentResolver.query(
            Observations.URI, null,
            Observations.VOIDED + " IS NOT 1 and "
                + Observations.CONCEPT_UUID + " = ?",
            new String[] {conceptUuid},
            Observations.MILLIS + " DESC"
        )) {
            Map<String, Obs> result = new HashMap<>();
            while (c.moveToNext()) {
                String patientUuid = Utils.getString(c, Observations.PATIENT_UUID);
                if (result.containsKey(patientUuid)) continue;
                result.put(patientUuid, loadObs(c, locale, concepts));
            }
            return result;
        }
    }

    /** Retrieves all the chart definitions from the local datastore. */
    public List<Chart> getCharts(Locale locale) {
        Map<Long, ChartSection> tileGroupsById = new HashMap<>();
        Map<Long, ChartSection> rowGroupsById = new HashMap<>();
        List<Chart> charts = new ArrayList<>();
        Chart chart = null;

        try (Cursor c = mContentResolver.query(ChartItems.URI, null, null, null, "weight")) {
            while (c.moveToNext()) {
                Long rowid = Utils.getLong(c, ChartItems.ROWID);
                Long parentRowid = Utils.getLong(c, ChartItems.PARENT_ROWID);
                String label = new Loc(Utils.getString(c, ChartItems.LABEL, "")).get(locale);
                if (parentRowid == null) {
                    // Add a section.
                    String sectionType = Utils.getString(c, ChartItems.SECTION_TYPE);
                    if (eq(sectionType, "CHART_DIVIDER") && chart != null) {
                        // TODO(ping): Get rid of CHART_DIVIDER sections and
                        // CHART_DIVIDER items, and instead store multiple
                        // charts each in their own form.
                        if (chart.tileGroups.size() + chart.rowGroups.size() > 0) {
                            charts.add(chart);
                        }
                    } else if (eq(sectionType, "FIXED_ROW") && chart != null) {
                        ChartSection fixedGroup = new ChartSection(label);
                        chart.fixedGroups.add(fixedGroup);
                        tileGroupsById.put(rowid, fixedGroup);
                    } else if (eq(sectionType, "TILE_ROW") && chart != null) {
                        ChartSection tileGroup = new ChartSection(label);
                        chart.tileGroups.add(tileGroup);
                        tileGroupsById.put(rowid, tileGroup);
                    } else if (eq(sectionType, "GRID_SECTION") && chart != null) {
                        ChartSection rowGroup = new ChartSection(label);
                        chart.rowGroups.add(rowGroup);
                        rowGroupsById.put(rowid, rowGroup);
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
                            new Loc(Utils.getString(c, ChartItems.FORMAT)).get(locale),
                            new Loc(Utils.getString(c, ChartItems.CAPTION_FORMAT)).get(locale),
                            new Loc(Utils.getString(c, ChartItems.CSS_CLASS)).get(locale),
                            new Loc(Utils.getString(c, ChartItems.CSS_STYLE)).get(locale),
                            Utils.getString(c, ChartItems.SCRIPT));
                        section.items.add(item);
                    } else {
                        String type = Utils.getString(c, ChartItems.TYPE);
                        if ((type != null) && (type.equals("CHART_DIVIDER"))) {
                            chart = new Chart(label);
                        }
                    }
                }
            }
        }
        charts.add(chart);
        return charts;
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
