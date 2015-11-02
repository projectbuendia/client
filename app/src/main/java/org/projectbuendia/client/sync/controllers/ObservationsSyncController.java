package org.projectbuendia.client.sync.controllers;

import android.content.ContentProvider;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.SyncResult;
import android.database.Cursor;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.TimingLogger;

import com.android.volley.toolbox.RequestFuture;

import org.joda.time.Instant;
import org.projectbuendia.client.App;
import org.projectbuendia.client.json.JsonEncounter;
import org.projectbuendia.client.json.JsonEncountersResponse;
import org.projectbuendia.client.models.AppModel;
import org.projectbuendia.client.net.OpenMrsChartServer;
import org.projectbuendia.client.providers.Contracts;
import org.projectbuendia.client.sync.DbSyncHelper;
import org.projectbuendia.client.utils.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Syncs observations from the server to the local database. Note that each sync is an incremental
 * operation; only records that have been updated are fetched and processed.
 */
public class ObservationsSyncController implements SyncController {
    private static final Logger LOG = Logger.create();

    /** RPC timeout for getting observations. */
    private static final int OBSERVATIONS_TIMEOUT_SECS = 180;

    @Override
    public void sync(
            SyncResult syncResult,
            ContentProviderClient providerClient,
            ContentResolver contentResolver) throws Throwable {
        OpenMrsChartServer chartServer = new OpenMrsChartServer(App.getConnectionDetails());
        // Get the charts asynchronously using volley.
        RequestFuture<JsonEncountersResponse> listFuture = RequestFuture.newFuture();

        TimingLogger timingLogger = new TimingLogger(LOG.tag, "obs update");
        Instant lastSyncTime = getLastObservationSyncTime(providerClient);
        Instant newSyncTime = updateObservations(
                lastSyncTime, providerClient, syncResult, chartServer, listFuture, timingLogger);
        // This is only safe transactionally if we can rely on the entire sync being transactional.
        storeLastSyncTime(providerClient, newSyncTime);

        // Remove all temporary observations now we have the real ones
        providerClient.delete(Contracts.Observations.CONTENT_URI,
                Contracts.Observations.TEMP_CACHE + "!=0",
                new String[0]);
        timingLogger.addSplit("delete temp observations");
        timingLogger.dumpToLog();
    }

    /**
     * Updates observations, possibly incrementally.
     * NOTE: this logic relies upon observations inserts being an upsert.
     */
    private Instant updateObservations(
            @Nullable Instant lastSyncTime, ContentProviderClient provider, SyncResult syncResult,
            OpenMrsChartServer chartServer, RequestFuture<JsonEncountersResponse> listFuture,
            TimingLogger timingLogger)
            throws RemoteException, InterruptedException, ExecutionException, TimeoutException {
        LOG.d("requesting incremental encounters");
        chartServer.getIncrementalEncounters(lastSyncTime, listFuture, listFuture);
        ArrayList<ContentValues> toInsert = new ArrayList<>();
        LOG.d("awaiting parsed incremental response");
        final JsonEncountersResponse response =
                listFuture.get(OBSERVATIONS_TIMEOUT_SECS, TimeUnit.SECONDS);
        LOG.d("got incremental response");
        timingLogger.addSplit("Fetched incremental encounters RPC");
        for (JsonEncounter record : response.results) {
            // Validate the encounter
            if (!isEncounterValid(record)) {
                LOG.e("Invalid encounter data received from server; dropping encounter.");
                continue;
            }
            // Add the observations from the encounter.
            toInsert.addAll(getObsValuesToInsert(record, syncResult));
            timingLogger.addSplit("added incremental obs to list");
        }
        timingLogger.addSplit("making operations");
        if (toInsert.size() > 0) {
            provider.bulkInsert(Contracts.Observations.CONTENT_URI,
                    toInsert.toArray(new ContentValues[toInsert.size()]));
            timingLogger.addSplit("bulk inserts");
        }
        return Instant.parse(response.snapshotTime);
    }

    private void storeLastSyncTime(ContentProviderClient providerClient, Instant newSyncTime)
            throws RemoteException {
        ContentValues cv = new ContentValues();
        cv.put(Contracts.Misc.OBS_SYNC_END_MILLIS, newSyncTime.getMillis());
        providerClient.insert(Contracts.Misc.CONTENT_URI, cv);
    }

    /** returns {@code true} if the encounter is valid. */
    private boolean isEncounterValid(JsonEncounter record) {
        return record.patient_uuid != null
                && record.uuid != null
                && record.timestamp != null;
    }

    /** Returns the server timestamp corresponding to the last observation sync. */
    @Nullable
    private Instant getLastObservationSyncTime(ContentProviderClient providerClient)
            throws RemoteException {
        Cursor c = null;
        try {
            c = providerClient.query(
                    Contracts.Misc.CONTENT_URI,
                    new String[] {Contracts.Misc.OBS_SYNC_END_MILLIS}, null, null, null);
            // Make the linter happy, there's no way that the cursor can be null without throwing
            // an exception.
            assert c != null;
            if (c.moveToNext()) {
                if (c.isNull(0)) {
                    return null;
                }
                // c.getLong will return 0 as a default value if the column has nothing in it, so
                // we explicitly do a null-check beforehand.
                return new Instant(c.getLong(0));
            } else {
                return null;
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    /** Converts an encounter data response into appropriate inserts in the encounters table. */
    private static List<ContentValues> getObsValuesToInsert(
            JsonEncounter encounter, SyncResult syncResult) {
        List<ContentValues> cvs = new ArrayList<>();
        ContentValues base = new ContentValues();
        base.put(Contracts.Observations.PATIENT_UUID, encounter.patient_uuid);
        base.put(Contracts.Observations.ENCOUNTER_UUID, encounter.uuid);
        base.put(Contracts.Observations.ENCOUNTER_MILLIS, encounter.timestamp.getMillis());

        if (encounter.observations != null) {
            for (Map.Entry<Object, Object> entry : encounter.observations.entrySet()) {
                final String conceptUuid = (String) entry.getKey();
                ContentValues values = new ContentValues(base);
                values.put(Contracts.Observations.CONCEPT_UUID, conceptUuid);
                values.put(Contracts.Observations.VALUE, entry.getValue().toString());
                cvs.add(values);
                syncResult.stats.numInserts++;
            }
        }
        if (encounter.order_uuids != null) {
            for (String orderUuid : encounter.order_uuids) {
                ContentValues values = new ContentValues(base);
                values.put(Contracts.Observations.CONCEPT_UUID, AppModel.ORDER_EXECUTED_CONCEPT_UUID);
                values.put(Contracts.Observations.VALUE, orderUuid);
                cvs.add(values);
                syncResult.stats.numInserts++;
            }
        }
        return cvs;
    }
}
