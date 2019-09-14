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
import org.projectbuendia.client.providers.Contracts.Locations;
import org.projectbuendia.client.utils.Logger;
import org.projectbuendia.client.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.projectbuendia.client.utils.Utils.eq;

/**
 * Handles syncing locations. All locations are always fetched, which is ok because the full set of
 * locations is fairly smaller.
 */
public class LocationsSyncWorker implements SyncWorker {
    private static final Logger LOG = Logger.create();

    @Override public boolean sync(
        ContentResolver resolver, SyncResult result, ContentProviderClient client
    ) throws Throwable {
        ArrayList<ContentProviderOperation> ops = getLocationUpdateOps(result);
        client.applyBatch(ops);
        if (ops.size() > 0) {
            resolver.notifyChange(Locations.URI, null, false);
        }
        return true;
    }

    /**
     * Requests locations from the server and transforms the response into an {@link ArrayList} of
     * {@link ContentProviderOperation}s for updating the database.
     */
    private static ArrayList<ContentProviderOperation> getLocationUpdateOps(SyncResult syncResult)
            throws ExecutionException, InterruptedException {
        final ContentResolver contentResolver = App.getResolver();

        final String[] projection = new String[] {
            Locations.UUID,
            Locations.NAME,
            Locations.PARENT_UUID
        };

        LOG.d("Before network call");
        RequestFuture<List<JsonLocation>> future = RequestFuture.newFuture();
        App.getServer().listLocations(future, future);

        List<JsonLocation> locations = future.get();
        LOG.d("After network call");
        ArrayList<ContentProviderOperation> batch = new ArrayList<>();

        Map<String, JsonLocation> locationsByUuid = new HashMap<>();
        for (JsonLocation location : locations) {
            locationsByUuid.put(location.uuid, location);
        }

        // Iterate through the locations currently in the local database.
        Uri uri = Locations.URI;
        Cursor c = contentResolver.query(uri, projection, null, null, null);
        assert c != null;
        LOG.i("Examining locations: %d local, %d from server", c.getCount(), locations.size());

        while (c.moveToNext()) {
            syncResult.stats.numEntries++;

            String uuid = Utils.getString(c, Locations.UUID);
            String name = Utils.getString(c, Locations.NAME);
            String parentUuid = Utils.getString(c, Locations.PARENT_UUID);

            JsonLocation location = locationsByUuid.get(uuid);
            if (location != null) {
                // Update locations from the server that also exist locally.
                if (!eq(location.name, name) || !eq(location.parent_uuid, parentUuid)) {
                    LOG.i("  - will update location " + uuid);
                    Uri existingUri = uri.buildUpon().appendPath(String.valueOf(uuid)).build();
                    batch.add(ContentProviderOperation.newUpdate(existingUri)
                        .withValue(Locations.UUID, uuid)
                        .withValue(Locations.NAME, location.name)
                        .withValue(Locations.PARENT_UUID, location.parent_uuid)
                        .build());
                    syncResult.stats.numUpdates++;
                }
                locationsByUuid.remove(uuid);  // remove to prevent insertion later

            } else {
                // Delete locations that exist locally but not on the server.
                LOG.i("  - will delete location " + uuid);
                Uri deleteUri = uri.buildUpon().appendPath(uuid).build();
                batch.add(ContentProviderOperation.newDelete(deleteUri).build());
                syncResult.stats.numDeletes++;
            }
        }
        c.close();

        // Insert all the locations we received that don't exist locally.
        for (JsonLocation location : locationsByUuid.values()) {
            LOG.i("  - will insert location " + location.uuid);
            batch.add(ContentProviderOperation.newInsert(Locations.URI)
                .withValue(Locations.UUID, location.uuid)
                .withValue(Locations.NAME, location.name)
                .withValue(Locations.PARENT_UUID, location.parent_uuid)
                .build());
            syncResult.stats.numInserts++;
        }

        return batch;
    }
}
