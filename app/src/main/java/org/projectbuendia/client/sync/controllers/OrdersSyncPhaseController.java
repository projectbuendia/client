package org.projectbuendia.client.sync.controllers;

import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.SyncResult;

import org.projectbuendia.client.providers.Contracts;
import org.projectbuendia.client.sync.DbSyncHelper;
import org.projectbuendia.client.utils.Logger;

import java.util.ArrayList;

/**
 * Handles syncing orders. Currently we always fetch all orders. This won't scale; incremental
 * fetch would help a lot.
 * TODO: Use a similar mechanism to {@link ObservationsSyncPhaseController} to implement incremental
 * sync for orders.
 */
public class OrdersSyncPhaseController implements SyncPhaseController {
    private static final Logger LOG = Logger.create();

    @Override
    public void sync(ContentResolver contentResolver, SyncResult syncResult, ContentProviderClient providerClient) throws Throwable {
        ArrayList<ContentProviderOperation> ops = DbSyncHelper.getOrderUpdateOps(syncResult);
        providerClient.applyBatch(ops);
        LOG.i("Finished updating orders (" + ops.size() + " db ops)");
        contentResolver.notifyChange(Contracts.Orders.CONTENT_URI, null, false);
    }
}
