package org.msf.records.sync;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
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
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import org.msf.records.App;
import org.msf.records.net.OpenMrsChartServer;
import org.msf.records.net.model.ChartStructure;
import org.msf.records.net.model.ConceptList;
import org.msf.records.net.model.Location;
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
     * If this key is present with boolean value true then sync locations.
     */
    public static String SYNC_LOCATIONS = "SYNC_LOCATIONS";

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
            if (extras.getBoolean(SYNC_LOCATIONS)) {
                specific = true;
                updateLocations(provider, syncResult);
            }
            if (!specific) {
                // If nothing is specified explicitly (such as from the android system menu),
                // do everything.
                updatePatientData(syncResult);
                updateConcepts(provider, syncResult);
                updateChartStructure(provider, syncResult);
                updateObservations(provider, syncResult);
                updateLocations(provider, syncResult);
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

    private void updateLocations(final ContentProviderClient provider, SyncResult syncResult)
            throws InterruptedException, ExecutionException, RemoteException,
            OperationApplicationException {
        final ContentResolver contentResolver = getContext().getContentResolver();

        final String[] projection = LocationProjection.getLocationProjection();
        final String[] namesProjection = LocationProjection.getLocationNamesProjection();

        Log.d(TAG, "Before network call");
        RequestFuture<List<Location>> future = RequestFuture.newFuture();
        App.getServer().listLocations(future, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, error.toString());
            }
        }, TAG);

        //No need for callbacks as the {@AbstractThreadedSyncAdapter} code is executed in a background thread
        List<Location> locations = future.get();
        Log.d(TAG, "After network call");
        ArrayList<ContentProviderOperation> batch = new ArrayList<>();


        HashMap<String, Location> locationsMap = new HashMap<>();
        for (Location location : locations) {
            locationsMap.put(location.uuid, location);
        }

        // Get list of all items
        Log.i(TAG, "Fetching local entries for merge");
        Uri uri = LocationProviderContract.LOCATIONS_CONTENT_URI; // Location tree
        Uri namesUri = LocationProviderContract.LOCATION_NAMES_CONTENT_URI; // Location names
        Cursor c = contentResolver.query(uri, projection, null, null, null);
        assert c != null;
        Cursor namesCur = contentResolver.query(namesUri, namesProjection, null, null, null);
        assert namesCur != null;
        Log.i(TAG, "Found " + c.getCount() + " local entries. Computing merge solution...");
        Log.i(TAG, "Found " + locations.size() + " external entries. Computing merge solution...");

        String id, parentId;

        // Build map of location names from the database.
        HashMap<String, HashMap<String, String>> dbLocationNames =
                new HashMap<String, HashMap<String, String>>();
        while (namesCur.moveToNext()) {
            String locationId = namesCur.getString(
                    LocationProjection.LOCATION_NAME_LOCATION_UUID_COLUMN);
            String locale = namesCur.getString(LocationProjection.LOCATION_NAME_LOCALE_COLUMN);
            String name = namesCur.getString(LocationProjection.LOCATION_NAME_NAME_COLUMN);
            if (locationId == null || locale == null || name == null) {
                continue;
            }

            if (!dbLocationNames.containsKey(locationId)) {
                dbLocationNames.put(locationId, new HashMap<String, String>());
            }

            dbLocationNames.get(locationId).put(locale, name);
        }
        namesCur.close();

        // Iterate through the list of locations
        while(c.moveToNext()){
            syncResult.stats.numEntries++;

            id = c.getString(LocationProjection.LOCATION_LOCATION_UUID_COLUMN);
            parentId = c.getString(LocationProjection.LOCATION_PARENT_UUID_COLUMN);

            Location location = locationsMap.get(id);
            if (location != null) {
                // Entry exists. Remove from entry map to prevent insert later.
                locationsMap.remove(id);

                // Grab the names stored in the database for this location.
                HashMap<String, String> locationNames = dbLocationNames.get(id);

                // Check to see if the entry needs to be updated
                Uri existingUri = uri.buildUpon().appendPath(String.valueOf(id)).build();

                if (location.parent_uuid != null && !location.parent_uuid.equals(parentId)) {
                    // Update existing record
                    Log.i(TAG, "Scheduling update: " + existingUri);
                    batch.add(ContentProviderOperation.newUpdate(existingUri)
                            .withValue(LocationProviderContract.LocationColumns.LOCATION_UUID, id)
                            .withValue(LocationProviderContract.LocationColumns.PARENT_UUID, parentId)
                            .build());
                    syncResult.stats.numUpdates++;
                } else {
                    Log.i(TAG, "No action required for " + existingUri);
                }

                if (location.names != null &&
                        (locationNames == null || !location.names.equals(locationNames))) {
                    Uri existingNamesUri = namesUri.buildUpon().appendPath(
                            String.valueOf(id)).build();
                    // Update location names by deleting any existing location names and
                    // repopulating.
                    Log.i(TAG, "Scheduling location names update: " + existingNamesUri);
                    batch.add(ContentProviderOperation.newDelete(existingNamesUri).build());
                    syncResult.stats.numDeletes++;
                    for (String locale : location.names.keySet()) {
                        batch.add(ContentProviderOperation.newInsert(existingNamesUri)
                                .withValue(
                                        LocationProviderContract.LocationColumns.LOCATION_UUID, id)
                                .withValue(
                                        LocationProviderContract.LocationColumns.LOCALE, locale)
                                .withValue(
                                        LocationProviderContract.LocationColumns.NAME,
                                        location.names.get(locale))
                                .build());
                        syncResult.stats.numInserts++;
                    }
                }
            } else {
                // Entry doesn't exist. Remove it from the database.
                Uri deleteUri = uri.buildUpon().appendPath(id).build();
                Uri namesDeleteUri = namesUri.buildUpon().appendPath(id).build();
                Log.i(TAG, "Scheduling delete: " + deleteUri);
                batch.add(ContentProviderOperation.newDelete(deleteUri).build());
                syncResult.stats.numDeletes++;
                Log.i(TAG, "Scheduling delete: " + namesDeleteUri);
                batch.add(ContentProviderOperation.newDelete(namesDeleteUri).build());
                syncResult.stats.numDeletes++;
            }
        }
        c.close();


        for (Location e : locationsMap.values()) {
            Log.i(TAG, "Scheduling insert: entry_id=" + e.uuid);
            batch.add(ContentProviderOperation.newInsert(LocationProviderContract.LOCATIONS_CONTENT_URI)
                    .withValue(LocationProviderContract.LocationColumns.LOCATION_UUID, e.uuid)
                    .withValue(LocationProviderContract.LocationColumns.PARENT_UUID, e.parent_uuid)
                    .build());
            syncResult.stats.numInserts++;

            for (String locale : e.names.keySet()) {
                Uri existingNamesUri = namesUri.buildUpon().appendPath(
                        String.valueOf(e.uuid)).build();
                batch.add(ContentProviderOperation.newInsert(existingNamesUri)
                        .withValue(
                                LocationProviderContract.LocationColumns.LOCATION_UUID, e.uuid)
                        .withValue(
                                LocationProviderContract.LocationColumns.LOCALE, locale)
                        .withValue(
                                LocationProviderContract.LocationColumns.NAME,
                                e.names.get(locale))
                        .build());
                syncResult.stats.numInserts++;
            }
        }
        Log.i(TAG, "Merge solution ready. Applying batch update");
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
