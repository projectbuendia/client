package org.projectbuendia.client.sync.controllers;

import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.SyncResult;
import android.net.Uri;
import android.os.RemoteException;

import com.android.volley.toolbox.RequestFuture;

import org.projectbuendia.client.App;
import org.projectbuendia.client.json.JsonOrder;
import org.projectbuendia.client.json.JsonOrdersResponse;
import org.projectbuendia.client.providers.Contracts;
import org.projectbuendia.client.providers.Contracts.Orders;
import org.projectbuendia.client.sync.SyncAdapter;
import org.projectbuendia.client.utils.Logger;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

/**
 * Handles syncing orders. Currently we always fetch all orders. This won't scale; incremental
 * fetch would help a lot.
 * TODO: Use a similar mechanism to {@link ObservationsSyncPhaseRunnable} to implement incremental
 * sync for orders.
 */
public class OrdersSyncPhaseRunnable implements SyncPhaseRunnable {
    private static final Logger LOG = Logger.create();

    @Override
    public void sync(
            ContentResolver contentResolver,
            SyncResult syncResult,
            ContentProviderClient providerClient)
            throws Throwable {

        String lastSyncToken = SyncAdapter.getLastSyncToken(providerClient, Contracts.Table.ORDERS);

        // Request orders from the server.
        RequestFuture<JsonOrdersResponse> future = RequestFuture.newFuture();
        App.getServer().listOrders(lastSyncToken, future, future);
        JsonOrdersResponse ordersResponse = future.get();
        LOG.d("Waiting for orders response.");
        ArrayList<ContentProviderOperation> ops =
                getOrderUpdateOps(syncResult, ordersResponse.results);
        providerClient.applyBatch(ops);
        LOG.d("Finished updating orders (%d db ops)", ops.size());

        SyncAdapter.storeSyncToken(
                providerClient, Contracts.Table.ORDERS, ordersResponse.snapshotTime);

        contentResolver.notifyChange(Orders.CONTENT_URI, null, false);
    }

    /**
     * Gets orders from the server and returns a list of operations that will
     * update the database with the new orders and edits to existing orders.
     */
    private static ArrayList<ContentProviderOperation> getOrderUpdateOps(
            SyncResult syncResult, JsonOrder[] orders)
            throws ExecutionException, InterruptedException, RemoteException {

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
        LOG.d("Finished computing DB ops for orders." +
                " Inserting/Replacing %d records, Deleting %d records",
                numInserts,
                numDeletes);

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
}
