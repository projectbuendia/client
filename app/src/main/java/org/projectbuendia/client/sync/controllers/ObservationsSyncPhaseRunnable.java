package org.projectbuendia.client.sync.controllers;

import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.net.Uri;
import android.os.RemoteException;

import com.android.volley.toolbox.RequestFuture;

import org.projectbuendia.client.App;
import org.projectbuendia.client.json.JsonObservation;
import org.projectbuendia.client.json.JsonObservationsResponse;
import org.projectbuendia.client.net.OpenMrsChartServer;
import org.projectbuendia.client.providers.Contracts;
import org.projectbuendia.client.providers.Contracts.Observations;
import org.projectbuendia.client.sync.SyncAdapter;
import org.projectbuendia.client.utils.Logger;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Handles syncing observations. Uses an incremental sync mechanism.
 */
// TODO: wrap this class in something that handles the core incremental sync behaviour.
public class ObservationsSyncPhaseRunnable implements SyncPhaseRunnable {

    /** RPC timeout for getting observations. */
    private static final int OBSERVATIONS_TIMEOUT_SECS = 180;
    private static final Logger LOG = Logger.create();

    @Override
    public void sync(ContentResolver contentResolver,
            SyncResult syncResult, ContentProviderClient providerClient)
            throws Throwable {
        String lastSyncToken =
                SyncAdapter.getLastSyncToken(providerClient, Contracts.Table.OBSERVATIONS);

        OpenMrsChartServer chartServer = new OpenMrsChartServer(App.getConnectionDetails());

        JsonObservationsResponse response;
        do {
            RequestFuture<JsonObservationsResponse> listFuture = RequestFuture.newFuture();
            chartServer.getIncrementalObservations(lastSyncToken, listFuture, listFuture);
            response = listFuture.get(OBSERVATIONS_TIMEOUT_SECS, TimeUnit.SECONDS);
            ArrayList<ContentProviderOperation> ops =
                    getObservationUpdateOps(response.results, syncResult);
            providerClient.applyBatch(ops);
            LOG.i("Updated page of observations (" + ops.size() + " db ops)");

            // Update the sync token
            lastSyncToken = response.syncToken;
        } while (response.more);

        // This is only safe transactionally if we can rely on the entire sync being transactional.
        SyncAdapter.storeSyncToken(
                providerClient, Contracts.Table.OBSERVATIONS, response.syncToken);

        // Remove all temporary observations now we have the real ones
        providerClient.delete(Observations.CONTENT_URI,
                Observations.UUID + " IS NULL",
                new String[0]);
    }

    /**
     * Updates observations, possibly incrementally.
     * NOTE: this logic relies upon observations inserts being an upsert.
     */
    private ArrayList<ContentProviderOperation> getObservationUpdateOps(
            JsonObservation[] obs, SyncResult syncResult)
            throws RemoteException, InterruptedException, ExecutionException, TimeoutException,
            OperationApplicationException {

        int deletes = 0;
        int inserts = 0;
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        for (JsonObservation observation: obs) {
            if (observation.voided) {
                Uri uri = Observations.CONTENT_URI.buildUpon().appendPath(observation.uuid).build();
                ops.add(ContentProviderOperation.newDelete(uri).build());
                deletes++;
            } else {
                ops.add(ContentProviderOperation.newInsert(Observations.CONTENT_URI)
                        .withValues(getObsValuesToInsert(observation)).build());
                inserts++;
            }
        }
        LOG.d("Observations processed! Inserts: %d, Deletes: %d", inserts, deletes);
        syncResult.stats.numInserts += inserts;
        syncResult.stats.numDeletes += deletes;
        return ops;
    }

    /** Converts an encounter data response into appropriate inserts in the encounters table. */
    public static ContentValues getObsValuesToInsert(
            JsonObservation observation) {
        ContentValues cvs = new ContentValues();
        cvs.put(Observations.UUID, observation.uuid);
        cvs.put(Observations.PATIENT_UUID, observation.patient_uuid);
        cvs.put(Observations.ENCOUNTER_UUID, observation.encounter_uuid);
        cvs.put(Observations.ENCOUNTER_MILLIS, observation.timestamp.getMillis());
        cvs.put(Observations.CONCEPT_UUID, observation.concept_uuid);
        cvs.put(Observations.VALUE, observation.value);

        return cvs;
    }
}
