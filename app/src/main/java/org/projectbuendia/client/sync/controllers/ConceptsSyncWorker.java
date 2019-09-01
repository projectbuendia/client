package org.projectbuendia.client.sync.controllers;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.SyncResult;

import com.android.volley.toolbox.RequestFuture;

import org.projectbuendia.client.App;
import org.projectbuendia.client.json.JsonConcept;
import org.projectbuendia.client.json.JsonConceptsResponse;
import org.projectbuendia.client.net.OpenMrsChartServer;
import org.projectbuendia.client.providers.Contracts;
import org.projectbuendia.client.utils.Logger;

import java.util.ArrayList;
import java.util.Map;

/**
 * Handles syncing concepts. All concepts are fetched every time. This is okay because concepts are
 * not a particularly huge data set.
 */
public class ConceptsSyncWorker implements SyncWorker {
    private static final Logger LOG = Logger.create();

    @Override public boolean sync(
        ContentResolver resolver, SyncResult result, ContentProviderClient client
    ) throws Throwable {
        OpenMrsChartServer chartServer = new OpenMrsChartServer(App.getConnectionDetails());
        RequestFuture<JsonConceptsResponse> future = RequestFuture.newFuture();
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
            result.stats.numInserts++;
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
                result.stats.numInserts++;
            }
        }
        client.bulkInsert(Contracts.Concepts.URI,
            conceptInserts.toArray(new ContentValues[conceptInserts.size()]));
        client.bulkInsert(Contracts.ConceptNames.URI,
            conceptNameInserts.toArray(new ContentValues[conceptNameInserts.size()]));
        return true;
    }

    @Override public void finalize(
        ContentResolver resolver, SyncResult result, ContentProviderClient client
    ) throws Throwable {
        App.getConceptService().invalidate();
    }
}
