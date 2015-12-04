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
public class OrdersSyncPhaseRunnable extends IncrementalSyncPhaseRunnable<JsonOrder> {
    private static final Logger LOG = Logger.create();

    public OrdersSyncPhaseRunnable() {
        super(
                "orders",
                Contracts.Table.ORDERS,
                JsonOrder.class);
    }

    @Override
    protected ArrayList<ContentProviderOperation> getUpdateOps(
            JsonOrder[] orders, SyncResult syncResult) {
        int numDeletes = 0;
        int numInserts = 0;
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
        syncResult.stats.numDeletes += numDeletes;
        syncResult.stats.numInserts += numInserts;
        LOG.d("Orders processed! Inserts: %d, Deletes: %d", numInserts, numDeletes);
        return ops;
    }

    private static ContentProviderOperation insertOrReplaceOrder(JsonOrder order) {
        return ContentProviderOperation.newInsert(Orders.CONTENT_URI)
                .withValue(Orders.UUID, order.uuid)
                .withValue(Orders.PATIENT_UUID, order.patient_uuid)
                .withValue(Orders.INSTRUCTIONS, order.instructions)
                .withValue(Orders.START_MILLIS, order.start_millis)
                .withValue(Orders.STOP_MILLIS, order.stop_millis)
                .build();
    }

    private static ContentProviderOperation deleteOrderWithUuid(String uuid) {
        Uri uri = Orders.CONTENT_URI.buildUpon().appendPath(uuid).build();
        return ContentProviderOperation.newDelete(uri).build();
    }

    @Override
    protected void afterSyncFinished(
            ContentResolver contentResolver,
            SyncResult syncResult,
            ContentProviderClient providerClient) throws Throwable {
        contentResolver.notifyChange(Orders.CONTENT_URI, null, false);
    }
}
