package org.msf.records.sync;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.RequestFuture;

import org.joda.time.DateTime;
import org.msf.records.App;
import org.msf.records.net.model.ChartGroup;
import org.msf.records.net.model.ChartStructure;
import org.msf.records.net.model.Encounter;
import org.msf.records.net.model.Location;
import org.msf.records.net.model.PatientChart;
import org.msf.records.net.model.User;
import org.msf.records.sync.providers.Contracts;
import org.msf.records.utils.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * A helper class for turning the Java beans that are the result of chart RPC calls into
 * appropriate ContentProviderOperations for inserting into the DB.
 */
public class RpcToDb {

    private static final Logger LOG = Logger.create();

    /**
     * Convert a ChartStructure response into appropriate inserts in the chart table.
     */
    public static ArrayList<ContentProviderOperation> chartStructureRpcToDb(
            ChartStructure response, SyncResult syncResult) {
        ArrayList<ContentProviderOperation> operations = new ArrayList<>();
        String chartUuid = response.uuid;
        if (chartUuid == null) {
            LOG.e("null chart encounterUuid when fetching chart structure");
        }
        int chartRow = 0;
        String groupUuid;
        for (ChartGroup group : response.groups) {
            groupUuid = group.uuid;
            if (groupUuid == null) {
                LOG.e("null group encounterUuid for chart " + chartUuid);
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

    /**
     * Convert a ChartStructure response into appropriate inserts in the chart table.
     */
    public static void observationsRpcToDb(
            PatientChart response, SyncResult syncResult, ArrayList<ContentValues> result) {
        final String patientUuid = response.uuid;
        for (Encounter encounter : response.encounters) {
            if (encounter.uuid == null) {
                LOG.e("Encounter encounterUuid was null for " + patientUuid);
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
     * Given a set of users, replaces the current set of users with users from that set.
     */
    public static ArrayList<ContentProviderOperation> userSetFromRpcToDb(
            Set<User> response, SyncResult syncResult) {
        ArrayList<ContentProviderOperation> operations = new ArrayList<>();
        // Delete all users before inserting.
        operations.add(
                ContentProviderOperation.newDelete(Contracts.Users.CONTENT_URI).build());
        // TODO(akalachman): Update syncResult delete counts.
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

    public static ArrayList<ContentProviderOperation> locationsRpcToDb(SyncResult syncResult)
            throws ExecutionException, InterruptedException {
        final ContentResolver contentResolver = App.getInstance().getContentResolver();

        final String[] projection = LocationProjection.getLocationProjection();
        final String[] namesProjection = LocationProjection.getLocationNamesProjection();

        LOG.d("Before network call");
        RequestFuture<List<Location>> future = RequestFuture.newFuture();
        App.getServer().listLocations(future, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                LOG.d(error.toString());
            }
        });

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

        String id, parentId;

        // Build map of location names from the database.
        HashMap<String, HashMap<String, String>> dbLocationNames =
                new HashMap<>();
        while (namesCur.moveToNext()) {
            String locationId = namesCur.getString(
                    LocationProjection.LOCATION_NAME_LOCATION_UUID_COLUMN);
            String locale = namesCur.getString(LocationProjection.LOCATION_NAME_LOCALE_COLUMN);
            String name = namesCur.getString(LocationProjection.LOCATION_NAME_NAME_COLUMN);
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
        while(c.moveToNext()){
            syncResult.stats.numEntries++;

            id = c.getString(LocationProjection.LOCATION_LOCATION_UUID_COLUMN);
            parentId = c.getString(LocationProjection.LOCATION_PARENT_UUID_COLUMN);

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

                if (location.names != null &&
                        (locationNames == null || !location.names.equals(locationNames))) {
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
                                        Contracts.LocationNames.LOCALIZED_NAME,
                                        location.names.get(locale))
                                .build());
                        syncResult.stats.numInserts++;
                    }
                }
            } else {
                // Entry doesn't exist. Remove it from the database.
                Uri deleteUri = uri.buildUpon().appendPath(id).build();
                Uri namesDeleteUri = namesUri.buildUpon().appendPath(id).build();
                LOG.i("Scheduling delete: " + deleteUri);
                batch.add(ContentProviderOperation.newDelete(deleteUri).build());
                syncResult.stats.numDeletes++;
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
                                    Contracts.LocationNames.LOCALIZED_NAME,
	                                location.names.get(locale))
	                        .build());
	                syncResult.stats.numInserts++;
	            }
            }
        }

        return batch;
    }
}