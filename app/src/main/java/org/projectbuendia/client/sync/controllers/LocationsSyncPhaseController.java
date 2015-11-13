package org.projectbuendia.client.sync.controllers;

import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.SyncResult;

import org.projectbuendia.client.providers.Contracts;
import org.projectbuendia.client.sync.DbSyncHelper;

import java.util.ArrayList;

/**
 * Handles syncing locations. All locations are always fetched, which is ok because the full set of
 * locations is fairly smaller.
 */
public class LocationsSyncPhaseController implements SyncPhaseController {
    @Override
    public void sync(ContentResolver contentResolver, SyncResult syncResult,
            ContentProviderClient providerClient)
            throws Throwable {
        ArrayList<ContentProviderOperation> ops = DbSyncHelper.getLocationUpdateOps(syncResult);
        providerClient.applyBatch(ops);
        contentResolver.notifyChange(Contracts.Locations.CONTENT_URI, null, false);
        contentResolver.notifyChange(Contracts.LocationNames.CONTENT_URI, null, false);
    }
}
