package org.projectbuendia.client.sync.controllers;

import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;

import com.android.volley.toolbox.RequestFuture;

import org.projectbuendia.client.App;
import org.projectbuendia.client.json.JsonLocation;
import org.projectbuendia.client.providers.Contracts.LocalizedLocations;
import org.projectbuendia.client.providers.Contracts.LocationNames;
import org.projectbuendia.client.providers.Contracts.Locations;
import org.projectbuendia.client.utils.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Handles syncing locations. All locations are always fetched, which is ok because the full set of
 * locations is fairly smaller.
 */
public class LocationsSyncPhaseRunnable implements SyncPhaseRunnable {
    private static final Logger LOG = Logger.create();

    @Override
    public void sync(ContentResolver contentResolver, SyncResult syncResult,
            ContentProviderClient providerClient)
            throws Throwable {
        ArrayList<ContentProviderOperation> ops = getLocationUpdateOps(syncResult);
        providerClient.applyBatch(ops);
        contentResolver.notifyChange(Locations.URI, null, false);
        contentResolver.notifyChange(LocationNames.URI, null, false);
        contentResolver.notifyChange(LocalizedLocations.URI, null, false);
    }

    /**
     * Requests locations from the server and transforms the response into an {@link ArrayList} of
     * {@link ContentProviderOperation}s for updating the database.
     */
    private static ArrayList<ContentProviderOperation> getLocationUpdateOps(SyncResult syncResult)
            throws ExecutionException, InterruptedException {
        final ContentResolver contentResolver = App.getInstance().getContentResolver();

        final String[] projection = new String[] {
                Locations.UUID,
                Locations.PARENT_UUID
        };
        final String[] namesProjection = new String[] {
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
        Uri uri = Locations.URI; // Location forest
        Uri namesUri = LocationNames.URI; // Location names
        Cursor c = contentResolver.query(uri, projection, null, null, null);
        assert c != null;
        Cursor namesCur = contentResolver.query(namesUri, namesProjection, null, null, null);
        assert namesCur != null;
        LOG.i("Examining locations: %d local, %d from server", c.getCount(), locations.size());

        String uuid;
        String parentUuid;

        // Build map of location names from the database.
        Map<String, Map<String, String>> dbLocationNames = new HashMap<>();
        while (namesCur.moveToNext()) {
            String locationUuid = namesCur.getString(
                    namesCur.getColumnIndex(LocationNames.LOCATION_UUID));
            String locale = namesCur.getString(
                    namesCur.getColumnIndex(LocationNames.LOCALE));
            String name = namesCur.getString(
                    namesCur.getColumnIndex(LocationNames.NAME));
            if (locationUuid == null || locale == null || name == null) continue;

            if (!dbLocationNames.containsKey(locationUuid)) {
                dbLocationNames.put(locationUuid, new HashMap<>());
            }

            dbLocationNames.get(locationUuid).put(locale, name);
        }
        namesCur.close();

        // Iterate through the list of locations
        while (c.moveToNext()) {
            syncResult.stats.numEntries++;

            uuid = c.getString(c.getColumnIndex(Locations.UUID));
            parentUuid = c.getString(c.getColumnIndex(Locations.PARENT_UUID));

            JsonLocation location = locationsByUuid.get(uuid);
            if (location != null) {
                // Entry exists. Remove from entry map to prevent insert later.
                locationsByUuid.remove(uuid);

                // Grab the names stored in the database for this location.
                Map<String, String> locationNames = dbLocationNames.get(uuid);

                // Check to see if the entry needs to be updated
                Uri existingUri = uri.buildUpon().appendPath(String.valueOf(uuid)).build();

                if (location.parent_uuid != null && !location.parent_uuid.equals(parentUuid)) {
                    // Update existing record
                    LOG.i("  - will reparent location " + uuid);
                    batch.add(ContentProviderOperation.newUpdate(existingUri)
                            .withValue(Locations.UUID, uuid)
                            .withValue(Locations.PARENT_UUID, parentUuid)
                            .build());
                    syncResult.stats.numUpdates++;
                }

                if (location.names != null
                        && (locationNames == null || !location.names.equals(locationNames))) {
                    // Update location names by deleting any existing location names and
                    // repopulating.
                    LOG.i("  - will update names for location " + uuid);
                    batch.add(ContentProviderOperation.newDelete(namesUri)
                        .withSelection(LocationNames.LOCATION_UUID + " = ?", new String[] {uuid})
                        .build());
                    syncResult.stats.numDeletes++;
                    for (String locale : location.names.keySet()) {

                        batch.add(ContentProviderOperation.newInsert(namesUri)
                                .withValue(LocationNames.LOCATION_UUID, uuid)
                                .withValue(LocationNames.LOCALE, locale)
                                .withValue(LocationNames.NAME, location.names.get(locale))
                                .build());
                        syncResult.stats.numInserts++;
                    }
                }
            } else {
                // Entry doesn't exist. Remove it from the database.
                LOG.i("  - will delete location " + uuid);
                Uri deleteUri = uri.buildUpon().appendPath(uuid).build();
                batch.add(ContentProviderOperation.newDelete(deleteUri).build());
                syncResult.stats.numDeletes++;
                Uri namesDeleteUri = namesUri.buildUpon().appendPath(uuid).build();
                batch.add(ContentProviderOperation.newDelete(namesDeleteUri).build());
                syncResult.stats.numDeletes++;
            }
        }
        c.close();

        for (JsonLocation location : locationsByUuid.values()) {
            LOG.i("  - will insert location " + location.uuid);
            batch.add(ContentProviderOperation.newInsert(Locations.URI)
                    .withValue(Locations.UUID, location.uuid)
                    .withValue(Locations.PARENT_UUID, location.parent_uuid)
                    .build());
            syncResult.stats.numInserts++;

            if (location.names != null) {
                for (String locale : location.names.keySet()) {
                    batch.add(ContentProviderOperation.newInsert(namesUri)
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
