package org.msf.records.sync;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.RequestFuture;

import org.msf.records.App;
import org.msf.records.net.OpenMrsChartServer;
import org.msf.records.net.model.ChartStructure;
import org.msf.records.net.model.ConceptList;
import org.msf.records.net.model.Patient;
import org.msf.records.net.model.PatientChart;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

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
    public static String SYNC_PATIENTS = "SYNC_PATIENTS";

    /**
     * If this key is present with boolean value true then sync concepts.
     */
    public static String SYNC_CONCEPTS = "SYNC_CONCEPTS";

    /**
     * If this key is present with boolean value true then sync the chart structure.
     */
    public static String SYNC_CHART_STRUCTURE = "SYNC_CHART_STRUCTURE";

    /**
     * If this key is present with boolean value true then sync the observations.
     */
    public static String SYNC_OBSERVATIONS = "SYNC_OBSERVATIONS";

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
        Log.i(TAG, "Beginning network synchronization");
        try {
            boolean specific = false;
            if (extras.getBoolean(SYNC_PATIENTS)) {
                specific = true;
                // default behaviour
                updatePatientData(syncResult);
            }
            if (extras.getBoolean(SYNC_CONCEPTS)) {
                specific = true;
                updateConcepts(provider, syncResult);
            }
            if (extras.getBoolean(SYNC_CHART_STRUCTURE)) {
                specific = true;
                updateChartStructure(provider, syncResult);
            }
            if (extras.getBoolean(SYNC_OBSERVATIONS)) {
                specific = true;
                updateObservations(provider, syncResult);
            }
            if (!specific) {
                // If nothing is specified explicitly (such as from the android system menu),
                // do everything.
                updatePatientData(syncResult);
                updateConcepts(provider, syncResult);
                updateChartStructure(provider, syncResult);
                updateObservations(provider, syncResult);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Error in RPC: " + e.toString());
            syncResult.stats.numIoExceptions++;
            return;
        } catch (OperationApplicationException e) {
            Log.e(TAG, "Error updating database: " + e.toString());
            syncResult.databaseError = true;
            return;
        } catch (InterruptedException e){
            Log.e(TAG, "Error interruption: " + e.toString());
            syncResult.stats.numIoExceptions++;
            return;
        } catch (ExecutionException e){
            Log.e(TAG, "Error failed to execute: " + e.toString());
            syncResult.stats.numIoExceptions++;
            return;
        } catch (Exception e){
            Log.e(TAG, "Error reading from network: " + e.toString());
            syncResult.stats.numIoExceptions++;
            return;
        }
        Log.i(TAG, "Network synchronization complete");
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
        Cursor c = contentResolver.query(uri, projection, null, null, null);
        assert c != null;
        Log.i(TAG, "Found " + c.getCount() + " local entries. Computing merge solution...");
        Log.i(TAG, "Found " + patients.size() + " external entries. Computing merge solution...");


        String id;
        String givenName, familyName, uuid, status, locationZone, locationTent, gender;
        int ageMonths = -1, ageYears = -1;
        long admissionTimestamp;

        //iterate through the list of patients
        while(c.moveToNext()){
            syncResult.stats.numEntries++;

            id = c.getString(PatientProjection.COLUMN_ID);
            givenName = c.getString(PatientProjection.COLUMN_GIVEN_NAME);
            familyName = c.getString(PatientProjection.COLUMN_FAMILY_NAME);
            uuid = c.getString(PatientProjection.COLUMN_UUID);
            status = c.getString(PatientProjection.COLUMN_STATUS);
            admissionTimestamp = c.getLong(PatientProjection.COLUMN_ADMISSION_TIMESTAMP);
            locationZone = c.getString(PatientProjection.COLUMN_LOCATION_ZONE);
            locationTent = c.getString(PatientProjection.COLUMN_LOCATION_TENT);
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
                        (patient.status != null && !patient.status.equals(status)) ||
                        (patient.admission_timestamp != null &&
                                !patient.admission_timestamp.equals(admissionTimestamp)) ||
                        (patient.assigned_location.zone != null &&
                            !patient.assigned_location.zone.equals(locationZone)) ||
                        (patient.assigned_location.tent != null &&
                            !patient.assigned_location.tent.equals(locationTent)) ||
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
                            .withValue(PatientColumns.COLUMN_NAME_STATUS, status)
                            .withValue(PatientColumns.COLUMN_NAME_LOCATION_ZONE, status)
                            .withValue(PatientColumns.COLUMN_NAME_ADMISSION_TIMESTAMP, admissionTimestamp)
                            .withValue(PatientColumns.COLUMN_NAME_LOCATION_ZONE, locationZone)
                            .withValue(PatientColumns.COLUMN_NAME_LOCATION_TENT, locationTent)
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
        c.close();


        for (Patient e : patientsMap.values()) {
            Log.i(TAG, "Scheduling insert: entry_id=" + e.id);
            batch.add(ContentProviderOperation.newInsert(PatientProviderContract.CONTENT_URI)
                    .withValue(PatientColumns._ID, e.id)
                    .withValue(PatientColumns.COLUMN_NAME_GIVEN_NAME, e.given_name)
                    .withValue(PatientColumns.COLUMN_NAME_FAMILY_NAME, e.family_name)
                    .withValue(PatientColumns.COLUMN_NAME_UUID, e.uuid)
                    .withValue(PatientColumns.COLUMN_NAME_STATUS, e.status)
                    .withValue(PatientColumns.COLUMN_NAME_ADMISSION_TIMESTAMP, e.admission_timestamp)
                    .withValue(PatientColumns.COLUMN_NAME_LOCATION_ZONE, e.assigned_location.zone)
                    .withValue(PatientColumns.COLUMN_NAME_LOCATION_TENT, e.assigned_location.tent)
                    .withValue(PatientColumns.COLUMN_NAME_AGE_MONTHS, e.age.months)
                    .withValue(PatientColumns.COLUMN_NAME_AGE_YEARS, e.age.years)
                    .withValue(PatientColumns.COLUMN_NAME_GENDER, e.gender)
                    .build());
            syncResult.stats.numInserts++;
        }
        Log.i(TAG, "Merge solution ready. Applying batch update");
        mContentResolver.applyBatch(PatientProviderContract.CONTENT_AUTHORITY, batch);
        mContentResolver.notifyChange(PatientProviderContract.CONTENT_URI, null, false);
        mContentResolver.notifyChange(PatientProviderContract.CONTENT_URI_PATIENT_ZONES, null, false);
        mContentResolver.notifyChange(PatientProviderContract.CONTENT_URI_PATIENT_TENTS, null, false);



        //TODO(giljulio) update the server as well as the client
    }

    private void updateConcepts(final ContentProviderClient provider, SyncResult syncResult)
            throws InterruptedException, ExecutionException, RemoteException,
            OperationApplicationException {
        OpenMrsChartServer chartServer = new OpenMrsChartServer(App.getConnectionDetails());
        RequestFuture<ConceptList> future = RequestFuture.newFuture();
        chartServer.getConcepts(future, future); // errors handled by caller
        ConceptList conceptList = future.get();
        provider.applyBatch(ChartRpcToDb.conceptRpcToDb(conceptList, syncResult));
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
        provider.applyBatch(ChartRpcToDb.chartStructureRpcToDb(conceptList, syncResult));
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
        ArrayList<RequestFuture<PatientChart>> futures = new ArrayList<>();
        while (c.moveToNext()) {
            RequestFuture<PatientChart> future = RequestFuture.newFuture();
            chartServer.getChart(c.getString(c.getColumnIndex(PatientColumns.COLUMN_NAME_UUID)),
                    future, future);
            futures.add(future);
        }
        for (RequestFuture<PatientChart> future : futures) {
            // As we are doing multiple request in parallel, deal with exceptions in the loop.
            try {
                PatientChart patientChart = future.get();
                if (patientChart.uuid == null) {
                    Log.e(TAG, "null patient id in observation response");
                    continue;
                }
                // Delete all existing observations for the patient.
                provider.delete(ChartProviderContract.OBSERVATIONS_CONTENT_URI,
                        ChartColumns.PATIENT_UUID + "=?",
                        new String[]{patientChart.uuid});
                // Add the new observations
                provider.applyBatch(ChartRpcToDb.observationsRpcToDb(patientChart, syncResult));
            } catch (InterruptedException e) {
                Log.e(TAG, "Error interruption: " + e.toString());
                syncResult.stats.numIoExceptions++;
                return;
            } catch (ExecutionException e){
                Log.e(TAG, "Error failed to execute: " + e.toString());
                syncResult.stats.numIoExceptions++;
                return;
            } catch (Exception e){
                Log.e(TAG, "Error reading from network: " + e.toString());
                syncResult.stats.numIoExceptions++;
                return;
            }
        }
    }
}
