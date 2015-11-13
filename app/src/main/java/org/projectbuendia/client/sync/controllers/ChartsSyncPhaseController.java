package org.projectbuendia.client.sync.controllers;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.SyncResult;

import com.android.volley.toolbox.RequestFuture;

import org.projectbuendia.client.App;
import org.projectbuendia.client.json.JsonChart;
import org.projectbuendia.client.models.AppModel;
import org.projectbuendia.client.net.OpenMrsChartServer;
import org.projectbuendia.client.providers.Contracts;
import org.projectbuendia.client.sync.DbSyncHelper;

/**
 * Handles syncing charts. Always fetches everything. This is okay because the full set of chart
 * layouts is usually fairly small in size.
 */
public class ChartsSyncPhaseController implements SyncPhaseController {
    @Override
    public void sync(ContentResolver contentResolver, SyncResult syncResult,
            ContentProviderClient providerClient)
            throws Throwable {
        OpenMrsChartServer chartServer = new OpenMrsChartServer(App.getConnectionDetails());
        RequestFuture<JsonChart> future = RequestFuture.newFuture();
        // errors handled by caller
        chartServer.getChartStructure(AppModel.CHART_UUID, future, future);
        final JsonChart chart = future.get();

        // When we do a chart update, delete everything first, then insert all the new rows.
        providerClient.delete(Contracts.ChartItems.CONTENT_URI, null, null);
        syncResult.stats.numDeletes++;
        providerClient.applyBatch(DbSyncHelper.getChartUpdateOps(chart, syncResult));
    }
}
