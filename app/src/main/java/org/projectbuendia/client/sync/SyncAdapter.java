// Copyright 2015 The Project Buendia Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at: http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distrib-
// uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
// specific language governing permissions and limitations under the License.

package org.projectbuendia.client.sync;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.database.Cursor;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.TimingLogger;

import com.android.volley.toolbox.RequestFuture;

import org.joda.time.Instant;
import org.projectbuendia.client.App;
import org.projectbuendia.client.R;
import org.projectbuendia.client.json.JsonEncounter;
import org.projectbuendia.client.models.AppModel;
import org.projectbuendia.client.net.OpenMrsChartServer;
import org.projectbuendia.client.json.JsonChart;
import org.projectbuendia.client.json.JsonConcept;
import org.projectbuendia.client.json.JsonConceptResponse;
import org.projectbuendia.client.json.JsonEncountersResponse;
import org.projectbuendia.client.providers.BuendiaProvider;
import org.projectbuendia.client.providers.Contracts;
import org.projectbuendia.client.providers.Contracts.ChartItems;
import org.projectbuendia.client.providers.Contracts.ConceptNames;
import org.projectbuendia.client.providers.Contracts.Concepts;
import org.projectbuendia.client.providers.Contracts.LocationNames;
import org.projectbuendia.client.providers.Contracts.Locations;
import org.projectbuendia.client.providers.Contracts.Misc;
import org.projectbuendia.client.providers.Contracts.Observations;
import org.projectbuendia.client.providers.Contracts.Orders;
import org.projectbuendia.client.providers.Contracts.Patients;
import org.projectbuendia.client.providers.SQLiteDatabaseTransactionHelper;
import org.projectbuendia.client.user.UserManager;
import org.projectbuendia.client.utils.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Global sync adapter for syncing all client side database caches. */
public class SyncAdapter extends AbstractThreadedSyncAdapter {

    private static final Logger LOG = Logger.create();
    /** RPC timeout for getting observations. */
    private static final int OBSERVATIONS_TIMEOUT_SECS = 180;
    /** Named used during the sync process for SQL savepoints. */
    private static final String SYNC_SAVEPOINT_NAME = "SYNC_SAVEPOINT";

    /** UI messages to show while each phase of the sync is in progress. */
    private static final Map<SyncPhase, Integer> PHASE_MESSAGES = new HashMap<>();

    static {
        PHASE_MESSAGES.put(SyncPhase.SYNC_USERS, R.string.syncing_users);
        PHASE_MESSAGES.put(SyncPhase.SYNC_LOCATIONS, R.string.syncing_locations);
        PHASE_MESSAGES.put(SyncPhase.SYNC_CHART_ITEMS, R.string.syncing_charts);
        PHASE_MESSAGES.put(SyncPhase.SYNC_CONCEPTS, R.string.syncing_concepts);
        PHASE_MESSAGES.put(SyncPhase.SYNC_PATIENTS, R.string.syncing_patients);
        PHASE_MESSAGES.put(SyncPhase.SYNC_OBSERVATIONS, R.string.syncing_observations);
        PHASE_MESSAGES.put(SyncPhase.SYNC_ORDERS, R.string.syncing_orders);
        PHASE_MESSAGES.put(SyncPhase.SYNC_FORMS, R.string.syncing_forms);
    }

    /** Content resolver, for performing database operations. */
    private final ContentResolver mContentResolver;
    /** Tracks whether the sync has been canceled. */
    private boolean mIsSyncCanceled = false;

    /**
     * Keys in the extras bundle used to select which sync phases to do.
     * Select a phase by setting a boolean value of true for the appropriate key.
     */
    public enum SyncPhase {
        SYNC_USERS,
        SYNC_LOCATIONS,
        SYNC_CHART_ITEMS,
        SYNC_CONCEPTS,
        SYNC_PATIENTS,
        SYNC_OBSERVATIONS,
        SYNC_ORDERS,
        SYNC_FORMS
    }

    public enum SyncOption {
        /**
         * If this key is present with boolean value true, then the starting
         * and ending times of the entire sync operation will be recorded (as a
         * way of recording whether a full sync has ever successfully completed).
         */
        FULL_SYNC
    }

    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        mContentResolver = context.getContentResolver();
    }

    public SyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
        mContentResolver = context.getContentResolver();
    }

    @Override public void onSyncCanceled() {
        mIsSyncCanceled = true;
        LOG.i("Detecting a sync cancellation, canceling sync soon.");
    }

    /** Not thread-safe but, by default, this will never be called multiple times in parallel. */
    @Override public void onPerformSync(
        Account account,
        Bundle extras,
        String authority,
        ContentProviderClient provider,
        SyncResult syncResult) {
        // Broadcast that sync is starting.
        Intent syncStartedIntent =
            new Intent(getContext(), SyncManager.SyncStatusBroadcastReceiver.class);
        syncStartedIntent.putExtra(SyncManager.SYNC_STATUS, SyncManager.STARTED);
        getContext().sendBroadcast(syncStartedIntent);

        Intent syncFailedIntent =
            new Intent(getContext(), SyncManager.SyncStatusBroadcastReceiver.class);
        syncFailedIntent.putExtra(SyncManager.SYNC_STATUS, SyncManager.FAILED);

        Intent syncCanceledIntent =
            new Intent(getContext(), SyncManager.SyncStatusBroadcastReceiver.class);
        syncCanceledIntent.putExtra(SyncManager.SYNC_STATUS, SyncManager.CANCELED);

        // If we can't access the Buendia API, short-circuit. Before this check was added, sync
        // would occasionally hang indefinitely when wifi is unavailable. As a side effect of this
        // change, however, any user-requested sync will instantly fail until the HealthMonitor has
        // made a determination that the server is definitely accessible.
        if (App.getInstance().getHealthMonitor().isApiUnavailable()) {
            LOG.e("Abort sync: Buendia API is unavailable.");
            getContext().sendBroadcast(syncFailedIntent);
            return;
        }

        try {
            checkCancellation("before work started");
        } catch (CancellationException e) {
            getContext().sendBroadcast(syncCanceledIntent);
            return;
        }

        // Decide which phases to do.  If FULL_SYNC is set or no phases
        // are specified, do them all.
        Set<SyncPhase> phases = new HashSet<>();
        for (SyncPhase phase : SyncPhase.values()) {
            if (extras.getBoolean(phase.name())) {
                phases.add(phase);
            }
        }
        if (phases.isEmpty() || extras.getBoolean(SyncOption.FULL_SYNC.name())) {
            Collections.addAll(phases, SyncPhase.values());
        }

        LOG.i("Requested phases are: %s", phases);
        reportProgress(0, R.string.sync_in_progress);

        BuendiaProvider buendiaProvider =
            (BuendiaProvider) (provider.getLocalContentProvider());
        SQLiteDatabaseTransactionHelper dbTransactionHelper =
            buendiaProvider.getDbTransactionHelper();
        LOG.i("Setting savepoint %s", SYNC_SAVEPOINT_NAME);
        dbTransactionHelper.startNamedTransaction(SYNC_SAVEPOINT_NAME);

        TimingLogger timings = new TimingLogger(LOG.tag, "onPerformSync");

        try {
            if (extras.getBoolean(SyncOption.FULL_SYNC.name())) {
                Instant syncStartTime = Instant.now();
                LOG.i("Recording full sync start time: " + syncStartTime);
                storeFullSyncStartTime(provider, syncStartTime);
            }

            int progressIncrement = 100/phases.size();
            for (SyncPhase phase : SyncPhase.values()) {
                if (!phases.contains(phase)) continue;
                checkCancellation("before " + phase);
                LOG.i("--- Begin %s ---", phase);
                reportProgress(0, PHASE_MESSAGES.get(phase));

                switch (phase) {
                    // Users: Always fetch all users.  This is okay because new users
                    // aren't added all that often and the set of users is fairly small.
                    case SYNC_USERS:
                        updateUsers(provider, syncResult);
                        break;

                    // Locations: Always fetch everything.  This is okay because
                    // profiles (and hence locations) change infrequently.
                    case SYNC_LOCATIONS:
                        updateLocations(provider, syncResult);
                        break;

                    // Chart layouts: Always fetch everything.  This is okay because
                    // profiles (and hence chart layouts) change infrequently.
                    case SYNC_CHART_ITEMS:
                        updateChartItems(provider, syncResult);
                        break;

                    // Concepts: Always fetch all concepts.  This is okay because
                    // profiles (and hence form definitions) change infrequently.
                    case SYNC_CONCEPTS:
                        updateConcepts(provider, syncResult);
                        break;

                    // Patients: Always fetch all patients.  This won't scale;
                    // incremental fetch would help a lot.
                    // TODO: Implement incremental fetch for patients.
                    case SYNC_PATIENTS:
                        updatePatients(syncResult);
                        break;

                    // Observations: Incremental fetch and full fetch are the same process.
                    case SYNC_OBSERVATIONS:
                        updateObservations(provider, syncResult);
                        break;

                    // Orders: Currently we always fetch all orders.  This won't scale;
                    // incremental fetch would help a lot.  If we link orders to
                    // encounters then we can use a common incremental sync
                    // mechanism observations and orders.
                    // TODO: Store and retrieve orders as associated with encounters.
                    case SYNC_ORDERS:
                        updateOrders(provider, syncResult);
                        break;

                    // Forms: We always fetch all forms.  This is okay because
                    // there are only a few forms, usually less than 10.
                    case SYNC_FORMS:
                        updateForms(provider, syncResult);
                        break;
                }
                timings.addSplit(phase.name() + " phase completed");
                reportProgress(progressIncrement, PHASE_MESSAGES.get(phase));
            }
            reportProgress(100, R.string.completing_sync);

            if (extras.getBoolean(SyncOption.FULL_SYNC.name())) {
                Instant syncEndTime = Instant.now();
                LOG.i("Recording full sync end time: " + syncEndTime);
                storeFullSyncEndTime(provider, syncEndTime);
            }
        } catch (CancellationException e) {
            rollbackSavepoint(dbTransactionHelper);
            // Reset canceled state so that it doesn't interfere with next sync.
            LOG.e(e, "Sync canceled");
            getContext().sendBroadcast(syncCanceledIntent);
            return;
        } catch (OperationApplicationException e) {
            rollbackSavepoint(dbTransactionHelper);
            LOG.e(e, "Error updating database during sync");
            syncResult.databaseError = true;
            getContext().sendBroadcast(syncFailedIntent);
            return;
        } catch (Throwable e) {
            rollbackSavepoint(dbTransactionHelper);
            LOG.e(e, "Error during sync");
            syncResult.stats.numIoExceptions++;
            getContext().sendBroadcast(syncFailedIntent);
            return;
        } finally {
            LOG.i("Releasing savepoint %s", SYNC_SAVEPOINT_NAME);
            dbTransactionHelper.releaseNamedTransaction(SYNC_SAVEPOINT_NAME);
            dbTransactionHelper.close();
        }
        timings.dumpToLog();

        // Fire a broadcast indicating that sync has completed.
        Intent syncCompletedIntent =
            new Intent(getContext(), SyncManager.SyncStatusBroadcastReceiver.class);
        syncCompletedIntent.putExtra(SyncManager.SYNC_STATUS, SyncManager.COMPLETED);
        getContext().sendBroadcast(syncCompletedIntent);
    }

    /**
     * Enforces sync cancellation, throwing a {@link CancellationException} if the sync has been
     * canceled. It is the responsibility of the caller to perform any actual cancellation
     * procedures.
     */
    private synchronized void checkCancellation(String when) throws CancellationException {
        if (mIsSyncCanceled) {
            mIsSyncCanceled = false;
            throw new CancellationException("Sync cancelled " + when);
        }
    }

    private void reportProgress(int progressIncrement, int stringResource) {
        reportProgress(progressIncrement, getContext().getResources().getString(stringResource));
    }

    private void storeFullSyncStartTime(ContentProviderClient provider, Instant syncStartTime)
        throws RemoteException {
        ContentValues cv = new ContentValues();
        cv.put(Misc.FULL_SYNC_START_MILLIS, syncStartTime.getMillis());
        provider.insert(Misc.CONTENT_URI, cv);
    }

    private void updateUsers(final ContentProviderClient provider, SyncResult syncResult)
        throws InterruptedException, ExecutionException, RemoteException,
        OperationApplicationException, UserManager.UserSyncException {
        App.getUserManager().syncKnownUsersSynchronously();
    }

    private void updateLocations(final ContentProviderClient provider, SyncResult syncResult)
        throws InterruptedException, ExecutionException, RemoteException,
        OperationApplicationException {
        ArrayList<ContentProviderOperation> ops = DbSyncHelper.getLocationUpdateOps(syncResult);
        checkCancellation("before applying location updates");
        LOG.i("Applying batch update of locations.");
        mContentResolver.applyBatch(Contracts.CONTENT_AUTHORITY, ops);
        mContentResolver.notifyChange(Locations.CONTENT_URI, null, false);
        mContentResolver.notifyChange(LocationNames.CONTENT_URI, null, false);
    }

    private void updateChartItems(final ContentProviderClient provider, SyncResult syncResult)
        throws InterruptedException, ExecutionException, RemoteException,
        OperationApplicationException {
        OpenMrsChartServer chartServer = new OpenMrsChartServer(App.getConnectionDetails());
        RequestFuture<JsonChart> future = RequestFuture.newFuture();
        chartServer.getChartStructure(AppModel.CHART_UUID, future, future); // errors handled by caller
        final JsonChart chart = future.get();

        // When we do a chart update, delete everything first, then insert all the new rows.
        checkCancellation("before applying chart structure deletions");
        provider.delete(ChartItems.CONTENT_URI, null, null);
        syncResult.stats.numDeletes++;
        checkCancellation("before applying chart structure insertions");
        provider.applyBatch(DbSyncHelper.getChartUpdateOps(chart, syncResult));
    }

    private void updateConcepts(final ContentProviderClient provider, SyncResult syncResult)
        throws InterruptedException, ExecutionException, RemoteException {
        OpenMrsChartServer chartServer = new OpenMrsChartServer(App.getConnectionDetails());
        RequestFuture<JsonConceptResponse> future = RequestFuture.newFuture();
        chartServer.getConcepts(future, future); // errors handled by caller
        ArrayList<ContentValues> conceptInserts = new ArrayList<>();
        ArrayList<ContentValues> conceptNameInserts = new ArrayList<>();
        for (JsonConcept concept : future.get().results) {
            checkCancellation("while determining concepts to insert");
            // This is safe because we have implemented insert on the content provider
            // with replace.
            ContentValues conceptInsert = new ContentValues();
            conceptInsert.put(Concepts.UUID, concept.uuid);
            conceptInsert.put(Concepts.XFORM_ID, concept.xform_id);
            conceptInsert.put(Concepts.CONCEPT_TYPE, concept.type.name());
            conceptInserts.add(conceptInsert);
            syncResult.stats.numInserts++;
            for (Map.Entry<String, String> entry : concept.names.entrySet()) {
                checkCancellation("while determining concept names to insert");
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
                conceptNameInsert.put(ConceptNames.CONCEPT_UUID, concept.uuid);
                conceptNameInsert.put(ConceptNames.LOCALE, locale);
                conceptNameInsert.put(ConceptNames.NAME, name);
                conceptNameInserts.add(conceptNameInsert);
                syncResult.stats.numInserts++;
            }
        }
        checkCancellation("before inserting concepts");
        provider.bulkInsert(Concepts.CONTENT_URI,
            conceptInserts.toArray(new ContentValues[conceptInserts.size()]));
        checkCancellation("before inserting concept names");
        provider.bulkInsert(ConceptNames.CONTENT_URI,
            conceptNameInserts.toArray(new ContentValues[conceptNameInserts.size()]));

        ChartDataHelper.invalidateLoadedConceptData();
    }

    private void updatePatients(SyncResult syncResult)
        throws InterruptedException, ExecutionException, RemoteException,
        OperationApplicationException {
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        ops.addAll(DbSyncHelper.getPatientUpdateOps(syncResult));
        checkCancellation("while processing patient data from server");
        mContentResolver.applyBatch(Contracts.CONTENT_AUTHORITY, ops);
        LOG.i("Finished updating patients (" + ops.size() + " db ops)");
        mContentResolver.notifyChange(Patients.CONTENT_URI, null, false);
    }

    private void updateObservations(final ContentProviderClient provider, SyncResult syncResult)
        throws RemoteException, InterruptedException, ExecutionException, TimeoutException {
        checkCancellation("before requesting observations");
        OpenMrsChartServer chartServer = new OpenMrsChartServer(App.getConnectionDetails());
        // Get the charts asynchronously using volley.
        RequestFuture<JsonEncountersResponse> listFuture = RequestFuture.newFuture();

        TimingLogger timingLogger = new TimingLogger(LOG.tag, "obs update");
        checkCancellation("before updating observations");
        Instant lastSyncTime = getLastObservationSyncTime(provider);
        Instant newSyncTime = updateObservations(
                lastSyncTime, provider, syncResult, chartServer, listFuture, timingLogger);
        // This is only safe transactionally if we can rely on the entire sync being transactional.
        storeLastSyncTime(provider, newSyncTime);

        checkCancellation("before deleting temporary observations");
        // Remove all temporary observations now we have the real ones
        provider.delete(Observations.CONTENT_URI,
            Observations.TEMP_CACHE + "!=0",
            new String[0]);
        timingLogger.addSplit("delete temp observations");
        timingLogger.dumpToLog();
    }

    private void updateOrders(final ContentProviderClient provider, SyncResult syncResult)
        throws InterruptedException, ExecutionException, RemoteException,
        OperationApplicationException {
        ArrayList<ContentProviderOperation> ops = DbSyncHelper.getOrderUpdateOps(syncResult);
        checkCancellation("before applying order updates");
        mContentResolver.applyBatch(Contracts.CONTENT_AUTHORITY, ops);
        LOG.i("Finished updating orders (" + ops.size() + " db ops)");
        mContentResolver.notifyChange(Orders.CONTENT_URI, null, false);
    }

    private void updateForms(ContentProviderClient provider, SyncResult syncResult)
        throws InterruptedException, ExecutionException, RemoteException,
        OperationApplicationException {
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        ops.addAll(DbSyncHelper.getFormUpdateOps(syncResult));
        checkCancellation("while downloading form list from server");
        mContentResolver.applyBatch(Contracts.CONTENT_AUTHORITY, ops);
        LOG.i("Finished updating forms (" + ops.size() + " db ops)");
        mContentResolver.notifyChange(Contracts.Forms.CONTENT_URI, null, false);
    }

    private void storeFullSyncEndTime(ContentProviderClient provider, Instant syncEndTime)
        throws RemoteException {
        ContentValues cv = new ContentValues();
        cv.put(Misc.FULL_SYNC_END_MILLIS, syncEndTime.getMillis());
        provider.insert(Misc.CONTENT_URI, cv);
    }

    private void rollbackSavepoint(SQLiteDatabaseTransactionHelper dbTransactionHelper) {
        LOG.i("Rolling back savepoint %s", SYNC_SAVEPOINT_NAME);
        dbTransactionHelper.rollbackNamedTransaction(SYNC_SAVEPOINT_NAME);
    }

    private void reportProgress(int progressIncrement, @Nullable String label) {
        Intent syncProgressIntent =
            new Intent(getContext(), SyncManager.SyncStatusBroadcastReceiver.class);
        syncProgressIntent.putExtra(SyncManager.SYNC_PROGRESS, progressIncrement);
        syncProgressIntent.putExtra(SyncManager.SYNC_STATUS, SyncManager.IN_PROGRESS);
        if (label != null) {
            syncProgressIntent.putExtra(SyncManager.SYNC_PROGRESS_LABEL, label);
        }
        getContext().sendBroadcast(syncProgressIntent);
    }

    @Nullable
    private Instant getLastObservationSyncTime(ContentProviderClient provider)
            throws RemoteException {
        Cursor c = null;
        try {
            c = provider.query(
                Misc.CONTENT_URI,
                new String[] {Misc.OBS_SYNC_END_MILLIS}, null, null, null);
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
        checkCancellation("before processing observation RPC results");
        for (JsonEncounter record : response.results) {
            // Validate the encounter
            if (!isEncounterValid(record)) {
                LOG.e("Invalid encounter data received from server; dropping encounter.");
                continue;
            }
            // Add the observations from the encounter.
                toInsert.addAll(DbSyncHelper.getObsValuesToInsert(record, syncResult));
                timingLogger.addSplit("added incremental obs to list");
        }
        timingLogger.addSplit("making operations");
        if (toInsert.size() > 0) {
            checkCancellation("before inserting incremental observations");
            provider.bulkInsert(Observations.CONTENT_URI,
                toInsert.toArray(new ContentValues[toInsert.size()]));
            timingLogger.addSplit("bulk inserts");
        }
        return Instant.parse(response.snapshotTime);
    }

    /** returns {@code true} if the encounter is valid. */
    private boolean isEncounterValid(JsonEncounter record) {
        return record.patient_uuid != null
                && record.uuid != null
                && record.timestamp != null;
    }

    private void storeLastSyncTime(ContentProviderClient provider, Instant newSyncTime)
        throws RemoteException {
        ContentValues cv = new ContentValues();
        cv.put(Misc.OBS_SYNC_END_MILLIS, newSyncTime.getMillis());
        provider.insert(Misc.CONTENT_URI, cv);
    }
}
