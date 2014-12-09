package org.msf.records.sync;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
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
import org.msf.records.net.model.Concept;
import org.msf.records.net.model.ConceptList;
import org.msf.records.net.model.Encounter;
import org.msf.records.net.model.Location;
import org.msf.records.net.model.PatientChart;
import org.msf.records.net.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static org.msf.records.sync.ChartProviderContract.CHART_CONTENT_URI;
import static org.msf.records.sync.ChartProviderContract.CONCEPT_NAMES_CONTENT_URI;
import static org.msf.records.sync.ChartProviderContract.ChartColumns;

/**
 * A helper class for turning the Java beans that are the result of chart RPC calls into
 * appropriate ContentProviderOperations for inserting into the DB.
 */
public class RpcToDb {

    private static final String TAG = "RpcToDb";

    /**
     * Convert a concept response into appropriate inserts in the concept and concept_name tables.
     */
    public static ArrayList<ContentProviderOperation> conceptRpcToDb(ConceptList response,
                                                                     SyncResult syncResult) {
        ArrayList<ContentProviderOperation> operations = new ArrayList<>();
        for (Concept concept : response.results) {
            // This is safe because we have implemented insert on the content provider
            // with replace.
            operations.add(ContentProviderOperation
                    .newInsert(ChartProviderContract.CONCEPTS_CONTENT_URI)
                    .withValue(ChartColumns._ID, concept.uuid)
                    .withValue(ChartColumns.CONCEPT_TYPE, concept.type.name())
                    .build());
            syncResult.stats.numInserts++;
            for (Map.Entry<String, String> entry : concept.names.entrySet()) {
                String locale = entry.getKey();
                if (locale == null) {
                    Log.e(TAG, "null locale in concept name rpc for " + concept);
                    continue;
                }
                String name = entry.getValue();
                if (name == null) {
                    Log.e(TAG, "null name in concept name rpc for " + concept);
                    continue;
                }
                operations.add(ContentProviderOperation
                        .newInsert(CONCEPT_NAMES_CONTENT_URI)
                        .withValue(ChartColumns.CONCEPT_UUID, concept.uuid)
                        .withValue(ChartColumns.LOCALE, locale)
                        .withValue(ChartColumns.NAME, name)
                        .build());
                syncResult.stats.numInserts++;
            }
        }
        return operations;
    }

    /**
     * Convert a ChartStructure response into appropriate inserts in the chart table.
     */
    public static ArrayList<ContentProviderOperation> chartStructureRpcToDb(
            ChartStructure response, SyncResult syncResult) {
        ArrayList<ContentProviderOperation> operations = new ArrayList<>();
        String chartUuid = response.uuid;
        if (chartUuid == null) {
            Log.e(TAG, "null chart uuid when fetching chart structure");
        }
        int chartRow = 0;
        String groupUuid;
        for (ChartGroup group : response.groups) {
            groupUuid = group.uuid;
            if (groupUuid == null) {
                Log.e(TAG, "null group uuid for chart " + chartUuid);
                continue;
            }
            for (String conceptUuid : group.concepts) {
                operations.add(ContentProviderOperation
                        .newInsert(CHART_CONTENT_URI)
                        .withValue(ChartColumns.CHART_UUID, chartUuid)
                        .withValue(ChartColumns.CHART_ROW, chartRow++)
                        .withValue(ChartColumns.GROUP_UUID, groupUuid)
                        .withValue(ChartColumns.CONCEPT_UUID, conceptUuid)
                        .build());
                syncResult.stats.numInserts++;
            }
        }
        return operations;
    }

    /**
     * Convert a ChartStructure response into appropriate inserts in the chart table.
     */
    public static ArrayList<ContentProviderOperation> observationsRpcToDb(
            PatientChart response, SyncResult syncResult) {
        final String patientUuid = response.uuid;
        ArrayList<ContentProviderOperation> operations = new ArrayList<>();
        for (Encounter encounter : response.encounters) {
            if (encounter.uuid == null) {
                Log.e(TAG, "Encounter uuid was null for " + patientUuid);
                continue;
            }
            final String encounterUuid = encounter.uuid;
            DateTime timestamp = encounter.timestamp;
            if (timestamp == null) {
                Log.e(TAG, "Encounter timestamp was null for " + encounterUuid);
                continue;
            }
            final int encounterTime = (int) (timestamp.getMillis() / 1000); // seconds since epoch
            for (Map.Entry<Object, Object> entry : encounter.observations.entrySet()) {
                final String conceptUuid = (String) entry.getKey();
                operations.add(ContentProviderOperation
                        .newInsert(ChartProviderContract.OBSERVATIONS_CONTENT_URI)
                        .withValue(ChartColumns.PATIENT_UUID, patientUuid)
                        .withValue(ChartColumns.ENCOUNTER_UUID, encounterUuid)
                        .withValue(ChartColumns.ENCOUNTER_TIME, encounterTime)
                        .withValue(ChartColumns.CONCEPT_UUID, conceptUuid)
                        .withValue(ChartColumns.VALUE, entry.getValue().toString())
                        .build());
                syncResult.stats.numInserts++;
            }
        }
        return operations;
    }

    /**
     * Given a set of users, replaces the current set of users with users from that set.
     */
    public static ArrayList<ContentProviderOperation> userSetFromRpcToDb(
            Set<User> response, SyncResult syncResult) {
        ArrayList<ContentProviderOperation> operations = new ArrayList<>();
        // Delete all users before inserting.
        operations.add(
                ContentProviderOperation.newDelete(UserProviderContract.USERS_CONTENT_URI).build());
        // TODO(akalachman): Update syncResult delete counts.
        for (User user : response) {
            operations.add(ContentProviderOperation
                    .newInsert(UserProviderContract.USERS_CONTENT_URI)
                    .withValue(UserProviderContract.UserColumns.UUID, user.getId())
                    .withValue(UserProviderContract.UserColumns.FULL_NAME, user.getFullName())
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

        Log.d(TAG, "Before network call");
        RequestFuture<List<Location>> future = RequestFuture.newFuture();
        App.getServer().listLocations(future, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, error.toString());
            }
        }, TAG);

        // No need for callbacks as the {@AbstractThreadedSyncAdapter} code is executed in a
        // background thread
        List<Location> locations = future.get();
        Log.d(TAG, "After network call");
        ArrayList<ContentProviderOperation> batch = new ArrayList<>();


        HashMap<String, Location> locationsMap = new HashMap<>();
        for (Location location : locations) {
            locationsMap.put(location.uuid, location);
        }

        // Get list of all items
        Log.i(TAG, "Fetching local entries for merge");
        Uri uri = LocationProviderContract.LOCATIONS_CONTENT_URI; // Location tree
        Uri namesUri = LocationProviderContract.LOCATION_NAMES_CONTENT_URI; // Location names
        Cursor c = contentResolver.query(uri, projection, null, null, null);
        assert c != null;
        Cursor namesCur = contentResolver.query(namesUri, namesProjection, null, null, null);
        assert namesCur != null;
        Log.i(TAG, "Found " + c.getCount() + " local entries. Computing merge solution...");
        Log.i(TAG, "Found " + locations.size() + " external entries. Computing merge solution...");

        String id, parentId;

        // Build map of location names from the database.
        HashMap<String, HashMap<String, String>> dbLocationNames =
                new HashMap<String, HashMap<String, String>>();
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
                    Log.i(TAG, "Scheduling update: " + existingUri);
                    batch.add(ContentProviderOperation.newUpdate(existingUri)
                            .withValue(LocationProviderContract.LocationColumns.LOCATION_UUID, id)
                            .withValue(LocationProviderContract.LocationColumns.PARENT_UUID, parentId)
                            .build());
                    syncResult.stats.numUpdates++;
                } else {
                    Log.i(TAG, "No action required for " + existingUri);
                }

                if (location.names != null &&
                        (locationNames == null || !location.names.equals(locationNames))) {
                    Uri existingNamesUri = namesUri.buildUpon().appendPath(
                            String.valueOf(id)).build();
                    // Update location names by deleting any existing location names and
                    // repopulating.
                    Log.i(TAG, "Scheduling location names update: " + existingNamesUri);
                    batch.add(ContentProviderOperation.newDelete(existingNamesUri).build());
                    syncResult.stats.numDeletes++;
                    for (String locale : location.names.getLocales()) {
                        batch.add(ContentProviderOperation.newInsert(existingNamesUri)
                                .withValue(
                                        LocationProviderContract.LocationColumns.LOCATION_UUID, id)
                                .withValue(
                                        LocationProviderContract.LocationColumns.LOCALE, locale)
                                .withValue(
                                        LocationProviderContract.LocationColumns.NAME,
                                        location.names.getTranslationForLocale(locale))
                                .build());
                        syncResult.stats.numInserts++;
                    }
                }
            } else {
                // Entry doesn't exist. Remove it from the database.
                Uri deleteUri = uri.buildUpon().appendPath(id).build();
                Uri namesDeleteUri = namesUri.buildUpon().appendPath(id).build();
                Log.i(TAG, "Scheduling delete: " + deleteUri);
                batch.add(ContentProviderOperation.newDelete(deleteUri).build());
                syncResult.stats.numDeletes++;
                Log.i(TAG, "Scheduling delete: " + namesDeleteUri);
                batch.add(ContentProviderOperation.newDelete(namesDeleteUri).build());
                syncResult.stats.numDeletes++;
            }
        }
        c.close();


        for (Location location : locationsMap.values()) {
            Log.i(TAG, "Scheduling insert: entry_id=" + location.uuid);
            batch.add(ContentProviderOperation.newInsert(LocationProviderContract.LOCATIONS_CONTENT_URI)
                    .withValue(LocationProviderContract.LocationColumns.LOCATION_UUID, location.uuid)
                    .withValue(LocationProviderContract.LocationColumns.PARENT_UUID, location.parent_uuid)
                    .build());
            syncResult.stats.numInserts++;

            for (String locale : location.names.getLocales()) {
                Uri existingNamesUri = namesUri.buildUpon().appendPath(
                        String.valueOf(location.uuid)).build();
                batch.add(ContentProviderOperation.newInsert(existingNamesUri)
                        .withValue(
                                LocationProviderContract.LocationColumns.LOCATION_UUID, location.uuid)
                        .withValue(
                                LocationProviderContract.LocationColumns.LOCALE, locale)
                        .withValue(
                                LocationProviderContract.LocationColumns.NAME,
                                location.names.getTranslationForLocale(locale))
                        .build());
                syncResult.stats.numInserts++;
            }
        }

        return batch;
    }
}