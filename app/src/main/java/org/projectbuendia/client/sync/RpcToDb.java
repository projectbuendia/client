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
import android.net.Uri;

import com.android.volley.toolbox.RequestFuture;

import org.joda.time.DateTime;
import org.projectbuendia.client.App;
import org.projectbuendia.client.net.model.ChartGroup;
import org.projectbuendia.client.net.model.ChartStructure;
import org.projectbuendia.client.net.model.Encounter;
import org.projectbuendia.client.net.model.Location;
import org.projectbuendia.client.net.model.Order;
import org.projectbuendia.client.net.model.PatientChart;
import org.projectbuendia.client.net.model.User;
import org.projectbuendia.client.sync.providers.Contracts;
import org.projectbuendia.client.utils.Logger;

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
public class RpcToDb {

    private static final Logger LOG = Logger.create();

    /** Converts a ChartStructure response into appropriate inserts in the chart table. */
    public static ArrayList<ContentProviderOperation> chartStructureRpcToDb(
            ChartStructure response, SyncResult syncResult) {
        ArrayList<ContentProviderOperation> operations = new ArrayList<>();
        String chartUuid = response.uuid;
        if (chartUuid == null) {
            LOG.e("null chart uuid when fetching chart structure");
        }
        int chartRow = 0;
        String groupUuid;
        for (ChartGroup group : response.groups) {
            groupUuid = group.uuid;
            if (groupUuid == null) {
                LOG.e("null group uuid for chart " + chartUuid);
                continue;
            }
            for (String conceptUuid : group.concepts) {
                operations.add(ContentProviderOperation
                        .newInsert(Contracts.Charts.CONTENT_URI)
                        .withValue(Contracts.Charts.CHART_UUID, chartUuid)
                        .withValue(Contracts.Charts.CHART_ROW, chartRow++)
                        .withValue(Contracts.Charts.GROUP_UUID, groupUuid)
                        .withValue(Contracts.Charts.CONCEPT_UUID, conceptUuid)
                        .build());
                syncResult.stats.numInserts++;
            }
        }
        return operations;
    }

    /** Converts a Order response into appropriate inserts in the chart table. */
    public static void observationsRpcToDb(
            PatientChart response, SyncResult syncResult, ArrayList<ContentValues> result) {
        final String patientUuid = response.uuid;
        for (Encounter encounter : response.encounters) {
            if (encounter.uuid == null) {
                LOG.e("Encounter uuid was null for " + patientUuid);
                continue;
            }
            final String encounterUuid = encounter.uuid;
            DateTime timestamp = encounter.timestamp;
            if (timestamp == null) {
                LOG.e("Encounter timestamp was null for " + encounterUuid);
                continue;
            }
            final int encounterTime = (int) (timestamp.getMillis() / 1000); // seconds since epoch
            ContentValues base = new ContentValues();
            base.put(Contracts.Observations.PATIENT_UUID, patientUuid);
            base.put(Contracts.Observations.ENCOUNTER_UUID, encounterUuid);
            base.put(Contracts.Observations.ENCOUNTER_TIME, encounterTime);

            for (Map.Entry<Object, Object> entry : encounter.observations.entrySet()) {
                final String conceptUuid = (String) entry.getKey();
                ContentValues values = new ContentValues(base);
                values.put(Contracts.Observations.CONCEPT_UUID, conceptUuid);
                values.put(Contracts.Observations.VALUE, entry.getValue().toString());
                result.add(values);
                syncResult.stats.numInserts++;
            }
        }
    }

    /**
     * Gets orders from the server and returns a list of operations that will
     * update the database with the new orders and edits to existing orders.
     */
    public static ArrayList<ContentProviderOperation> ordersRpcToDb(SyncResult syncResult)
            throws ExecutionException, InterruptedException {
        // Request all orders from the server.
        RequestFuture<List<Order>> future = RequestFuture.newFuture();
        App.getServer().listOrders(future, future);
        Map<String, Order> ordersToStore = new HashMap<>();
        for (Order order : future.get()) {
            ordersToStore.put(order.uuid, order);
        }

        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        final ContentResolver resolver = App.getInstance().getContentResolver();
        Cursor c = resolver.query(Contracts.Orders.CONTENT_URI, new String[] {
                Contracts.Orders.UUID,
                Contracts.Orders.PATIENT_UUID,
                Contracts.Orders.INSTRUCTIONS,
                Contracts.Orders.START_TIME,
                Contracts.Orders.STOP_TIME
        }, null, null, null);

        LOG.i("Merging in orders: client has %d, server has %d.", c.getCount(), ordersToStore.size());
        // Scan all the locally stored orders, updating the orders we've just received.
        while (c.moveToNext()) {
            String uuid = c.getString(c.getColumnIndex(Contracts.Orders.UUID));
            Uri uri = Contracts.Orders.CONTENT_URI.buildUpon().appendPath(uuid).build();
            Order order = ordersToStore.get(uuid);
            if (order != null) {  // apply update to a local order
                LOG.v("  - will update order " + uuid);
                ops.add(ContentProviderOperation.newUpdate(uri)
                        .withValue(Contracts.Orders.PATIENT_UUID, order.patient_uuid)
                        .withValue(Contracts.Orders.INSTRUCTIONS, order.instructions)
                        .withValue(Contracts.Orders.START_TIME, order.start)
                        .withValue(Contracts.Orders.STOP_TIME, order.stop)
                        .build());
                ordersToStore.remove(uuid);  // done with this incoming order
                syncResult.stats.numUpdates++;
            } else {  // delete the local order (the server doesn't have it)
                LOG.v("  - will delete order " + uuid);
                ops.add(ContentProviderOperation.newDelete(uri).build());
                syncResult.stats.numDeletes++;
            }
        }

        // Store all the remaining received orders as new orders.
        for (Order order : ordersToStore.values()) {
            LOG.v("  - will insert order " + order.uuid);
            ops.add(ContentProviderOperation.newInsert(Contracts.Orders.CONTENT_URI)
                    .withValue(Contracts.Orders.UUID, order.uuid)
                    .withValue(Contracts.Orders.PATIENT_UUID, order.patient_uuid)
                    .withValue(Contracts.Orders.INSTRUCTIONS, order.instructions)
                    .withValue(Contracts.Orders.START_TIME, order.start)
                    .withValue(Contracts.Orders.STOP_TIME, order.stop)
                    .build());
            syncResult.stats.numInserts++;
        }
        return ops;
    }

    /** Given a set of users, replaces the current set of users with users from that set. */
    public static ArrayList<ContentProviderOperation> userSetFromRpcToDb(
            Set<User> response, SyncResult syncResult) {
        ArrayList<ContentProviderOperation> operations = new ArrayList<>();
        // Delete all users before inserting.
        operations.add(
                ContentProviderOperation.newDelete(Contracts.Users.CONTENT_URI).build());
        // TODO: Update syncResult delete counts.
        for (User user : response) {
            operations.add(ContentProviderOperation
                    .newInsert(Contracts.Users.CONTENT_URI)
                    .withValue(Contracts.Users.UUID, user.id)
                    .withValue(Contracts.Users.FULL_NAME, user.fullName)
                    .build());
            syncResult.stats.numInserts++;
        }
        return operations;
    }

    /**
     * Requests locations from the server and transforms the response into an {@link ArrayList} of
     * {@link ContentProviderOperation}s for updating the database.
     */
    public static ArrayList<ContentProviderOperation> locationsRpcToDb(SyncResult syncResult)
            throws ExecutionException, InterruptedException {
        final ContentResolver contentResolver = App.getInstance().getContentResolver();

        final String[] projection = new String[] {
                Contracts.Locations.LOCATION_UUID,
                Contracts.Locations.PARENT_UUID
        };
        final String[] namesProjection = new String[] {
                Contracts.LocationNames._ID,
                Contracts.LocationNames.LOCATION_UUID,
                Contracts.LocationNames.LOCALE,
                Contracts.LocationNames.NAME
        };

        LOG.d("Before network call");
        RequestFuture<List<Location>> future = RequestFuture.newFuture();
        App.getServer().listLocations(future, future);

        // No need for callbacks as the {@AbstractThreadedSyncAdapter} code is executed in a
        // background thread
        List<Location> locations = future.get();
        LOG.d("After network call");
        ArrayList<ContentProviderOperation> batch = new ArrayList<>();


        HashMap<String, Location> locationsMap = new HashMap<>();
        for (Location location : locations) {
            locationsMap.put(location.uuid, location);
        }

        // Get list of all items
        LOG.i("Fetching local entries for merge");
        Uri uri = Contracts.Locations.CONTENT_URI; // Location tree
        Uri namesUri = Contracts.LocationNames.CONTENT_URI; // Location names
        Cursor c = contentResolver.query(uri, projection, null, null, null);
        assert c != null;
        Cursor namesCur = contentResolver.query(namesUri, namesProjection, null, null, null);
        assert namesCur != null;
        LOG.i("Found " + c.getCount() + " local entries. Computing merge solution...");
        LOG.i("Found " + locations.size() + " external entries. Computing merge solution...");

        String id;
        String parentId;

        // Build map of location names from the database.
        HashMap<String, HashMap<String, String>> dbLocationNames =
                new HashMap<>();
        while (namesCur.moveToNext()) {
            String locationId = namesCur.getString(
                    namesCur.getColumnIndex(Contracts.LocationNames.LOCATION_UUID));
            String locale = namesCur.getString(
                    namesCur.getColumnIndex(Contracts.LocationNames.LOCALE));
            String name = namesCur.getString(
                    namesCur.getColumnIndex(Contracts.LocationNames.NAME));
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

            id = c.getString(c.getColumnIndex(Contracts.Locations.LOCATION_UUID));
            parentId = c.getString(c.getColumnIndex(Contracts.Locations.PARENT_UUID));

            Location location = locationsMap.get(id);
            if (location != null) {
                // Entry exists. Remove from entry map to prevent insert later.
                locationsMap.remove(id);

                // Grab the names stored in the database for this location.
                HashMap<String, String> locationNames = dbLocationNames.get(id);

                // Check to see if the entry needs to be updated
                Uri existingUri = uri.buildUpon().appendPath(String.valueOf(id)).build();

                if (location.parent_uuid != null && !location.parent_uuid.equals(parentId)) {
                    // Update existing record
                    LOG.i("Scheduling update: " + existingUri);
                    batch.add(ContentProviderOperation.newUpdate(existingUri)
                            .withValue(Contracts.Locations.LOCATION_UUID, id)
                            .withValue(Contracts.Locations.PARENT_UUID, parentId)
                            .build());
                    syncResult.stats.numUpdates++;
                } else {
                    LOG.i("No action required for " + existingUri);
                }

                if (location.names != null
                        && (locationNames == null || !location.names.equals(locationNames))) {
                    Uri existingNamesUri = namesUri.buildUpon().appendPath(
                            String.valueOf(id)).build();
                    // Update location names by deleting any existing location names and
                    // repopulating.
                    LOG.i("Scheduling location names update: " + existingNamesUri);
                    batch.add(ContentProviderOperation.newDelete(existingNamesUri).build());
                    syncResult.stats.numDeletes++;
                    for (String locale : location.names.keySet()) {
                        batch.add(ContentProviderOperation.newInsert(existingNamesUri)
                                .withValue(
                                        Contracts.LocationNames.LOCATION_UUID, id)
                                .withValue(
                                        Contracts.LocationNames.LOCALE, locale)
                                .withValue(
                                        Contracts.LocationNames.NAME,
                                        location.names.get(locale))
                                .build());
                        syncResult.stats.numInserts++;
                    }
                }
            } else {
                // Entry doesn't exist. Remove it from the database.
                Uri deleteUri = uri.buildUpon().appendPath(id).build();
                LOG.i("Scheduling delete: " + deleteUri);
                batch.add(ContentProviderOperation.newDelete(deleteUri).build());
                syncResult.stats.numDeletes++;

                Uri namesDeleteUri = namesUri.buildUpon().appendPath(id).build();
                LOG.i("Scheduling delete: " + namesDeleteUri);
                batch.add(ContentProviderOperation.newDelete(namesDeleteUri).build());
                syncResult.stats.numDeletes++;
            }
        }
        c.close();


        for (Location location : locationsMap.values()) {
            LOG.i("Scheduling insert: entry_id=" + location.uuid);
            batch.add(ContentProviderOperation.newInsert(Contracts.Locations.CONTENT_URI)
                    .withValue(Contracts.Locations.LOCATION_UUID, location.uuid)
                    .withValue(Contracts.Locations.PARENT_UUID, location.parent_uuid)
                    .build());
            syncResult.stats.numInserts++;

            if (location.names != null) {
                for (String locale : location.names.keySet()) {
                    Uri existingNamesUri = namesUri.buildUpon().appendPath(
                            String.valueOf(location.uuid)).build();
                    batch.add(ContentProviderOperation.newInsert(existingNamesUri)
                            .withValue(
                                    Contracts.LocationNames.LOCATION_UUID, location.uuid)
                            .withValue(
                                    Contracts.LocationNames.LOCALE, locale)
                            .withValue(
                                    Contracts.LocationNames.NAME,
                                    location.names.get(locale))
                            .build());
                    syncResult.stats.numInserts++;
                }
            }
        }

        return batch;
    }
}
