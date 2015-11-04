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
import org.projectbuendia.client.providers.Contracts;
import org.projectbuendia.client.utils.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Syncs locations from the server to the local database. Note that this is a full sync every time;
 * but seeing as the size of the dataset is small, this shouldn't have a significant performance
 * impact.
 */
public class LocationsSyncController implements SyncController {
    private static final Logger LOG = Logger.create();

    @Override
    public void sync(SyncResult syncResult, ContentProviderClient providerClient, ContentResolver contentResolver) throws Throwable {
        ArrayList<ContentProviderOperation> ops = getLocationUpdateOps(syncResult);
        LOG.i("Applying batch update of locations.");
        contentResolver.applyBatch(Contracts.CONTENT_AUTHORITY, ops);
        contentResolver.notifyChange(Contracts.Locations.CONTENT_URI, null, false);
        contentResolver.notifyChange(Contracts.LocationNames.CONTENT_URI, null, false);
    }

    /**
     * Requests locations from the server and transforms the response into an {@link ArrayList} of
     * {@link ContentProviderOperation}s for updating the database.
     */
    private static ArrayList<ContentProviderOperation> getLocationUpdateOps(SyncResult syncResult)
            throws ExecutionException, InterruptedException {
        final ContentResolver contentResolver = App.getInstance().getContentResolver();

        final String[] projection = new String[] {
                Contracts.Locations.UUID,
                Contracts.Locations.PARENT_UUID
        };
        final String[] namesProjection = new String[] {
                Contracts.LocationNames.LOCATION_UUID,
                Contracts.LocationNames.LOCALE,
                Contracts.LocationNames.NAME
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
        Uri uri = Contracts.Locations.CONTENT_URI; // Location tree
        Uri namesUri = Contracts.LocationNames.CONTENT_URI; // Location names
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
                    namesCur.getColumnIndex(Contracts.LocationNames.LOCATION_UUID));
            String locale = namesCur.getString(
                    namesCur.getColumnIndex(Contracts.LocationNames.LOCALE));
            String name = namesCur.getString(
                    namesCur.getColumnIndex(Contracts.LocationNames.NAME));
            if (locationUuid == null || locale == null || name == null) continue;

            if (!dbLocationNames.containsKey(locationUuid)) {
                dbLocationNames.put(locationUuid, new HashMap<String, String>());
            }

            dbLocationNames.get(locationUuid).put(locale, name);
        }
        namesCur.close();

        // Iterate through the list of locations
        while (c.moveToNext()) {
            syncResult.stats.numEntries++;

            uuid = c.getString(c.getColumnIndex(Contracts.Locations.UUID));
            parentUuid = c.getString(c.getColumnIndex(Contracts.Locations.PARENT_UUID));

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
                    LOG.i("  - will update location " + uuid);
                    batch.add(ContentProviderOperation.newUpdate(existingUri)
                            .withValue(Contracts.Locations.UUID, uuid)
                            .withValue(Contracts.Locations.PARENT_UUID, parentUuid)
                            .build());
                    syncResult.stats.numUpdates++;
                }

                if (location.names != null
                        && (locationNames == null || !location.names.equals(locationNames))) {
                    Uri existingNamesUri = namesUri.buildUpon().appendPath(
                            String.valueOf(uuid)).build();
                    // Update location names by deleting any existing location names and
                    // repopulating.
                    batch.add(ContentProviderOperation.newDelete(existingNamesUri).build());
                    syncResult.stats.numDeletes++;
                    for (String locale : location.names.keySet()) {

                        batch.add(ContentProviderOperation.newInsert(existingNamesUri)
                                .withValue(Contracts.LocationNames.LOCATION_UUID, uuid)
                                .withValue(Contracts.LocationNames.LOCALE, locale)
                                .withValue(Contracts.LocationNames.NAME, location.names.get(locale))
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
            batch.add(ContentProviderOperation.newInsert(Contracts.Locations.CONTENT_URI)
                    .withValue(Contracts.Locations.UUID, location.uuid)
                    .withValue(Contracts.Locations.PARENT_UUID, location.parent_uuid)
                    .build());
            syncResult.stats.numInserts++;

            if (location.names != null) {
                for (String locale : location.names.keySet()) {
                    Uri existingNamesUri = namesUri.buildUpon().appendPath(
                            String.valueOf(location.uuid)).build();
                    batch.add(ContentProviderOperation.newInsert(existingNamesUri)
                            .withValue(Contracts.LocationNames.LOCATION_UUID, location.uuid)
                            .withValue(Contracts.LocationNames.LOCALE, locale)
                            .withValue(Contracts.LocationNames.NAME, location.names.get(locale))
                            .build());
                    syncResult.stats.numInserts++;
                }
            }
        }

        return batch;
    }
}
