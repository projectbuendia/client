package org.projectbuendia.client.sync.controllers;

import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.SyncResult;
import android.net.Uri;

import org.projectbuendia.client.json.JsonOrder;
import org.projectbuendia.client.providers.Contracts;
import org.projectbuendia.client.providers.Contracts.Orders;
import org.projectbuendia.client.utils.Logger;

import java.util.ArrayList;

/**
 * Handles syncing orders. Currently we always fetch all orders. This won't scale; incremental
 * fetch would help a lot.
 */
public class OrdersSyncWorker extends IncrementalSyncWorker<JsonOrder> {
    private static final Logger LOG = Logger.create();

    public OrdersSyncWorker() {
        super("orders", Contracts.Table.ORDERS, JsonOrder.class);
    }

    @Override
    protected ArrayList<ContentProviderOperation> getUpdateOps(
            JsonOrder[] orders, SyncResult syncResult) {
        int numInserts = 0;
        int numDeletes = 0;
        ArrayList<ContentProviderOperation> ops = new ArrayList<>(orders.length);
        for (JsonOrder order : orders) {
            if (order.voided) {
                ops.add(deleteOrderWithUuid(order.uuid));
                numDeletes++;
            } else {
                ops.add(insertOrReplaceOrder(order));
                numInserts++;
            }
        }
        LOG.d("Orders: %d inserts, %d deletes", numInserts, numDeletes);
        syncResult.stats.numInserts += numInserts;
        syncResult.stats.numDeletes += numDeletes;
        return ops;
    }

    private static ContentProviderOperation insertOrReplaceOrder(JsonOrder order) {
        return ContentProviderOperation.newInsert(Orders.URI)
                .withValue(Orders.UUID, order.uuid)
                .withValue(Orders.PATIENT_UUID, order.patient_uuid)
                .withValue(Orders.INSTRUCTIONS, order.instructions)
                .withValue(Orders.START_MILLIS, order.start_millis)
                .withValue(Orders.STOP_MILLIS, order.stop_millis)
                .build();
    }

    private static ContentProviderOperation deleteOrderWithUuid(String uuid) {
        Uri uri = Orders.URI.buildUpon().appendPath(uuid).build();
        return ContentProviderOperation.newDelete(uri).build();
    }

    @Override public void finalize(
        ContentResolver resolver, SyncResult result, ContentProviderClient client) {
        if (result.stats.numInserts + result.stats.numDeletes > 0) {
            resolver.notifyChange(Orders.URI, null, false);
        }
    }
}
