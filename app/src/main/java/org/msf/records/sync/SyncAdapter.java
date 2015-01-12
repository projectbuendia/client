package org.msf.records.sync;

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
import android.util.TimingLogger;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.RequestFuture;

import org.joda.time.Instant;
import org.joda.time.LocalDate;
import org.msf.records.App;
import org.msf.records.model.Zone;
import org.msf.records.net.OpenMrsChartServer;
import org.msf.records.net.model.ChartStructure;
import org.msf.records.net.model.Concept;
import org.msf.records.net.model.ConceptList;
import org.msf.records.net.model.Patient;
import org.msf.records.net.model.PatientChart;
import org.msf.records.net.model.PatientChartList;
import org.msf.records.sync.providers.Contracts;
import org.msf.records.utils.Logger;
import org.msf.records.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Global sync adapter for syncing all client side database caches.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {

    private static final Logger LOG = Logger.create();

    public static final String KNOWN_CHART_UUID = "ea43f213-66fb-4af6-8a49-70fd6b9ce5d4";

    /**
     * If this key is present with boolean value true then sync patients.
     */
    public static final String SYNC_PATIENTS = "SYNC_PATIENTS";

    /**
     * If this key is present with boolean value true then sync concepts.
     */
    public static final String SYNC_CONCEPTS = "SYNC_CONCEPTS";

    /**
     * If this key is present with boolean value true then sync the chart structure.
     */
    public static final String SYNC_CHART_STRUCTURE = "SYNC_CHART_STRUCTURE";

    /**
     * If this key is present with boolean value true then sync the observations.
     */
    public static final String SYNC_OBSERVATIONS = "SYNC_OBSERVATIONS";

    /**
     * If this key is present with boolean value true then sync locations.
     */
    public static final String SYNC_LOCATIONS = "SYNC_LOCATIONS";

    /**
     * If this key is present with boolean value true then sync users.
     */
    public static final String SYNC_USERS = "SYNC_USERS";

    /**
     * If this key is present with boolean value true then sync users.
     */
    public static final String INCREMENTAL_OBSERVATIONS_UPDATE = "INCREMENTAL_OBSERVATIONS_UPDATE";

    /**
     * RPC timeout for getting observations.
     */
    private static final int OBSERVATIONS_TIMEOUT = 100;

    /**
     * Content resolver, for performing database operations.
     */
    private final ContentResolver mContentResolver;

    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        mContentResolver = context.getContentResolver();
    }

    public SyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
        mContentResolver = context.getContentResolver();
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        // Fire a broadcast indicating that sync has completed.
        Intent syncStartedIntent =
                new Intent(getContext(), SyncManager.SyncStatusBroadcastReceiver.class);
        syncStartedIntent.putExtra(SyncManager.SYNC_STATUS, SyncManager.STARTED);
        getContext().sendBroadcast(syncStartedIntent);

        Intent syncFailedIntent =
                new Intent(getContext(), SyncManager.SyncStatusBroadcastReceiver.class);
        syncFailedIntent.putExtra(SyncManager.SYNC_STATUS, SyncManager.FAILED);

        LOG.i("Beginning network synchronization");
        TimingLogger timings = new TimingLogger(LOG.tag, "onPerformSync");
        try {
            boolean specific = false;
            if (extras.getBoolean(SYNC_PATIENTS)) {
                specific = true;
                // default behaviour
                updatePatientData(syncResult);
                timings.addSplit("update patient data specified");
            }
            if (extras.getBoolean(SYNC_CONCEPTS)) {
                specific = true;
                updateConcepts(provider, syncResult);
                timings.addSplit("update concepts specified");
            }
            if (extras.getBoolean(SYNC_CHART_STRUCTURE)) {
                specific = true;
                timings.addSplit("update chart specified");
                updateChartStructure(provider, syncResult);
            }
            if (extras.getBoolean(SYNC_OBSERVATIONS)) {
                specific = true;
                updateObservations(provider, syncResult, extras);
                timings.addSplit("update observations specified");
            }
            if (extras.getBoolean(SYNC_LOCATIONS)) {
                specific = true;
                updateLocations(provider, syncResult);
                timings.addSplit("update locations specified");
            }
            if (extras.getBoolean(SYNC_USERS)) {
                specific = true;
                updateUsers(provider, syncResult);
                timings.addSplit("update users specified");
            }
            if (!specific) {
                // If nothing is specified explicitly (such as from the android system menu),
                // do everything.
                updatePatientData(syncResult);
                timings.addSplit("update all (patients)");
                updateConcepts(provider, syncResult);
                timings.addSplit("update all (concepts)");
                updateChartStructure(provider, syncResult);
                timings.addSplit("update all (chart)");
                updateObservations(provider, syncResult, extras);
                timings.addSplit("update all (observations)");
                updateLocations(provider, syncResult);
                timings.addSplit("update all (locations)");
                updateUsers(provider, syncResult);
                timings.addSplit("update all (users)");
            }
        } catch (RemoteException e) {
            LOG.e(e, "Error in RPC");
            syncResult.stats.numIoExceptions++;
            getContext().sendBroadcast(syncFailedIntent);
            return;
        } catch (OperationApplicationException e) {
            LOG.e(e, "Error updating database");
            syncResult.databaseError = true;
            getContext().sendBroadcast(syncFailedIntent);
            return;
        } catch (InterruptedException e){
            LOG.e(e, "Error interruption");
            syncResult.stats.numIoExceptions++;
            getContext().sendBroadcast(syncFailedIntent);
            return;
        } catch (ExecutionException e){
            LOG.e(e, "Error failed to execute");
            syncResult.stats.numIoExceptions++;
            getContext().sendBroadcast(syncFailedIntent);
            return;
        } catch (Exception e){
            LOG.e(e, "Error reading from network");
            syncResult.stats.numIoExceptions++;
            getContext().sendBroadcast(syncFailedIntent);
            return;
        }
        timings.dumpToLog();
        LOG.i("Network synchronization complete");

        // Fire a broadcast indicating that sync has completed.
        Intent syncCompletedIntent =
                new Intent(getContext(), SyncManager.SyncStatusBroadcastReceiver.class);
        syncCompletedIntent.putExtra(SyncManager.SYNC_STATUS, SyncManager.COMPLETED);
        getContext().sendBroadcast(syncCompletedIntent);
    }

    private void updatePatientData(SyncResult syncResult) throws InterruptedException, ExecutionException, RemoteException, OperationApplicationException {
        final ContentResolver contentResolver = getContext().getContentResolver();

        final String[] projection = PatientProjection.getProjectionColumns();

        LOG.d("Before network call");
        RequestFuture<List<Patient>> future = RequestFuture.newFuture();
        App.getServer().listPatients("", "", "", future, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                LOG.d(error.toString());
            }
        });

        //No need for callbacks as the {@AbstractThreadedSyncAdapter} code is executed in a background thread
        List<Patient> patients = future.get();
        LOG.d("After network call");
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
        LOG.i("Found " + c.getCount() + " local entries. Computing merge solution...");
        LOG.i("Found " + patients.size() + " external entries. Computing merge solution...");


        String id;
        String givenName, familyName, uuid, locationUuid;
        String gender;
        LocalDate birthdate;
        Long admissionTimestamp;

        //iterate through the list of patients
        while(c.moveToNext()){
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
            birthdate = Utils.stringToLocalDate(c.getString(PatientProjection.COLUMN_BIRTHDATE));
            gender = c.getString(PatientProjection.COLUMN_GENDER);

            Patient patient = patientsMap.get(id);
            if (patient != null) {
                // Entry exists. Remove from entry map to prevent insert later.
                patientsMap.remove(id);
                // Check to see if the entry needs to be updated
                Uri existingUri = Contracts.Patients.CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build();

                //check if it needs updating
                String patientAssignedLocationUuid =
                        patient.assigned_location == null ? null : patient.assigned_location.uuid;
                if (!Objects.equals(patient.given_name, givenName) ||
                        !Objects.equals(patient.family_name, familyName) ||
                        !Objects.equals(patient.uuid, uuid) ||
                        !Objects.equals(patient.admission_timestamp, admissionTimestamp) ||
                        !Objects.equals(patientAssignedLocationUuid, locationUuid) ||
                        !Objects.equals(patient.birthdate, birthdate) ||
                        !Objects.equals(patient.gender, gender) ||
                        !Objects.equals(patient.id, id)) {
                    // Update existing record
                    LOG.i("Scheduling update: " + existingUri);
                    batch.add(ContentProviderOperation.newUpdate(existingUri)
                            .withValue(Contracts.Patients.GIVEN_NAME, patient.given_name)
                            .withValue(Contracts.Patients.FAMILY_NAME, patient.family_name)
                            .withValue(Contracts.Patients.UUID, patient.uuid)
                            .withValue(Contracts.Patients.ADMISSION_TIMESTAMP, patient.admission_timestamp)
                            .withValue(Contracts.Patients.LOCATION_UUID, patientAssignedLocationUuid)
                            .withValue(Contracts.Patients.BIRTHDATE, Utils.localDateToString(patient.birthdate))
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
        c.close();


        for (Patient e : patientsMap.values()) {
            LOG.i("Scheduling insert: entry_id=" + e.id);
            ContentProviderOperation.Builder builder =
                    ContentProviderOperation.newInsert(Contracts.Patients.CONTENT_URI)
                            .withValue(Contracts.Patients._ID, e.id)
                            .withValue(Contracts.Patients.GIVEN_NAME, e.given_name)
                            .withValue(Contracts.Patients.FAMILY_NAME, e.family_name)
                            .withValue(Contracts.Patients.UUID, e.uuid)
                            .withValue(Contracts.Patients.ADMISSION_TIMESTAMP, e.admission_timestamp)
                            .withValue(Contracts.Patients.BIRTHDATE, Utils.localDateToString(e.birthdate))
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
        mContentResolver.applyBatch(Contracts.CONTENT_AUTHORITY, batch);
        LOG.i("batch apply done");
        mContentResolver.notifyChange(Contracts.Patients.CONTENT_URI, null, false);
        LOG.i("change notified");



        //TODO(giljulio) update the server as well as the client
    }

    private void updateConcepts(final ContentProviderClient provider, SyncResult syncResult)
            throws InterruptedException, ExecutionException, RemoteException {
        OpenMrsChartServer chartServer = new OpenMrsChartServer(App.getConnectionDetails());
        RequestFuture<ConceptList> future = RequestFuture.newFuture();
        chartServer.getConcepts(future, future); // errors handled by caller
        ConceptList conceptList = future.get();
        ArrayList<ContentValues> conceptInserts = new ArrayList<>();
        ArrayList<ContentValues> conceptNameInserts = new ArrayList<>();
        for (Concept concept : conceptList.results) {
            // This is safe because we have implemented insert on the content provider
            // with replace.
            ContentValues conceptInsert = new ContentValues();
            conceptInsert.put(Contracts.Concepts._ID, concept.uuid);
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
                conceptNameInsert.put(Contracts.ConceptNames.LOCALIZED_NAME, name);
                conceptNameInserts.add(conceptNameInsert);
                syncResult.stats.numInserts++;
            }
        }
        provider.bulkInsert(Contracts.Concepts.CONTENT_URI,
                conceptInserts.toArray(new ContentValues[conceptInserts.size()]));
        provider.bulkInsert(Contracts.ConceptNames.CONTENT_URI,
                conceptNameInserts.toArray(new ContentValues[conceptNameInserts.size()]));
    }

    private void updateLocations(final ContentProviderClient provider, SyncResult syncResult)
            throws InterruptedException, ExecutionException, RemoteException,
            OperationApplicationException {
        ArrayList<ContentProviderOperation> batch = RpcToDb.locationsRpcToDb(syncResult);
        LOG.i("locations Merge solution ready. Applying batch update");
        mContentResolver.applyBatch(Contracts.CONTENT_AUTHORITY, batch);
        mContentResolver.notifyChange(Contracts.Locations.CONTENT_URI, null, false);
        mContentResolver.notifyChange(
                Contracts.LocationNames.CONTENT_URI, null, false);

        // TODO(akalachman): Update the server as well as the client
    }

    private void updateChartStructure(final ContentProviderClient provider, SyncResult syncResult)
            throws InterruptedException, ExecutionException, RemoteException,
            OperationApplicationException {
        OpenMrsChartServer chartServer = new OpenMrsChartServer(App.getConnectionDetails());
        RequestFuture<ChartStructure> future = RequestFuture.newFuture();
        chartServer.getChartStructure(KNOWN_CHART_UUID, future, future); // errors handled by caller
        ChartStructure conceptList = future.get();
        // When we do a chart update, delete everything first.
        provider.delete(Contracts.Charts.CONTENT_URI, null, null);
        syncResult.stats.numDeletes++;
        provider.applyBatch(RpcToDb.chartStructureRpcToDb(conceptList, syncResult));
    }

    private void updateObservations(final ContentProviderClient provider, SyncResult syncResult,
                                    Bundle extras)
            throws RemoteException {

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

        OpenMrsChartServer chartServer = new OpenMrsChartServer(App.getConnectionDetails());
        // Get the charts asynchronously using volley.
        RequestFuture<PatientChartList> listFuture = RequestFuture.newFuture();

        TimingLogger timingLogger = new TimingLogger(LOG.tag, "obs update");
        try {
            Instant lastSyncTime = getLastSyncTime();
            Instant newSyncTime;
            if (extras.getBoolean(INCREMENTAL_OBSERVATIONS_UPDATE) && lastSyncTime != null) {
                newSyncTime = updateIncrementalObservations(lastSyncTime, provider, syncResult,
                        chartServer, listFuture, timingLogger);
            } else {
                newSyncTime = updateAllObservations(provider, syncResult, chartServer, listFuture,
                        timingLogger);
            }
            storeLastSyncTime(newSyncTime);
        } catch (InterruptedException e) {
            LOG.e(e, "Error interruption: ");
            syncResult.stats.numIoExceptions++;
            return;
        } catch (ExecutionException e) {
            LOG.e(e, "Error failed to execute: ");
            syncResult.stats.numIoExceptions++;
            return;
        } catch (Exception e) {
            LOG.e(e, "Error reading from network: ");
            syncResult.stats.numIoExceptions++;
            return;
        }
        // Remove all temporary observations now we have the real ones
        provider.delete(Contracts.Observations.CONTENT_URI,
                Contracts.Observations.TEMP_CACHE + "!=0",
                new String[0]);
        timingLogger.addSplit("delete temp observations");
        timingLogger.dumpToLog();
    }

    private Instant getLastSyncTime() {
        return null;
    }

    private void storeLastSyncTime(Instant newSyncTime) {

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
        PatientChartList patientChartList = listFuture.get(OBSERVATIONS_TIMEOUT, TimeUnit.SECONDS);
        LOG.d("got response ");
        timingLogger.addSplit("Get all charts RPC");
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
        bulkDelete(provider, toDelete);
        timingLogger.addSplit("bulk deletes");
        provider.bulkInsert(Contracts.Observations.CONTENT_URI,
                toInsert.toArray(new ContentValues[toInsert.size()]));
        timingLogger.addSplit("bulk inserts");
        return patientChartList.snapshotTime == null ? null :
                patientChartList.snapshotTime.toInstant();
    }

    private void bulkDelete(ContentProviderClient provider, ArrayList<String> toDelete) throws RemoteException {
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
        PatientChartList patientChartList = listFuture.get(OBSERVATIONS_TIMEOUT, TimeUnit.SECONDS);
        LOG.d("got incremental response");
        timingLogger.addSplit("Get incremental charts RPC");
        for (PatientChart patientChart : patientChartList.results) {
            // As we are doing multiple request in parallel, deal with exceptions in the loop.
            timingLogger.addSplit("awaiting incremental future");
            if (patientChart.uuid == null) {
                LOG.e("null patient id in observation response");
                continue;
            }
            // Add the new observations
            RpcToDb.observationsRpcToDb(patientChart, syncResult, toInsert);
            timingLogger.addSplit("added incremental obs to list");
        }
        timingLogger.addSplit("making operations");
        provider.bulkInsert(Contracts.Observations.CONTENT_URI,
                toInsert.toArray(new ContentValues[toInsert.size()]));
        timingLogger.addSplit("bulk inserts");
        return patientChartList.snapshotTime == null ? null :
                patientChartList.snapshotTime.toInstant();
    }

    private void updateUsers(final ContentProviderClient provider, SyncResult syncResult) {
        App.getUserManager().syncKnownUsers();
    }
}
