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
import org.projectbuendia.client.net.OpenMrsServer;
import org.projectbuendia.client.providers.Contracts;
import org.projectbuendia.client.utils.Logger;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/** Syncs concepts.  All concepts are fetched every time. */
public class ConceptsSyncWorker implements SyncWorker {
    private static final Logger LOG = Logger.create();

    @Override public boolean sync(
        ContentResolver resolver, SyncResult result, ContentProviderClient client
    ) throws Throwable {
        OpenMrsChartServer chartServer = new OpenMrsChartServer(App.getConnectionDetails());
        RequestFuture<JsonConceptsResponse> future = RequestFuture.newFuture();
        chartServer.getConcepts(future, future); // errors handled by caller
        ArrayList<ContentValues> conceptInserts = new ArrayList<>();
        for (JsonConcept concept : future.get(OpenMrsServer.TIMEOUT_SECONDS, TimeUnit.SECONDS).results) {
            // This is safe because the ContentProvider implements insert as replace.
            ContentValues conceptInsert = new ContentValues();
            conceptInsert.put(Contracts.Concepts.UUID, concept.uuid);
            conceptInsert.put(Contracts.Concepts.XFORM_ID, concept.xform_id);
            conceptInsert.put(Contracts.Concepts.TYPE, concept.type.name());
            conceptInsert.put(Contracts.Concepts.NAME, concept.name);
            conceptInserts.add(conceptInsert);
            result.stats.numInserts++;
        }
        client.bulkInsert(Contracts.Concepts.URI,
            conceptInserts.toArray(new ContentValues[conceptInserts.size()]));
        return true;
    }

    @Override public void finalize(
        ContentResolver resolver, SyncResult result, ContentProviderClient client
    ) throws Throwable {
        App.getConceptService().invalidate();
    }
}
