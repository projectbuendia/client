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
import org.projectbuendia.client.providers.Contracts;
import org.projectbuendia.client.sync.DbSyncHelper;
import org.projectbuendia.client.utils.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Syncs orders from the server to the local database.
 */
public class OrdersSyncController implements SyncController {
    private static final Logger LOG = Logger.create();

    @Override
    public void sync(
            SyncResult syncResult,
            ContentProviderClient providerClient,
            ContentResolver contentResolver) throws Throwable {
        ArrayList<ContentProviderOperation> ops = getOrderUpdateOps(syncResult);
        contentResolver.applyBatch(Contracts.CONTENT_AUTHORITY, ops);
        LOG.i("Finished updating orders (" + ops.size() + " db ops)");
        contentResolver.notifyChange(Contracts.Orders.CONTENT_URI, null, false);
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
        Cursor c = resolver.query(Contracts.Orders.CONTENT_URI, new String[]{
                Contracts.Orders.UUID,
                Contracts.Orders.PATIENT_UUID,
                Contracts.Orders.INSTRUCTIONS,
                Contracts.Orders.START_MILLIS,
                Contracts.Orders.STOP_MILLIS
        }, null, null, null);
        try {
            LOG.i("Examining orders: %d local, %d from server.", c.getCount(), ordersToStore.size());
            // Scan all the locally stored orders, updating the orders we've just received.
            while (c.moveToNext()) {
                String uuid = c.getString(c.getColumnIndex(Contracts.Orders.UUID));
                Uri uri = Contracts.Orders.CONTENT_URI.buildUpon().appendPath(uuid).build();
                JsonOrder order = ordersToStore.get(uuid);
                if (order != null) {  // apply update to a local order
                    LOG.v("  - will update order " + uuid);
                    ops.add(ContentProviderOperation.newUpdate(uri)
                            .withValue(Contracts.Orders.PATIENT_UUID, order.patient_uuid)
                            .withValue(Contracts.Orders.INSTRUCTIONS, order.instructions)
                            .withValue(Contracts.Orders.START_MILLIS, order.start_millis)
                            .withValue(Contracts.Orders.STOP_MILLIS, order.stop_millis)
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
            ops.add(ContentProviderOperation.newInsert(Contracts.Orders.CONTENT_URI)
                    .withValue(Contracts.Orders.UUID, order.uuid)
                    .withValue(Contracts.Orders.PATIENT_UUID, order.patient_uuid)
                    .withValue(Contracts.Orders.INSTRUCTIONS, order.instructions)
                    .withValue(Contracts.Orders.START_MILLIS, order.start_millis)
                    .withValue(Contracts.Orders.STOP_MILLIS, order.stop_millis)
                    .build());
            syncResult.stats.numInserts++;
        }
        return ops;
    }
}
