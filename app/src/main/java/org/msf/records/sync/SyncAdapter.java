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
import android.util.Log;
import android.util.TimingLogger;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.RequestFuture;

import org.msf.records.App;
import org.msf.records.model.Zone;
import org.msf.records.net.OpenMrsChartServer;
import org.msf.records.net.model.ChartStructure;
import org.msf.records.net.model.Concept;
import org.msf.records.net.model.ConceptList;
import org.msf.records.net.model.Patient;
import org.msf.records.net.model.PatientChart;
import org.msf.records.net.model.PatientChartList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.msf.records.sync.ChartProviderContract.CONCEPTS_CONTENT_URI;
import static org.msf.records.sync.ChartProviderContract.CONCEPT_NAMES_CONTENT_URI;
import static org.msf.records.sync.ChartProviderContract.ChartColumns;
import static org.msf.records.sync.PatientProviderContract.PatientColumns;

/**
 * Global sync adapter for syncing all client side database caches.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {

    private static final String TAG = SyncAdapter.class.getSimpleName();

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

        Log.i(TAG, "Beginning network synchronization");
        TimingLogger timings = new TimingLogger(TAG, "onPerformSync");
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
                updateObservations(provider, syncResult);
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
                updateObservations(provider, syncResult);
                timings.addSplit("update all (observations)");
                updateLocations(provider, syncResult);
                timings.addSplit("update all (locations)");
                updateUsers(provider, syncResult);
                timings.addSplit("update all (users)");
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Error in RPC", e);
            syncResult.stats.numIoExceptions++;
            getContext().sendBroadcast(syncFailedIntent);
            return;
        } catch (OperationApplicationException e) {
            Log.e(TAG, "Error updating database", e);
            syncResult.databaseError = true;
            getContext().sendBroadcast(syncFailedIntent);
            return;
        } catch (InterruptedException e){
            Log.e(TAG, "Error interruption", e);
            syncResult.stats.numIoExceptions++;
            getContext().sendBroadcast(syncFailedIntent);
            return;
        } catch (ExecutionException e){
            Log.e(TAG, "Error failed to execute", e);
            syncResult.stats.numIoExceptions++;
            getContext().sendBroadcast(syncFailedIntent);
            return;
        } catch (Exception e){
            Log.e(TAG, "Error reading from network", e);
            syncResult.stats.numIoExceptions++;
            getContext().sendBroadcast(syncFailedIntent);
            return;
        }
        timings.dumpToLog();
        Log.i(TAG, "Network synchronization complete");

        // Fire a broadcast indicating that sync has completed.
        Intent syncCompletedIntent =
                new Intent(getContext(), SyncManager.SyncStatusBroadcastReceiver.class);
        syncCompletedIntent.putExtra(SyncManager.SYNC_STATUS, SyncManager.COMPLETED);
        getContext().sendBroadcast(syncCompletedIntent);
    }

    private void updatePatientData(SyncResult syncResult) throws InterruptedException, ExecutionException, RemoteException, OperationApplicationException {
        final ContentResolver contentResolver = getContext().getContentResolver();

        final String[] projection = PatientProjection.getProjectionColumns();

        Log.d(TAG, "Before network call");
        RequestFuture<List<Patient>> future = RequestFuture.newFuture();
        App.getServer().listPatients("", "", "", future, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, error.toString());
            }
        }, TAG);

        //No need for callbacks as the {@AbstractThreadedSyncAdapter} code is executed in a background thread
        List<Patient> patients = future.get();
        Log.d(TAG, "After network call");
        ArrayList<ContentProviderOperation> batch = new ArrayList<>();


        HashMap<String, Patient> patientsMap = new HashMap<>();
        for (Patient p : patients) {
            patientsMap.put(p.id, p);
        }

        // Get list of all items
        Log.i(TAG, "Fetching local entries for merge");
        Uri uri = PatientProviderContract.CONTENT_URI; // Get all entries
        try (Cursor c = contentResolver.query(uri, projection, null, null, null)) {
            assert c != null;
            Log.i(TAG, "Found " + c.getCount() + " local entries. Computing merge solution...");
            Log.i(TAG, "Found " + patients.size() + " external entries. Computing merge solution...");


            String id;
            String givenName, familyName, uuid, locationUuid;
            String gender;
            int ageMonths = -1, ageYears = -1;
            long admissionTimestamp;

            //iterate through the list of patients
            while (c.moveToNext()) {
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
                if (!c.isNull(PatientProjection.COLUMN_AGE_MONTHS)) {
                    ageMonths = c.getInt(PatientProjection.COLUMN_AGE_MONTHS);
                }
                if (!c.isNull(PatientProjection.COLUMN_AGE_YEARS)) {
                    ageYears = c.getInt(PatientProjection.COLUMN_AGE_YEARS);
                }
                gender = c.getString(PatientProjection.COLUMN_GENDER);

                Patient patient = patientsMap.get(id);
                if (patient != null) {
                    // Entry exists. Remove from entry map to prevent insert later.
                    patientsMap.remove(id);
                    // Check to see if the entry needs to be updated
                    Uri existingUri = PatientProviderContract.CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build();

                    //check if it needs updating
                    if ((patient.given_name != null && !patient.given_name.equals(givenName)) ||
                            (patient.family_name != null && !patient.family_name.equals(familyName)) ||
                            (patient.uuid != null && !patient.uuid.equals(uuid)) ||
                            (patient.admission_timestamp != null &&
                                    !patient.admission_timestamp.equals(admissionTimestamp)) ||
                            (patient.assigned_location != null &&
                                    patient.assigned_location.uuid != null &&
                                    !patient.assigned_location.uuid.equals(locationUuid)) ||
                            (patient.age.months != ageMonths) ||
                            (patient.age.years != ageYears) ||
                            (patient.gender != null && !patient.gender.equals(gender)) ||
                            (patient.id != null && !patient.id.equals(id))) {
                        // Update existing record
                        Log.i(TAG, "Scheduling update: " + existingUri);
                        batch.add(ContentProviderOperation.newUpdate(existingUri)
                                .withValue(PatientColumns.COLUMN_NAME_GIVEN_NAME, givenName)
                                .withValue(PatientColumns.COLUMN_NAME_FAMILY_NAME, familyName)
                                .withValue(PatientColumns.COLUMN_NAME_UUID, uuid)
                                .withValue(PatientColumns.COLUMN_NAME_ADMISSION_TIMESTAMP, admissionTimestamp)
                                .withValue(PatientColumns.COLUMN_NAME_LOCATION_UUID, locationUuid)
                                .withValue(PatientColumns.COLUMN_NAME_AGE_MONTHS, ageMonths)
                                .withValue(PatientColumns.COLUMN_NAME_AGE_YEARS, ageYears)
                                .withValue(PatientColumns.COLUMN_NAME_GENDER, gender)
                                .withValue(PatientColumns._ID, id)
                                .build());
                        syncResult.stats.numUpdates++;
                    } else {
                        Log.i(TAG, "No action required for " + existingUri);
                    }
                } else {
                    // Entry doesn't exist. Remove it from the database.
                    Uri deleteUri = PatientProviderContract.CONTENT_URI.buildUpon()
                            .appendPath(id).build();
                    Log.i(TAG, "Scheduling delete: " + deleteUri);
                    batch.add(ContentProviderOperation.newDelete(deleteUri).build());
                    syncResult.stats.numDeletes++;
                }
            }
        }


        for (Patient e : patientsMap.values()) {
            Log.i(TAG, "Scheduling insert: entry_id=" + e.id);
            ContentProviderOperation.Builder builder =
                    ContentProviderOperation.newInsert(PatientProviderContract.CONTENT_URI)
                            .withValue(PatientColumns._ID, e.id)
                            .withValue(PatientColumns.COLUMN_NAME_GIVEN_NAME, e.given_name)
                            .withValue(PatientColumns.COLUMN_NAME_FAMILY_NAME, e.family_name)
                            .withValue(PatientColumns.COLUMN_NAME_UUID, e.uuid)
                            .withValue(PatientColumns.COLUMN_NAME_ADMISSION_TIMESTAMP, e.admission_timestamp)
                            .withValue(PatientColumns.COLUMN_NAME_AGE_MONTHS, e.age.months)
                            .withValue(PatientColumns.COLUMN_NAME_AGE_YEARS, e.age.years)
                            .withValue(PatientColumns.COLUMN_NAME_GENDER, e.gender);

            if (e.assigned_location == null) {
                builder.withValue(
                        PatientColumns.COLUMN_NAME_LOCATION_UUID, Zone.DEFAULT_LOCATION);
            } else {
                builder.withValue(
                        PatientColumns.COLUMN_NAME_LOCATION_UUID, e.assigned_location.uuid);
            }

            batch.add(builder.build());

            syncResult.stats.numInserts++;
        }
        Log.i(TAG, "Merge solution ready. Applying batch update");
        mContentResolver.applyBatch(PatientProviderContract.CONTENT_AUTHORITY, batch);
        Log.i(TAG, "batch apply done");
        mContentResolver.notifyChange(PatientProviderContract.CONTENT_URI, null, false);
        Log.i(TAG, "change notified");



        //TODO(giljulio) update the server as well as the client
    }

    private void updateConcepts(final ContentProviderClient provider, SyncResult syncResult)
            throws InterruptedException, ExecutionException, RemoteException,
            OperationApplicationException {
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
            conceptInsert.put(ChartColumns._ID, concept.uuid);
            conceptInsert.put(ChartColumns.XFORM_ID, concept.xform_id);
            conceptInsert.put(ChartColumns.CONCEPT_TYPE, concept.type.name());
            conceptInserts.add(conceptInsert);
            syncResult.stats.numInserts++;
            for (Map.Entry<String, String> entry : concept.names.entrySet()) {
                String locale = entry.getKey();
                if (locale == null) {
                    Log.e(TAG, "null locale in concept name rpc for " + concept);
                    continue;
                }
                String name = entry.getValue();
                if (name == null) {
                    Log.e(TAG, "null name in concept name rpc for " + concept);
                    continue;
                }
                ContentValues conceptNameInsert = new ContentValues();
                conceptNameInsert.put(ChartColumns.CONCEPT_UUID, concept.uuid);
                conceptNameInsert.put(ChartColumns.LOCALE, locale);
                conceptNameInsert.put(ChartColumns.NAME, name);
                conceptNameInserts.add(conceptNameInsert);
                syncResult.stats.numInserts++;
            }
        }
        provider.bulkInsert(CONCEPTS_CONTENT_URI,
                conceptInserts.toArray(new ContentValues[conceptInserts.size()]));
        provider.bulkInsert(CONCEPT_NAMES_CONTENT_URI,
                conceptNameInserts.toArray(new ContentValues[conceptNameInserts.size()]));
    }

    private void updateLocations(final ContentProviderClient provider, SyncResult syncResult)
            throws InterruptedException, ExecutionException, RemoteException,
            OperationApplicationException {
        ArrayList<ContentProviderOperation> batch = RpcToDb.locationsRpcToDb(syncResult);
        Log.i(TAG, "locations Merge solution ready. Applying batch update");
        mContentResolver.applyBatch(PatientProviderContract.CONTENT_AUTHORITY, batch);
        mContentResolver.notifyChange(LocationProviderContract.LOCATIONS_CONTENT_URI, null, false);
        mContentResolver.notifyChange(
                LocationProviderContract.LOCATION_NAMES_CONTENT_URI, null, false);

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
        provider.delete(ChartProviderContract.CHART_CONTENT_URI, null, null);
        syncResult.stats.numDeletes++;
        provider.applyBatch(RpcToDb.chartStructureRpcToDb(conceptList, syncResult));
    }

    private void updateObservations(final ContentProviderClient provider, SyncResult syncResult)
            throws InterruptedException, ExecutionException, RemoteException,
            OperationApplicationException {

        // Get call patients from the cache.
        Uri uri = PatientProviderContract.CONTENT_URI; // Get all entries
        Cursor c = provider.query(
                uri, new String[]{PatientColumns.COLUMN_NAME_UUID}, null, null, null);
        if (c.getCount() < 1) {
            return;
        }

        OpenMrsChartServer chartServer = new OpenMrsChartServer(App.getConnectionDetails());
        // Get the charts asynchronously using volley.
        RequestFuture<PatientChartList> listFuture = RequestFuture.newFuture();

        Log.d(TAG, "requesting all charts");
        chartServer.getAllCharts(listFuture, listFuture);
        ArrayList<String> toDelete = new ArrayList<>();
        ArrayList<ContentValues> toInsert = new ArrayList<>();
        TimingLogger timingLogger = new TimingLogger(TAG, "obs update");
        try {
            Log.d(TAG, "awaiting parsed response");
            PatientChartList patientChartList = listFuture.get(100, TimeUnit.SECONDS);
            Log.d(TAG, "got response ");
            timingLogger.addSplit("Get all charts RPC");
            for (PatientChart patientChart : patientChartList.results) {
                // As we are doing multiple request in parallel, deal with exceptions in the loop.
                timingLogger.addSplit("awaiting future");
                if (patientChart.uuid == null) {
                    Log.e(TAG, "null patient id in observation response");
                    continue;
                }
                // Delete all existing observations for the patient.
                toDelete.add(patientChart.uuid);
                timingLogger.addSplit("added delete to list");
                // Add the new observations
                RpcToDb.observationsRpcToDb(patientChart, syncResult, toInsert);
                timingLogger.addSplit("added obs to list");
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "Error interruption: ", e);
            syncResult.stats.numIoExceptions++;
            return;
        } catch (ExecutionException e){
            Log.e(TAG, "Error failed to execute: ", e);
            syncResult.stats.numIoExceptions++;
            return;
        } catch (Exception e){
            Log.e(TAG, "Error reading from network: ", e);
            syncResult.stats.numIoExceptions++;
            return;
        }
        timingLogger.addSplit("making operations");
        StringBuilder select = new StringBuilder(ChartColumns.PATIENT_UUID);
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
        provider.delete(ChartProviderContract.OBSERVATIONS_CONTENT_URI, select.toString(),
                new String[0]);
        timingLogger.addSplit("batch deletes");
        provider.bulkInsert(ChartProviderContract.OBSERVATIONS_CONTENT_URI,
                toInsert.toArray(new ContentValues[toInsert.size()]));
        timingLogger.addSplit("bulk inserts");
        // Remove all temporary observations now we have the real ones
        provider.delete(ChartProviderContract.OBSERVATIONS_CONTENT_URI,
                ChartColumns.TEMP_CACHE + "!=0",
                new String[0]);
        timingLogger.addSplit("delete temp observations");
        timingLogger.dumpToLog();
    }

    private void updateUsers(final ContentProviderClient provider, SyncResult syncResult) {
        App.getUserManager().syncKnownUsers();
    }
}
