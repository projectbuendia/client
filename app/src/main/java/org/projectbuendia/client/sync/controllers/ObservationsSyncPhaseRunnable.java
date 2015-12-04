package org.projectbuendia.client.sync.controllers;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.SyncResult;
import android.net.Uri;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.TimingLogger;

import com.android.volley.toolbox.RequestFuture;

import org.projectbuendia.client.App;
import org.projectbuendia.client.json.JsonObservation;
import org.projectbuendia.client.json.JsonObservationsResponse;
import org.projectbuendia.client.net.OpenMrsChartServer;
import org.projectbuendia.client.providers.Contracts;
import org.projectbuendia.client.providers.Contracts.Observations;
import org.projectbuendia.client.sync.SyncAdapter;
import org.projectbuendia.client.utils.Logger;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.lang.String.format;

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
        OpenMrsChartServer chartServer = new OpenMrsChartServer(App.getConnectionDetails());
        // Get the charts asynchronously using volley.
        RequestFuture<JsonObservationsResponse> listFuture = RequestFuture.newFuture();

        TimingLogger timingLogger = new TimingLogger(LOG.tag, "obs update");
        String lastSyncToken =
                SyncAdapter.getLastSyncToken(providerClient, Contracts.Table.OBSERVATIONS);
        String newSyncToken = updateObservations(
                lastSyncToken, providerClient, syncResult, chartServer, listFuture, timingLogger);
        timingLogger.addSplit("finished observation update");
        // This is only safe transactionally if we can rely on the entire sync being transactional.
        SyncAdapter.storeSyncToken(providerClient, Contracts.Table.OBSERVATIONS, newSyncToken);

        // Remove all temporary observations now we have the real ones
        providerClient.delete(Observations.CONTENT_URI,
                format("%s IS NULL AND %s == 1", Observations.UUID, Observations.SUBMITTED),
                new String[0]);
        timingLogger.addSplit("delete temp observations");
        timingLogger.dumpToLog();
    }

    /**
     * Updates observations, possibly incrementally.
     * NOTE: this logic relies upon observations inserts being an upsert.
     */
    private String updateObservations(
            @Nullable String lastSyncToken, ContentProviderClient provider, SyncResult syncResult,
            OpenMrsChartServer chartServer, RequestFuture<JsonObservationsResponse> listFuture,
            TimingLogger timingLogger)
            throws RemoteException, InterruptedException, ExecutionException, TimeoutException {
        LOG.d("Requesting Observations");
        chartServer.getIncrementalObservations(lastSyncToken, listFuture, listFuture);
        LOG.d("Awaiting parsed observations response");
        final JsonObservationsResponse response =
                listFuture.get(OBSERVATIONS_TIMEOUT_SECS, TimeUnit.SECONDS);
        LOG.d("Got JSON Observations response");
        timingLogger.addSplit("Fetched incremental encounters RPC");
        int deletes = 0;
        int inserts = 0;
        for (JsonObservation observation: response.results) {
            if (observation.voided) {
                Uri uri = Observations.CONTENT_URI.buildUpon().appendPath(observation.uuid).build();
                provider.delete(uri, null, null);
                deletes++;
            } else {
                provider.insert(Observations.CONTENT_URI, getObsValuesToInsert(observation));
                inserts++;
            }
            // Add the observations from the encounter.
            timingLogger.addSplit("added incremental obs to list");
        }
        LOG.d("Observations done! Inserts: %d, Deletes: %d", inserts, deletes);
        syncResult.stats.numInserts += inserts;
        syncResult.stats.numDeletes += deletes;
        return response.snapshotTime;
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
