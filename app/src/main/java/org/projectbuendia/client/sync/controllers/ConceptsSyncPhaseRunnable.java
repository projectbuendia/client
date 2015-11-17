package org.projectbuendia.client.sync.controllers;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.SyncResult;

import com.android.volley.toolbox.RequestFuture;

import org.projectbuendia.client.App;
import org.projectbuendia.client.json.JsonConcept;
import org.projectbuendia.client.json.JsonConceptResponse;
import org.projectbuendia.client.net.OpenMrsChartServer;
import org.projectbuendia.client.providers.Contracts;
import org.projectbuendia.client.sync.ChartDataHelper;
import org.projectbuendia.client.utils.Logger;

import java.util.ArrayList;
import java.util.Map;

/**
 * Handles syncing concepts. All concepts are fetched everytime. This is okay because concepts are
 * not a particularly huge data set.
 */
public class ConceptsSyncPhaseRunnable implements SyncPhaseRunnable {
    private static final Logger LOG = Logger.create();

    @Override
    public void sync(ContentResolver contentResolver, SyncResult syncResult,
            ContentProviderClient providerClient)
            throws Throwable {
        OpenMrsChartServer chartServer = new OpenMrsChartServer(App.getConnectionDetails());
        RequestFuture<JsonConceptResponse> future = RequestFuture.newFuture();
        chartServer.getConcepts(future, future); // errors handled by caller
        ArrayList<ContentValues> conceptInserts = new ArrayList<>();
        ArrayList<ContentValues> conceptNameInserts = new ArrayList<>();
        for (JsonConcept concept : future.get().results) {
            // This is safe because we have implemented insert on the content provider
            // with replace.
            ContentValues conceptInsert = new ContentValues();
            conceptInsert.put(Contracts.Concepts.UUID, concept.uuid);
            conceptInsert.put(Contracts.Concepts.XFORM_ID, concept.xform_id);
            conceptInsert.put(Contracts.Concepts.CONCEPT_TYPE, concept.type.name());
            conceptInserts.add(conceptInsert);
            syncResult.stats.numInserts++;
            for (Map.Entry<String, String> entry : concept.names.entrySet()) {
                String locale = entry.getKey();
                if (locale == null) {
                    LOG.e("null locale in concept name rpc for " + concept);
                    continue;
                }
                String name = entry.getValue();
                if (name == null) {
                    LOG.e("null name in concept name rpc for " + concept);
                    continue;
                }
                ContentValues conceptNameInsert = new ContentValues();
                conceptNameInsert.put(Contracts.ConceptNames.CONCEPT_UUID, concept.uuid);
                conceptNameInsert.put(Contracts.ConceptNames.LOCALE, locale);
                conceptNameInsert.put(Contracts.ConceptNames.NAME, name);
                conceptNameInserts.add(conceptNameInsert);
                syncResult.stats.numInserts++;
            }
        }
        providerClient.bulkInsert(Contracts.Concepts.CONTENT_URI,
                conceptInserts.toArray(new ContentValues[conceptInserts.size()]));
        providerClient.bulkInsert(Contracts.ConceptNames.CONTENT_URI,
                conceptNameInserts.toArray(new ContentValues[conceptNameInserts.size()]));

        ChartDataHelper.invalidateLoadedConceptData();
    }
}
