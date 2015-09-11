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

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.SyncResult;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;

import com.android.volley.toolbox.RequestFuture;

import org.joda.time.DateTime;
import org.projectbuendia.client.App;
import org.projectbuendia.client.data.app.AppForm;
import org.projectbuendia.client.data.app.AppModel;
import org.projectbuendia.client.data.app.AppPatient;
import org.projectbuendia.client.net.json.JsonChart;
import org.projectbuendia.client.net.json.JsonChartSection;
import org.projectbuendia.client.net.json.JsonEncounter;
import org.projectbuendia.client.net.json.JsonForm;
import org.projectbuendia.client.net.json.JsonLocation;
import org.projectbuendia.client.net.json.JsonOrder;
import org.projectbuendia.client.net.json.JsonPatient;
import org.projectbuendia.client.net.json.JsonPatientRecord;
import org.projectbuendia.client.net.json.JsonUser;
import org.projectbuendia.client.sync.providers.Contracts;
import org.projectbuendia.client.sync.providers.Contracts.Charts;
import org.projectbuendia.client.sync.providers.Contracts.LocationNames;
import org.projectbuendia.client.sync.providers.Contracts.Locations;
import org.projectbuendia.client.sync.providers.Contracts.Observations;
import org.projectbuendia.client.sync.providers.Contracts.Orders;
import org.projectbuendia.client.sync.providers.Contracts.Patients;
import org.projectbuendia.client.sync.providers.Contracts.Users;
import org.projectbuendia.client.utils.Logger;
import org.projectbuendia.client.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * A helper class for turning the Java beans that are the result of chart RPC calls into
 * appropriate {@link ContentProviderOperation}s for inserting into the DB.
 */
public class DbSyncHelper {
    private static final Logger LOG = Logger.create();

    /** Converts a JsonChart response into appropriate inserts in the chart table. */
    public static ArrayList<ContentProviderOperation> getChartUpdateOps(
            JsonChart response, SyncResult syncResult) {
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        if (response.uuid == null) {
            LOG.e("null chart uuid when fetching chart structure");
        }
        int chartRow = 0;
        for (JsonChartSection section : response.groups) {
            if (section.uuid == null) {
                LOG.e("null group uuid for chart " + response.uuid);
                continue;
            }
            for (String conceptUuid : section.concepts) {
                ops.add(ContentProviderOperation
                        .newInsert(Charts.CONTENT_URI)
                        .withValue(Charts.CHART_UUID, response.uuid)
                        .withValue(Charts.CHART_ROW, chartRow++)
                        .withValue(Charts.GROUP_UUID, section.uuid)
                        .withValue(Charts.CONCEPT_UUID, conceptUuid)
                        .build());
                syncResult.stats.numInserts++;
            }
        }
        return ops;
    }

    public static List<ContentProviderOperation> getFormUpdateOps(SyncResult syncResult)
        throws ExecutionException, InterruptedException {
        final ContentResolver resolver = App.getInstance().getContentResolver();

        LOG.i("Listing all forms on server");
        RequestFuture<List<JsonForm>> future = RequestFuture.newFuture();
        App.getServer().listForms(future, future);
        Map<String, ContentValues> cvs = new HashMap<>();
        for (JsonForm form : future.get()) {
            cvs.put(form.id, AppForm.fromNet(form).toContentValues());
        }

        Cursor c = resolver.query(Contracts.Forms.CONTENT_URI, null, null, null, null);
        LOG.i("Examining forms: " + c.getCount() + " local, " + cvs.size() + " from server");

        List<ContentProviderOperation> ops = new ArrayList<>();
        try {
            while (c.moveToNext()) {
                String localId = Utils.getString(c, Contracts.Forms._ID);
                Uri uri = Contracts.Forms.CONTENT_URI.buildUpon().appendPath(localId).build();
                LOG.i("  - will delete form " + localId);
                ops.add(ContentProviderOperation.newDelete(uri).build());
            }
        } finally {
            c.close();
        }

        for (ContentValues values : cvs.values()) {  // server has a new record
            LOG.i("  - will insert form " + values.getAsString(Contracts.Forms._ID));
            ops.add(ContentProviderOperation.newInsert(Contracts.Forms.CONTENT_URI).withValues(values).build());
            syncResult.stats.numInserts++;
        }
        return ops;
    }

    /**
     * Downloads all patients from the server and produces a list of the operations
     * needed to bring the local database in sync with the server.
     */
    public static List<ContentProviderOperation> getPatientUpdateOps(SyncResult syncResult)
            throws ExecutionException, InterruptedException {
        final ContentResolver resolver = App.getInstance().getContentResolver();

        RequestFuture<List<JsonPatient>> future = RequestFuture.newFuture();
        App.getServer().listPatients("", "", "", future, future);
        Map<String, ContentValues> cvs = new HashMap<>();
        for (JsonPatient patient : future.get()) {
            cvs.put(patient.id, AppPatient.fromNet(patient).toContentValues());
        }

        Cursor c = resolver.query(Patients.CONTENT_URI, null, null, null, null);
        LOG.i("Examining patients: " + c.getCount() + " local, " + cvs.size() + " from server");

        List<ContentProviderOperation> ops = new ArrayList<>();
        try {
            while (c.moveToNext()) {
                String localId = Utils.getString(c, Patients._ID);
                Uri uri = Patients.CONTENT_URI.buildUpon().appendPath(localId).build();
                syncResult.stats.numEntries++;

                ContentValues cv = cvs.remove(localId);
                if (cv != null) {
                    ContentValues localCv = new ContentValues();
                    DatabaseUtils.cursorRowToContentValues(c, localCv);
                    if (!cv.equals(localCv)) {  // record has changed on server
                        LOG.i("  - will update patient " + localId);
                        ops.add(ContentProviderOperation.newUpdate(uri).withValues(cv).build());
                        syncResult.stats.numUpdates++;
                    }
                } else {  // record doesn't exist on server
                    LOG.i("  - will delete patient " + localId);
                    ops.add(ContentProviderOperation.newDelete(uri).build());
                    syncResult.stats.numDeletes++;
                }
            }
        } finally {
            c.close();
        }

        for (ContentValues values : cvs.values()) {  // server has a new record
            LOG.i("  - will insert patient " + values.getAsString(Patients._ID));
            ops.add(ContentProviderOperation.newInsert(Patients.CONTENT_URI).withValues(values).build());
            syncResult.stats.numInserts++;
        }
        
        return ops;
    }
    
    /** Converts a chart data response into appropriate inserts in the chart table. */
    public static List<ContentValues> getObsValuesToInsert(
            JsonPatientRecord response, SyncResult syncResult) {
        List<ContentValues> cvs = new ArrayList<>();
        final String patientUuid = response.uuid;
        for (JsonEncounter encounter : response.encounters) {
            if (encounter.uuid == null) {
                LOG.e("Patient %s has an encounter with uuid = null", patientUuid);
                continue;
            }
            final String encounterUuid = encounter.uuid;
            DateTime timestamp = encounter.timestamp;
            if (timestamp == null) {
                LOG.e("Encounter %s has timestamp = null", encounterUuid);
                continue;
            }
            final int encounterTime = (int) (timestamp.getMillis() / 1000); // seconds since epoch
            ContentValues base = new ContentValues();
            base.put(Observations.PATIENT_UUID, patientUuid);
            base.put(Observations.ENCOUNTER_UUID, encounterUuid);
            base.put(Observations.ENCOUNTER_TIME, encounterTime);

            if (encounter.observations != null) {
                for (Map.Entry<Object, Object> entry : encounter.observations.entrySet()) {
                    final String conceptUuid = (String) entry.getKey();
                    ContentValues values = new ContentValues(base);
                    values.put(Observations.CONCEPT_UUID, conceptUuid);
                    values.put(Observations.VALUE, entry.getValue().toString());
                    cvs.add(values);
                    syncResult.stats.numInserts++;
                }
            }
            if (encounter.order_uuids != null) {
                for (String orderUuid : encounter.order_uuids) {
                    ContentValues values = new ContentValues(base);
                    values.put(Observations.CONCEPT_UUID, AppModel.ORDER_EXECUTED_CONCEPT_UUID);
                    values.put(Observations.VALUE, orderUuid);
                    cvs.add(values);
                    syncResult.stats.numInserts++;
                }
            }
        }
        return cvs;
    }

    /**
     * Gets orders from the server and returns a list of operations that will
     * update the database with the new orders and edits to existing orders.
     */
    public static ArrayList<ContentProviderOperation> getOrderUpdateOps(SyncResult syncResult)
            throws ExecutionException, InterruptedException {
        // Request all orders from the server.
        RequestFuture<List<JsonOrder>> future = RequestFuture.newFuture();
        App.getServer().listOrders(future, future);
        Map<String, JsonOrder> ordersToStore = new HashMap<>();
        for (JsonOrder order : future.get()) {
            ordersToStore.put(order.uuid, order);
        }

        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        final ContentResolver resolver = App.getInstance().getContentResolver();
        Cursor c = resolver.query(Orders.CONTENT_URI, new String[] {
                Orders.UUID,
                Orders.PATIENT_UUID,
                Orders.INSTRUCTIONS,
                Orders.START_TIME,
                Orders.STOP_TIME
        }, null, null, null);
        try {
            LOG.i("Examining orders: %d local, %d from server.", c.getCount(), ordersToStore.size());
            // Scan all the locally stored orders, updating the orders we've just received.
            while (c.moveToNext()) {
                String uuid = c.getString(c.getColumnIndex(Orders.UUID));
                Uri uri = Orders.CONTENT_URI.buildUpon().appendPath(uuid).build();
                JsonOrder order = ordersToStore.get(uuid);
                if (order != null) {  // apply update to a local order
                    LOG.v("  - will update order " + uuid);
                    ops.add(ContentProviderOperation.newUpdate(uri)
                            .withValue(Orders.PATIENT_UUID, order.patient_uuid)
                            .withValue(Orders.INSTRUCTIONS, order.instructions)
                            .withValue(Orders.START_TIME, order.start)
                            .withValue(Orders.STOP_TIME, order.stop)
                            .build());
                    ordersToStore.remove(uuid);  // done with this incoming order
                    syncResult.stats.numUpdates++;
                } else {  // delete the local order (the server doesn't have it)
                    LOG.v("  - will delete order " + uuid);
                    ops.add(ContentProviderOperation.newDelete(uri).build());
                    syncResult.stats.numDeletes++;
                }
            }
        } finally {
            c.close();
        }

        // Store all the remaining received orders as new orders.
        for (JsonOrder order : ordersToStore.values()) {
            LOG.v("  - will insert order " + order.uuid);
            ops.add(ContentProviderOperation.newInsert(Orders.CONTENT_URI)
                    .withValue(Orders.UUID, order.uuid)
                    .withValue(Orders.PATIENT_UUID, order.patient_uuid)
                    .withValue(Orders.INSTRUCTIONS, order.instructions)
                    .withValue(Orders.START_TIME, order.start)
                    .withValue(Orders.STOP_TIME, order.stop)
                    .build());
            syncResult.stats.numInserts++;
        }
        return ops;
    }

    /** Given a set of users, replaces the current set of users with users from that set. */
    public static ArrayList<ContentProviderOperation> getUserUpdateOps(
            Set<JsonUser> response, SyncResult syncResult) {
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        // Delete all users before inserting.
        ops.add(ContentProviderOperation.newDelete(Users.CONTENT_URI).build());
        // TODO: Update syncResult delete counts.
        for (JsonUser user : response) {
            ops.add(ContentProviderOperation.newInsert(Users.CONTENT_URI)
                    .withValue(Users.UUID, user.id)
                    .withValue(Users.FULL_NAME, user.fullName)
                    .build());
            syncResult.stats.numInserts++;
        }
        return ops;
    }

    /**
     * Requests locations from the server and transforms the response into an {@link ArrayList} of
     * {@link ContentProviderOperation}s for updating the database.
     */
    public static ArrayList<ContentProviderOperation> getLocationUpdateOps(SyncResult syncResult)
            throws ExecutionException, InterruptedException {
        final ContentResolver contentResolver = App.getInstance().getContentResolver();

        final String[] projection = new String[] {
                Locations.LOCATION_UUID,
                Locations.PARENT_UUID
        };
        final String[] namesProjection = new String[] {
                LocationNames._ID,
                LocationNames.LOCATION_UUID,
                LocationNames.LOCALE,
                LocationNames.NAME
        };

        LOG.d("Before network call");
        RequestFuture<List<JsonLocation>> future = RequestFuture.newFuture();
        App.getServer().listLocations(future, future);

        // No need for callbacks as the {@AbstractThreadedSyncAdapter} code is executed in a
        // background thread
        List<JsonLocation> locations = future.get();
        LOG.d("After network call");
        ArrayList<ContentProviderOperation> batch = new ArrayList<>();

        Map<String, JsonLocation> locationsByUuid = new HashMap<>();
        for (JsonLocation location : locations) {
            locationsByUuid.put(location.uuid, location);
        }

        // Get list of all items
        Uri uri = Locations.CONTENT_URI; // Location tree
        Uri namesUri = LocationNames.CONTENT_URI; // Location names
        Cursor c = contentResolver.query(uri, projection, null, null, null);
        assert c != null;
        Cursor namesCur = contentResolver.query(namesUri, namesProjection, null, null, null);
        assert namesCur != null;
        LOG.i("Examining locations: %d local, %d from server", c.getCount(), locations.size());

        String id;
        String parentId;

        // Build map of location names from the database.
        Map<String, Map<String, String>> dbLocationNames = new HashMap<>();
        while (namesCur.moveToNext()) {
            String locationId = namesCur.getString(
                    namesCur.getColumnIndex(LocationNames.LOCATION_UUID));
            String locale = namesCur.getString(
                    namesCur.getColumnIndex(LocationNames.LOCALE));
            String name = namesCur.getString(
                    namesCur.getColumnIndex(LocationNames.NAME));
            if (locationId == null || locale == null || name == null) {
                continue;
            }

            if (!dbLocationNames.containsKey(locationId)) {
                dbLocationNames.put(locationId, new HashMap<String, String>());
            }

            dbLocationNames.get(locationId).put(locale, name);
        }
        namesCur.close();

        // Iterate through the list of locations
        while (c.moveToNext()) {
            syncResult.stats.numEntries++;

            id = c.getString(c.getColumnIndex(Locations.LOCATION_UUID));
            parentId = c.getString(c.getColumnIndex(Locations.PARENT_UUID));

            JsonLocation location = locationsByUuid.get(id);
            if (location != null) {
                // Entry exists. Remove from entry map to prevent insert later.
                locationsByUuid.remove(id);

                // Grab the names stored in the database for this location.
                Map<String, String> locationNames = dbLocationNames.get(id);

                // Check to see if the entry needs to be updated
                Uri existingUri = uri.buildUpon().appendPath(String.valueOf(id)).build();

                if (location.parent_uuid != null && !location.parent_uuid.equals(parentId)) {
                    // Update existing record
                    LOG.i("  - will update location " + id);
                    batch.add(ContentProviderOperation.newUpdate(existingUri)
                            .withValue(Locations.LOCATION_UUID, id)
                            .withValue(Locations.PARENT_UUID, parentId)
                            .build());
                    syncResult.stats.numUpdates++;
                }

                if (location.names != null
                        && (locationNames == null || !location.names.equals(locationNames))) {
                    Uri existingNamesUri = namesUri.buildUpon().appendPath(
                            String.valueOf(id)).build();
                    // Update location names by deleting any existing location names and
                    // repopulating.
                    batch.add(ContentProviderOperation.newDelete(existingNamesUri).build());
                    syncResult.stats.numDeletes++;
                    for (String locale : location.names.keySet()) {

                        batch.add(ContentProviderOperation.newInsert(existingNamesUri)
                                .withValue(LocationNames.LOCATION_UUID, id)
                                .withValue(LocationNames.LOCALE, locale)
                                .withValue(LocationNames.NAME, location.names.get(locale))
                                .build());
                        syncResult.stats.numInserts++;
                    }
                }
            } else {
                // Entry doesn't exist. Remove it from the database.
                LOG.i("  - will delete location " + id);
                Uri deleteUri = uri.buildUpon().appendPath(id).build();
                batch.add(ContentProviderOperation.newDelete(deleteUri).build());
                syncResult.stats.numDeletes++;
                Uri namesDeleteUri = namesUri.buildUpon().appendPath(id).build();
                batch.add(ContentProviderOperation.newDelete(namesDeleteUri).build());
                syncResult.stats.numDeletes++;
            }
        }
        c.close();

        for (JsonLocation location : locationsByUuid.values()) {
            LOG.i("  - will insert location " + location.uuid);
            batch.add(ContentProviderOperation.newInsert(Locations.CONTENT_URI)
                    .withValue(Locations.LOCATION_UUID, location.uuid)
                    .withValue(Locations.PARENT_UUID, location.parent_uuid)
                    .build());
            syncResult.stats.numInserts++;

            if (location.names != null) {
                for (String locale : location.names.keySet()) {
                    Uri existingNamesUri = namesUri.buildUpon().appendPath(
                            String.valueOf(location.uuid)).build();
                    batch.add(ContentProviderOperation.newInsert(existingNamesUri)
                            .withValue(LocationNames.LOCATION_UUID, location.uuid)
                            .withValue(LocationNames.LOCALE, locale)
                            .withValue(LocationNames.NAME, location.names.get(locale))
                            .build());
                    syncResult.stats.numInserts++;
                }
            }
        }

        return batch;
    }
}
