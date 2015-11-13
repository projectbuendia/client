package org.projectbuendia.client.sync.controllers;

import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;

import com.android.volley.toolbox.RequestFuture;

import org.projectbuendia.client.App;
import org.projectbuendia.client.json.JsonOrder;
import org.projectbuendia.client.providers.Contracts.Orders;
import org.projectbuendia.client.utils.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

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
        ArrayList<ContentProviderOperation> ops = getOrderUpdateOps(syncResult);
        providerClient.applyBatch(ops);
        LOG.i("Finished updating orders (" + ops.size() + " db ops)");
        contentResolver.notifyChange(Orders.CONTENT_URI, null, false);
    }

    /**
     * Gets orders from the server and returns a list of operations that will
     * update the database with the new orders and edits to existing orders.
     */
    private static ArrayList<ContentProviderOperation> getOrderUpdateOps(SyncResult syncResult)
            throws ExecutionException, InterruptedException {
        // Request all orders from the server.
        RequestFuture<List<JsonOrder>> future = RequestFuture.newFuture();
        App.getServer().listOrders(future, future);
        Map<String, JsonOrder> ordersToStore = new HashMap<>();
        for (JsonOrder order : future.get()) {
            ordersToStore.put(order.uuid, order);
        }

        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        final ContentResolver resolver = App.getInstance().getContentResolver();
        Cursor c = resolver.query(Orders.CONTENT_URI, new String[]{
                Orders.UUID,
                Orders.PATIENT_UUID,
                Orders.INSTRUCTIONS,
                Orders.START_MILLIS,
                Orders.STOP_MILLIS
        }, null, null, null);
        try {
            LOG.i("Examining orders: %d local, %d from server.", c.getCount(), ordersToStore.size());
            // Scan all the locally stored orders, updating the orders we've just received.
            while (c.moveToNext()) {
                String uuid = c.getString(c.getColumnIndex(Orders.UUID));
                Uri uri = Orders.CONTENT_URI.buildUpon().appendPath(uuid).build();
                JsonOrder order = ordersToStore.get(uuid);
                if (order != null) {  // apply update to a local order
                    LOG.v("  - will update order " + uuid);
                    ops.add(ContentProviderOperation.newUpdate(uri)
                            .withValue(Orders.PATIENT_UUID, order.patient_uuid)
                            .withValue(Orders.INSTRUCTIONS, order.instructions)
                            .withValue(Orders.START_MILLIS, order.start_millis)
                            .withValue(Orders.STOP_MILLIS, order.stop_millis)
                            .build());
                    ordersToStore.remove(uuid);  // done with this incoming order
                    syncResult.stats.numUpdates++;
                } else {  // delete the local order (the server doesn't have it)
                    LOG.v("  - will delete order " + uuid);
                    ops.add(ContentProviderOperation.newDelete(uri).build());
                    syncResult.stats.numDeletes++;
                }
            }
        } finally {
            c.close();
        }

        // Store all the remaining received orders as new orders.
        for (JsonOrder order : ordersToStore.values()) {
            LOG.v("  - will insert order " + order.uuid);
            ops.add(ContentProviderOperation.newInsert(Orders.CONTENT_URI)
                    .withValue(Orders.UUID, order.uuid)
                    .withValue(Orders.PATIENT_UUID, order.patient_uuid)
                    .withValue(Orders.INSTRUCTIONS, order.instructions)
                    .withValue(Orders.START_MILLIS, order.start_millis)
                    .withValue(Orders.STOP_MILLIS, order.stop_millis)
                    .build());
            syncResult.stats.numInserts++;
        }
        return ops;
    }
}
