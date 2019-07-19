package org.projectbuendia.client.sync.controllers;

import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.SyncResult;

import com.android.volley.toolbox.RequestFuture;
import com.google.common.base.Joiner;

import org.projectbuendia.client.App;
import org.projectbuendia.client.json.JsonChart;
import org.projectbuendia.client.json.JsonChartItem;
import org.projectbuendia.client.json.JsonChartSection;
import org.projectbuendia.client.models.AppModel;
import org.projectbuendia.client.net.OpenMrsChartServer;
import org.projectbuendia.client.providers.Contracts;
import org.projectbuendia.client.utils.Logger;
import org.projectbuendia.client.utils.Utils;

import java.util.ArrayList;

/**
 * Handles syncing charts. Always fetches everything. This is okay because the full set of chart
 * layouts is usually fairly small in size.
 */
public class ChartsSyncPhaseRunnable implements SyncPhaseRunnable {
    private static final Logger LOG = Logger.create();

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
        providerClient.delete(Contracts.ChartItems.URI, null, null);
        syncResult.stats.numDeletes++;
        providerClient.applyBatch(getChartUpdateOps(chart, syncResult));
    }

    /** Converts a JsonChart response into appropriate inserts in the chart table. */
    private static ArrayList<ContentProviderOperation> getChartUpdateOps(
            JsonChart response, SyncResult syncResult) {
        if (response.uuid == null) {
            LOG.e("null chart uuid when fetching chart structure");
        }

        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        int nextId = 1;
        int nextWeight = 1;
        for (JsonChartSection section : response.sections) {
            int parentId = nextId;
            ops.add(ContentProviderOperation.newInsert(Contracts.ChartItems.URI)
                    .withValue("rowid", nextId++)
                    .withValue(Contracts.ChartItems.CHART_UUID, response.uuid)
                    .withValue(Contracts.ChartItems.WEIGHT, nextWeight++)
                    .withValue(Contracts.ChartItems.SECTION_TYPE, section.type != null ? section.type.name() : null)
                    .withValue(Contracts.ChartItems.LABEL, section.label)
                    .build());
            syncResult.stats.numInserts++;

            for (JsonChartItem item : section.items) {
                Object[] conceptUuids = new Object[item.concepts.length];
                for (int i = 0; i < conceptUuids.length; i++) {
                    conceptUuids[i] = Utils.expandUuid(item.concepts[i]);
                }
                ops.add(ContentProviderOperation.newInsert(Contracts.ChartItems.URI)
                        .withValue("rowid", nextId++)
                        .withValue(Contracts.ChartItems.CHART_UUID, response.uuid)
                        .withValue(Contracts.ChartItems.WEIGHT, nextWeight++)
                        .withValue(Contracts.ChartItems.PARENT_ROWID, parentId)
                        .withValue(Contracts.ChartItems.LABEL, item.label)
                        .withValue(Contracts.ChartItems.TYPE, item.type)
                        .withValue(Contracts.ChartItems.REQUIRED, item.required ? 1 : 0)
                        .withValue(Contracts.ChartItems.CONCEPT_UUIDS, Joiner.on(",").join(conceptUuids))
                        .withValue(Contracts.ChartItems.FORMAT, item.format)
                        .withValue(Contracts.ChartItems.CAPTION_FORMAT, item.caption_format)
                        .withValue(Contracts.ChartItems.CSS_CLASS, item.css_class)
                        .withValue(Contracts.ChartItems.CSS_STYLE, item.css_style)
                        .withValue(Contracts.ChartItems.SCRIPT, item.script)
                        .build());
                syncResult.stats.numInserts++;
            }
        }
        return ops;
    }
}
