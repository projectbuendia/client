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
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.TimingLogger;

import com.android.volley.toolbox.RequestFuture;

import org.joda.time.Instant;
import org.joda.time.LocalDate;
import org.projectbuendia.client.App;
import org.projectbuendia.client.R;
import org.projectbuendia.client.model.Zone;
import org.projectbuendia.client.net.OpenMrsChartServer;
import org.projectbuendia.client.net.model.ChartStructure;
import org.projectbuendia.client.net.model.Concept;
import org.projectbuendia.client.net.model.ConceptList;
import org.projectbuendia.client.net.model.Patient;
import org.projectbuendia.client.net.model.PatientChart;
import org.projectbuendia.client.net.model.PatientChartList;
import org.projectbuendia.client.sync.providers.BuendiaProvider;
import org.projectbuendia.client.sync.providers.Contracts;
import org.projectbuendia.client.sync.providers.SQLiteDatabaseTransactionHelper;
import org.projectbuendia.client.user.UserManager;
import org.projectbuendia.client.utils.Logger;
import org.projectbuendia.client.utils.date.Dates;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Global sync adapter for syncing all client side database caches. */
public class SyncAdapter extends AbstractThreadedSyncAdapter {

    private static final Logger LOG = Logger.create();

    public static final String KNOWN_CHART_UUID = "ea43f213-66fb-4af6-8a49-70fd6b9ce5d4";

    /**
     * Keys in the extras bundle used to select which sync phases to do.
     * Select a phase by setting a boolean value of true for the appropriate key.
     */
    enum SyncPhase {
        SYNC_USERS,
        SYNC_LOCATIONS,
        SYNC_CHART_STRUCTURE,
        SYNC_CONCEPTS,
        SYNC_PATIENTS,
        SYNC_OBSERVATIONS
    }

    /** UI messages to show while each phase of the sync is in progress. */
    static final Map<SyncPhase, Integer> PHASE_MESSAGES = new HashMap<>();
    static {
        PHASE_MESSAGES.put(SyncPhase.SYNC_USERS, R.string.syncing_users);
        PHASE_MESSAGES.put(SyncPhase.SYNC_LOCATIONS, R.string.syncing_locations);
        PHASE_MESSAGES.put(SyncPhase.SYNC_CHART_STRUCTURE, R.string.syncing_charts);
        PHASE_MESSAGES.put(SyncPhase.SYNC_CONCEPTS, R.string.syncing_concepts);
        PHASE_MESSAGES.put(SyncPhase.SYNC_PATIENTS, R.string.syncing_patients);
        PHASE_MESSAGES.put(SyncPhase.SYNC_OBSERVATIONS, R.string.syncing_observations);
    }

    enum SyncOption {
        /**
         * If this key is present with a boolean value of true, only fetch
         * observations entered since the last fetch of observations.
         */
        INCREMENTAL_OBS,
        /**
         * If this key is present with boolean value true, then the starting
         * and ending times of the entire sync operation will be recorded (as a
         * way of recording whether a full sync has ever successfully completed).
         */
        FULL_SYNC
    }

    /** RPC timeout for getting observations. */
    private static final int OBSERVATIONS_TIMEOUT_SECS = 180;

    /** Named used during the sync process for SQL savepoints. */
    private static final String SYNC_SAVEPOINT_NAME = "SYNC_SAVEPOINT";

    /** Content resolver, for performing database operations. */
    private final ContentResolver mContentResolver;

    /** Tracks whether the sync has been canceled. */
    private boolean mIsSyncCanceled = false;

    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        mContentResolver = context.getContentResolver();
    }

    public SyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
        mContentResolver = context.getContentResolver();
    }

    @Override
    public void onSyncCanceled() {
        mIsSyncCanceled = true;
        LOG.i("Detecting a sync cancellation, canceling sync soon.");
    }

    /** Not thread-safe but, by default, this will never be called multiple times in parallel. */
    @Override
    public void onPerformSync(
            Account account,
            Bundle extras,
            String authority,
            ContentProviderClient provider,
            SyncResult syncResult) {
        // Fire a broadcast indicating that sync has completed.
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
            LOG.e("Sync failed: Buendia API is unavailable.");
            getContext().sendBroadcast(syncFailedIntent);
        }

        try {
            checkCancellation("Sync was canceled before it started.");
        } catch (CancellationException e) {
            getContext().sendBroadcast(syncCanceledIntent);
            return;
        }

        LOG.i("Beginning network synchronization");
        reportProgress(0, R.string.sync_in_progress);

        BuendiaProvider buendiaProvider =
                (BuendiaProvider) (provider.getLocalContentProvider());
        SQLiteDatabaseTransactionHelper dbTransactionHelper =
                buendiaProvider.getDbTransactionHelper();
        LOG.i("Setting savepoint %s", SYNC_SAVEPOINT_NAME);
        dbTransactionHelper.startNamedTransaction(SYNC_SAVEPOINT_NAME);

        TimingLogger timings = new TimingLogger(LOG.tag, "onPerformSync");

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

        try {
            if (extras.getBoolean(SyncOption.FULL_SYNC.name())) {
                Instant syncStartTime = Instant.now();
                LOG.i("Recording full sync start time: " + syncStartTime);
                storeFullSyncStartTime(provider, syncStartTime);
            }

            int progressIncrement = 100 / phases.size();
            for (SyncPhase phase : SyncPhase.values()) {
                if (!phases.contains(phase)) break;
                checkCancellation("Sync was cancelled before " + phase);
                reportProgress(0, PHASE_MESSAGES.get(phase));

                switch (phase) {
                    // Patients: Always fetch all patients.  The patient list changes
                    // often and can grow very large; incremental fetch would help a lot.
                    // TODO: Implement incremental fetch for patients.
                    case SYNC_PATIENTS:
                        updatePatientData(syncResult);
                        break;

                    // Concepts: Always fetch all available concepts.  The concepts only
                    // need to be updated when the form definitions change; this is
                    // infrequent, so wiping and reloading all concepts is acceptable.
                    case SYNC_CONCEPTS:
                        updateConcepts(provider, syncResult);
                        break;

                    // Chart layouts: Always fetch everything.  The layouts only need
                    // to be updated when the profile changes; this is infrequent, so
                    // wiping and reloading all chart layouts is acceptable.
                    case SYNC_CHART_STRUCTURE:
                        updateChartStructure(provider, syncResult);
                        break;

                    // Observations: Both full fetch and incremental fetch are supported.
                    case SYNC_OBSERVATIONS:
                        updateObservations(provider, syncResult,
                                extras.getBoolean(SyncOption.INCREMENTAL_OBS.name()));
                        break;

                    // Locations: Always fetch everything.  The locations only need
                    // to be updated when the profile changes, which is infrequent,
                    // so wiping and reloading all locations is acceptable.
                    case SYNC_LOCATIONS:
                        updateLocations(provider, syncResult);
                        break;

                    // Users: Always fetch all users.  New users aren't added all that
                    // often and the set of users stays fairly small, so wiping and
                    // reloading all users is acceptable.
                    case SYNC_USERS:
                        updateUsers(provider, syncResult);
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
        } catch (RemoteException e) {
            rollbackSavepoint(dbTransactionHelper);
            LOG.e(e, "Error in RPC");
            syncResult.stats.numIoExceptions++;
            getContext().sendBroadcast(syncFailedIntent);
            return;
        } catch (OperationApplicationException e) {
            rollbackSavepoint(dbTransactionHelper);
            LOG.e(e, "Error updating database");
            syncResult.databaseError = true;
            getContext().sendBroadcast(syncFailedIntent);
            return;
        } catch (InterruptedException e) {
            rollbackSavepoint(dbTransactionHelper);
            LOG.e(e, "Error interruption");
            syncResult.stats.numIoExceptions++;
            getContext().sendBroadcast(syncFailedIntent);
            return;
        } catch (ExecutionException e) {
            rollbackSavepoint(dbTransactionHelper);
            LOG.e(e, "Error failed to execute");
            syncResult.stats.numIoExceptions++;
            getContext().sendBroadcast(syncFailedIntent);
            return;
        } catch (Exception e) {
            rollbackSavepoint(dbTransactionHelper);
            LOG.e(e, "Error reading from network");
            syncResult.stats.numIoExceptions++;
            getContext().sendBroadcast(syncFailedIntent);
            return;
        } catch (UserManager.UserSyncException e) {
            rollbackSavepoint(dbTransactionHelper);
            LOG.e(e, "Error syncing users");
            syncResult.stats.numIoExceptions++;
            getContext().sendBroadcast(syncFailedIntent);
            return;
        } finally {
            LOG.i("Releasing savepoint %s", SYNC_SAVEPOINT_NAME);
            dbTransactionHelper.releaseNamedTransaction(SYNC_SAVEPOINT_NAME);
            dbTransactionHelper.close();
        }
        timings.dumpToLog();
        LOG.i("Network synchronization complete");

        // Fire a broadcast indicating that sync has completed.
        Intent syncCompletedIntent =
                new Intent(getContext(), SyncManager.SyncStatusBroadcastReceiver.class);
        syncCompletedIntent.putExtra(SyncManager.SYNC_STATUS, SyncManager.COMPLETED);
        getContext().sendBroadcast(syncCompletedIntent);
    }

    private void rollbackSavepoint(SQLiteDatabaseTransactionHelper dbTransactionHelper) {
        LOG.i("Rolling back savepoint %s", SYNC_SAVEPOINT_NAME);
        dbTransactionHelper.rollbackNamedTransaction(SYNC_SAVEPOINT_NAME);
    }

    /**
     * Enforces sync cancellation, throwing a {@link CancellationException} if the sync has been
     * canceled. It is the responsibility of the caller to perform any actual cancellation
     * procedures.
     */
    private synchronized void checkCancellation(String message) throws CancellationException {
        if (mIsSyncCanceled) {
            mIsSyncCanceled = false;
            throw new CancellationException(message);
        }
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

    private void reportProgress(int progressIncrement, int stringResource) {
        String progressString = getContext().getResources().getString(stringResource);
        LOG.d("Sync progress checkpoint: +%d%%, %s", progressIncrement, progressString);
        reportProgress(progressIncrement, progressString);
    }

    private void reportProgress(int progressIncrement) {
        reportProgress(progressIncrement, null);
    }

    private void updatePatientData(SyncResult syncResult)
            throws InterruptedException, ExecutionException, RemoteException,
            OperationApplicationException {
        final ContentResolver contentResolver = getContext().getContentResolver();

        final String[] projection = PatientProjection.getProjectionColumns();

        LOG.d("Before network call to retrieve patients");
        RequestFuture<List<Patient>> future = RequestFuture.newFuture();
        App.getServer().listPatients("", "", "", future, future);

        // No need for callbacks as the {@AbstractThreadedSyncAdapter} code is executed in a
        // background thread
        List<Patient> patients = future.get();
        LOG.d("After network call to retrieve patients");
        checkCancellation("Sync was canceled before parsing retrieved patient data.");
        ArrayList<ContentProviderOperation> batch = new ArrayList<>();


        HashMap<String, Patient> patientsMap = new HashMap<>();
        for (Patient p : patients) {
            patientsMap.put(p.id, p);
        }

        // Get list of all items
        LOG.i("Fetching local entries for merge");
        Uri uri = Contracts.Patients.CONTENT_URI; // Get all entries
        Cursor c = contentResolver.query(uri, projection, null, null, null);
        assert c != null;
        checkCancellation("Sync was canceled before merging patient data.");
        LOG.i("Found " + c.getCount() + " local entries. Computing merge solution...");
        LOG.i("Found " + patients.size() + " external entries. Computing merge solution...");


        String id;
        String givenName;
        String familyName;
        String uuid;
        String locationUuid;
        String gender;
        LocalDate birthdate;
        Long admissionTimestamp;

        //iterate through the list of patients
        try {
            while (c.moveToNext()) {
                checkCancellation("Sync was canceled while merging retrieved patient data.");
                syncResult.stats.numEntries++;

                id = c.getString(PatientProjection.COLUMN_ID);
                givenName = c.getString(PatientProjection.COLUMN_GIVEN_NAME);
                familyName = c.getString(PatientProjection.COLUMN_FAMILY_NAME);
                uuid = c.getString(PatientProjection.COLUMN_UUID);
                admissionTimestamp = c.getLong(PatientProjection.COLUMN_ADMISSION_TIMESTAMP);
                locationUuid = c.getString(PatientProjection.COLUMN_LOCATION_UUID);
                if (locationUuid == null) {
                    locationUuid = Zone.DEFAULT_LOCATION;
                }
                birthdate = Dates.toLocalDate(
                        c.getString(PatientProjection.COLUMN_BIRTHDATE));
                gender = c.getString(PatientProjection.COLUMN_GENDER);

                Patient patient = patientsMap.get(id);
                if (patient != null) {
                    // Entry exists. Remove from entry map to prevent insert later.
                    patientsMap.remove(id);
                    // Check to see if the entry needs to be updated
                    Uri existingUri =
                            Contracts.Patients.CONTENT_URI.buildUpon()
                                    .appendPath(String.valueOf(id)).build();

                    //check if it needs updating
                    String patientAssignedLocationUuid =
                            patient.assigned_location == null
                                    ? null : patient.assigned_location.uuid;
                    if (!Objects.equals(patient.given_name, givenName)
                            || !Objects.equals(patient.family_name, familyName)
                            || !Objects.equals(patient.uuid, uuid)
                            || !Objects.equals(patient.admission_timestamp, admissionTimestamp)
                            || !Objects.equals(patientAssignedLocationUuid, locationUuid)
                            || !Objects.equals(patient.birthdate, birthdate)
                            || !Objects.equals(patient.gender, gender)
                            || !Objects.equals(patient.id, id)) {
                        // Update existing record
                        LOG.i("Scheduling update: " + existingUri);
                        batch.add(ContentProviderOperation.newUpdate(existingUri)
                                .withValue(Contracts.Patients.GIVEN_NAME, patient.given_name)
                                .withValue(Contracts.Patients.FAMILY_NAME, patient.family_name)
                                .withValue(Contracts.Patients.UUID, patient.uuid)
                                .withValue(
                                        Contracts.Patients.ADMISSION_TIMESTAMP,
                                        patient.admission_timestamp)
                                .withValue(
                                        Contracts.Patients.LOCATION_UUID,
                                        patientAssignedLocationUuid)
                                .withValue(
                                        Contracts.Patients.BIRTHDATE,
                                        Dates.toString(patient.birthdate))
                                .withValue(Contracts.Patients.GENDER, patient.gender)
                                .withValue(Contracts.Patients._ID, patient.id)
                                .build());
                        syncResult.stats.numUpdates++;
                    } else {
                        LOG.i("No action required for " + existingUri);
                    }
                } else {
                    // Entry doesn't exist. Remove it from the database.
                    Uri deleteUri = Contracts.Patients.CONTENT_URI.buildUpon()
                            .appendPath(id).build();
                    LOG.i("Scheduling delete: " + deleteUri);
                    batch.add(ContentProviderOperation.newDelete(deleteUri).build());
                    syncResult.stats.numDeletes++;
                }
            }
        } finally {
            c.close();
        }


        for (Patient e : patientsMap.values()) {
            checkCancellation("Sync was canceled while inserting new patient data.");
            LOG.i("Scheduling insert: entry_id=" + e.id);
            ContentProviderOperation.Builder builder =
                    ContentProviderOperation.newInsert(Contracts.Patients.CONTENT_URI)
                            .withValue(Contracts.Patients._ID, e.id)
                            .withValue(Contracts.Patients.GIVEN_NAME, e.given_name)
                            .withValue(Contracts.Patients.FAMILY_NAME, e.family_name)
                            .withValue(Contracts.Patients.UUID, e.uuid)
                            .withValue(
                                    Contracts.Patients.ADMISSION_TIMESTAMP,
                                    e.admission_timestamp)
                            .withValue(
                                    Contracts.Patients.BIRTHDATE,
                                    Dates.toString(e.birthdate))
                            .withValue(Contracts.Patients.GENDER, e.gender);

            if (e.assigned_location == null) {
                builder.withValue(
                        Contracts.Patients.LOCATION_UUID, Zone.DEFAULT_LOCATION);
            } else {
                builder.withValue(
                        Contracts.Patients.LOCATION_UUID, e.assigned_location.uuid);
            }

            batch.add(builder.build());

            syncResult.stats.numInserts++;
        }
        LOG.i("Merge solution ready. Applying batch update");
        checkCancellation("Sync was canceled before completing patient merge operation.");
        mContentResolver.applyBatch(Contracts.CONTENT_AUTHORITY, batch);
        LOG.i("batch apply done");
        mContentResolver.notifyChange(Contracts.Patients.CONTENT_URI, null, false);
        LOG.i("change notified");
    }

    private void updateConcepts(final ContentProviderClient provider, SyncResult syncResult)
            throws InterruptedException, ExecutionException, RemoteException {
        OpenMrsChartServer chartServer = new OpenMrsChartServer(App.getConnectionDetails());
        RequestFuture<ConceptList> future = RequestFuture.newFuture();
        chartServer.getConcepts(future, future); // errors handled by caller
        final ConceptList conceptList = future.get();
        ArrayList<ContentValues> conceptInserts = new ArrayList<>();
        ArrayList<ContentValues> conceptNameInserts = new ArrayList<>();
        for (Concept concept : conceptList.results) {
            checkCancellation("Sync was canceled while determining concepts to insert.");
            // This is safe because we have implemented insert on the content provider
            // with replace.
            ContentValues conceptInsert = new ContentValues();
            conceptInsert.put(Contracts.Concepts._ID, concept.uuid);
            conceptInsert.put(Contracts.Concepts.XFORM_ID, concept.xform_id);
            conceptInsert.put(Contracts.Concepts.CONCEPT_TYPE, concept.type.name());
            conceptInserts.add(conceptInsert);
            syncResult.stats.numInserts++;
            for (Map.Entry<String, String> entry : concept.names.entrySet()) {
                checkCancellation("Sync was canceled while determining concept names to insert.");
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
        checkCancellation("Sync was canceled before inserting concepts.");
        provider.bulkInsert(Contracts.Concepts.CONTENT_URI,
                conceptInserts.toArray(new ContentValues[conceptInserts.size()]));
        checkCancellation("Sync was canceled before inserting concept names.");
        provider.bulkInsert(Contracts.ConceptNames.CONTENT_URI,
                conceptNameInserts.toArray(new ContentValues[conceptNameInserts.size()]));
    }

    private void updateLocations(final ContentProviderClient provider, SyncResult syncResult)
            throws InterruptedException, ExecutionException, RemoteException,
            OperationApplicationException {
        ArrayList<ContentProviderOperation> batch = RpcToDb.locationsRpcToDb(syncResult);
        checkCancellation("Sync was canceled before applying location updates.");
        LOG.i("locations Merge solution ready. Applying batch update");
        mContentResolver.applyBatch(Contracts.CONTENT_AUTHORITY, batch);
        mContentResolver.notifyChange(Contracts.Locations.CONTENT_URI, null, false);
        mContentResolver.notifyChange(
                Contracts.LocationNames.CONTENT_URI, null, false);
    }

    private void updateChartStructure(final ContentProviderClient provider, SyncResult syncResult)
            throws InterruptedException, ExecutionException, RemoteException,
            OperationApplicationException {
        OpenMrsChartServer chartServer = new OpenMrsChartServer(App.getConnectionDetails());
        RequestFuture<ChartStructure> future = RequestFuture.newFuture();
        chartServer.getChartStructure(KNOWN_CHART_UUID, future, future); // errors handled by caller
        final ChartStructure conceptList = future.get();
        checkCancellation("Sync was canceled before applying chart structure deletions.");
        // When we do a chart update, delete everything first.
        provider.delete(Contracts.Charts.CONTENT_URI, null, null);
        checkCancellation("Sync was canceled before applying chart structure insertions.");
        syncResult.stats.numDeletes++;
        provider.applyBatch(RpcToDb.chartStructureRpcToDb(conceptList, syncResult));
    }

    private void updateObservations(final ContentProviderClient provider, SyncResult syncResult,
                                    boolean incrementalFetch)
            throws RemoteException, InterruptedException, ExecutionException, TimeoutException {

        // Get call patients from the cache.
        Uri uri = Contracts.Patients.CONTENT_URI; // Get all entries
        Cursor c = provider.query(
                uri, new String[] {Contracts.Patients.UUID}, null, null, null);
        try {
            if (c.getCount() < 1) {
                return;
            }
        } finally {
            c.close();
        }

        checkCancellation("Sync was canceled before requesting observations.");
        OpenMrsChartServer chartServer = new OpenMrsChartServer(App.getConnectionDetails());
        // Get the charts asynchronously using volley.
        RequestFuture<PatientChartList> listFuture = RequestFuture.newFuture();

        TimingLogger timingLogger = new TimingLogger(LOG.tag, "obs update");
        Instant lastSyncTime = getLastSyncTime(provider);
        Instant newSyncTime;
        checkCancellation("Sync was canceled before updating observations.");
        if (incrementalFetch && lastSyncTime != null) {
            newSyncTime = updateIncrementalObservations(lastSyncTime, provider, syncResult,
                    chartServer, listFuture, timingLogger);
        } else {
            newSyncTime = updateAllObservations(provider, syncResult, chartServer, listFuture,
                    timingLogger);
        }
        // This is only safe transactionally if we can rely on the entire sync being transactional.
        storeLastSyncTime(provider, newSyncTime);

        checkCancellation("Sync was canceled before deleting temporary observations.");
        // Remove all temporary observations now we have the real ones
        provider.delete(Contracts.Observations.CONTENT_URI,
                Contracts.Observations.TEMP_CACHE + "!=0",
                new String[0]);
        timingLogger.addSplit("delete temp observations");
        timingLogger.dumpToLog();
    }

    private Instant getLastSyncTime(ContentProviderClient provider) throws RemoteException {
        Cursor c = null;
        try {
            c = provider.query(
                    Contracts.Misc.CONTENT_URI,
                    new String[]{Contracts.Misc.OBS_SYNC_TIME}, null, null, null);
            if (c.moveToNext()) {
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

    private void storeLastSyncTime(ContentProviderClient provider, Instant newSyncTime)
            throws RemoteException {
        ContentValues contentValues = new ContentValues();
        contentValues.put(Contracts.Misc.OBS_SYNC_TIME, newSyncTime.getMillis());
        provider.insert(Contracts.Misc.CONTENT_URI, contentValues);
    }

    private Instant updateAllObservations(
            ContentProviderClient provider, SyncResult syncResult, OpenMrsChartServer chartServer,
            RequestFuture<PatientChartList> listFuture, TimingLogger timingLogger)
            throws RemoteException, InterruptedException, ExecutionException, TimeoutException {
        LOG.d("requesting all charts");
        chartServer.getAllCharts(listFuture, listFuture);
        ArrayList<String> toDelete = new ArrayList<>();
        ArrayList<ContentValues> toInsert = new ArrayList<>();
        LOG.d("awaiting parsed response");
        final PatientChartList patientChartList =
                listFuture.get(OBSERVATIONS_TIMEOUT_SECS, TimeUnit.SECONDS);
        LOG.d("got response ");
        timingLogger.addSplit("Get all charts RPC");
        checkCancellation("Sync was canceled before processing observation RPC results.");
        for (PatientChart patientChart : patientChartList.results) {
            // As we are doing multiple request in parallel, deal with exceptions in the loop.
            timingLogger.addSplit("awaiting future");
            if (patientChart.uuid == null) {
                LOG.e("null patient id in observation response");
                continue;
            }
            // Delete all existing observations for the patient.
            toDelete.add(patientChart.uuid);
            timingLogger.addSplit("added delete to list");
            // Add the new observations
            RpcToDb.observationsRpcToDb(patientChart, syncResult, toInsert);
            timingLogger.addSplit("added obs to list");
        }
        timingLogger.addSplit("making operations");
        checkCancellation("Sync was canceled before bulk deleting observations.");
        bulkDelete(provider, toDelete);
        timingLogger.addSplit("bulk deletes");
        checkCancellation("Sync was canceled before bulk inserting observations.");
        provider.bulkInsert(Contracts.Observations.CONTENT_URI,
                toInsert.toArray(new ContentValues[toInsert.size()]));
        timingLogger.addSplit("bulk inserts");
        return patientChartList.snapshotTime == null ? null :
                patientChartList.snapshotTime.toInstant();
    }

    private void bulkDelete(ContentProviderClient provider, ArrayList<String> toDelete)
            throws RemoteException {
        StringBuilder select = new StringBuilder(Contracts.Observations.PATIENT_UUID);
        select.append(" IN (");
        boolean first = true;
        for (String uuid : toDelete) {
            if (first) {
                first = false;
            } else {
                select.append(',');
            }
            select.append('\"');
            select.append(uuid);
            select.append('\"');
        }
        select.append(")");
        provider.delete(Contracts.Observations.CONTENT_URI, select.toString(),
                new String[0]);
    }

    private Instant updateIncrementalObservations(Instant lastSyncTime,
            ContentProviderClient provider, SyncResult syncResult, OpenMrsChartServer chartServer,
            RequestFuture<PatientChartList> listFuture, TimingLogger timingLogger)
            throws RemoteException, InterruptedException, ExecutionException, TimeoutException {
        LOG.d("requesting incremental charts");
        chartServer.getIncrementalCharts(lastSyncTime, listFuture, listFuture);
        ArrayList<ContentValues> toInsert = new ArrayList<>();
        LOG.d("awaiting parsed incremental response");
        final PatientChartList patientChartList =
                listFuture.get(OBSERVATIONS_TIMEOUT_SECS, TimeUnit.SECONDS);
        LOG.d("got incremental response");
        timingLogger.addSplit("Get incremental charts RPC");
        checkCancellation("Sync was canceled before processing observation RPC results.");
        for (PatientChart patientChart : patientChartList.results) {
            // As we are doing multiple request in parallel, deal with exceptions in the loop.
            timingLogger.addSplit("awaiting incremental future");
            if (patientChart.uuid == null) {
                LOG.e("null patient id in observation response");
                continue;
            }
            if (patientChart.encounters.length > 0) {
                // Add the new observations
                RpcToDb.observationsRpcToDb(patientChart, syncResult, toInsert);
                timingLogger.addSplit("added incremental obs to list");
            }
        }
        timingLogger.addSplit("making operations");
        if (toInsert.size() > 0) {
            checkCancellation("Sync was canceled before inserting incremental observations.");
            provider.bulkInsert(Contracts.Observations.CONTENT_URI,
                    toInsert.toArray(new ContentValues[toInsert.size()]));
            timingLogger.addSplit("bulk inserts");
        }
        return patientChartList.snapshotTime == null ? null :
                patientChartList.snapshotTime.toInstant();
    }

    private void updateUsers(final ContentProviderClient provider, SyncResult syncResult)
            throws InterruptedException, ExecutionException, RemoteException,
            OperationApplicationException, UserManager.UserSyncException {
        App.getUserManager().syncKnownUsersSynchronously();
    }

    private void storeFullSyncStartTime(ContentProviderClient provider, Instant syncStartTime)
            throws RemoteException {
        ContentValues contentValues = new ContentValues();
        contentValues.put(Contracts.Misc.FULL_SYNC_START_TIME, syncStartTime.getMillis());
        provider.insert(Contracts.Misc.CONTENT_URI, contentValues);
    }

    private void storeFullSyncEndTime(ContentProviderClient provider, Instant syncEndTime)
            throws RemoteException {
        ContentValues contentValues = new ContentValues();
        contentValues.put(Contracts.Misc.FULL_SYNC_END_TIME, syncEndTime.getMillis());
        provider.insert(Contracts.Misc.CONTENT_URI, contentValues);
    }
}
