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
import org.msf.records.model.Patient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Created by Gil on 21/11/14.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {

    private static final String TAG = SyncAdapter.class.getSimpleName();

    /**
     * Project used when querying content provider. Returns all known fields.
     */
    private static final String[] PROJECTION = new String[] {
            PatientContract.PatientMeta._ID,
            PatientContract.PatientMeta.COLUMN_NAME_GIVEN_NAME,
            PatientContract.PatientMeta.COLUMN_NAME_FAMILY_NAME,
            PatientContract.PatientMeta.COLUMN_NAME_UUID,
            PatientContract.PatientMeta.COLUMN_NAME_STATUS,
            PatientContract.PatientMeta.COLUMN_NAME_ADMISSION_TIMESTAMP,
            PatientContract.PatientMeta.COLUMN_NAME_LOCATION_ZONE
    };

    // Constants representing column positions from PROJECTION.
    public static final int COLUMN_ID = 0;
    public static final int COLUMN_GIVEN_NAME = 1;
    public static final int COLUMN_FAMILY_NAME = 2;
    public static final int COLUMN_UUID = 3;
    public static final int COLUMN_STATUS = 4;
    public static final int COLUMN_ADMISSION_TIMESTAMP = 5;
    public static final int COLUMN_LOCATION_ZONE = 6;


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
            updatePatientData(syncResult);
        } catch (RemoteException e) {
            Log.e(TAG, "Error updating database: " + e.toString());
            syncResult.databaseError = true;
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
        Uri uri = PatientContract.PatientMeta.CONTENT_URI; // Get all entries
        Cursor c = contentResolver.query(uri, PROJECTION, null, null, null);
        assert c != null;
        Log.i(TAG, "Found " + c.getCount() + " local entries. Computing merge solution...");
        Log.i(TAG, "Found " + patients.size() + " external entries. Computing merge solution...");


        String id;
        String givenName, familyName, uuid, status, locationZone;
        long admissionTimestamp;

        //iterate through the list of patients
        while(c.moveToNext()){
            syncResult.stats.numEntries++;

            id = c.getString(COLUMN_ID);
            givenName = c.getString(COLUMN_GIVEN_NAME);
            familyName = c.getString(COLUMN_FAMILY_NAME);
            uuid = c.getString(COLUMN_UUID);
            status = c.getString(COLUMN_STATUS);
            admissionTimestamp = c.getLong(COLUMN_ADMISSION_TIMESTAMP);
            locationZone = c.getString(COLUMN_LOCATION_ZONE);

            Patient patient = patientsMap.get(id);
            if (patient != null) {
                // Entry exists. Remove from entry map to prevent insert later.
                patientsMap.remove(id);
                // Check to see if the entry needs to be updated
                Uri existingUri = PatientContract.PatientMeta.CONTENT_URI.buildUpon().appendPath(String.valueOf(id)).build();

                //check if it needs updating
                if ((patient.given_name != null && !patient.given_name.equals(givenName)) ||
                        (patient.family_name != null && !patient.family_name.equals(familyName)) ||
                        (patient.uuid != null && !patient.uuid.equals(uuid)) ||
                        (patient.status != null && !patient.status.equals(status)) ||
                        (patient.admission_timestamp != null &&
                                !patient.admission_timestamp.equals(admissionTimestamp)) ||
                        (patient.assigned_location.zone != null &&
                            !patient.assigned_location.zone.equals(locationZone))) {
                    // Update existing record
                    Log.i(TAG, "Scheduling update: " + existingUri);
                    batch.add(ContentProviderOperation.newUpdate(existingUri)
                            .withValue(PatientContract.PatientMeta.COLUMN_NAME_GIVEN_NAME, givenName)
                            .withValue(PatientContract.PatientMeta.COLUMN_NAME_FAMILY_NAME, familyName)
                            .withValue(PatientContract.PatientMeta.COLUMN_NAME_UUID, uuid)
                            .withValue(PatientContract.PatientMeta.COLUMN_NAME_STATUS, status)
                            .withValue(PatientContract.PatientMeta.COLUMN_NAME_LOCATION_ZONE, status)
                            .withValue(PatientContract.PatientMeta.COLUMN_NAME_ADMISSION_TIMESTAMP, admissionTimestamp)
                            .withValue(PatientContract.PatientMeta.COLUMN_NAME_LOCATION_ZONE, locationZone)
                            .build());
                    syncResult.stats.numUpdates++;
                } else {
                    Log.i(TAG, "No action required for " + existingUri);
                }
            } else {
                // Entry doesn't exist. Remove it from the database.
                Uri deleteUri = PatientContract.PatientMeta.CONTENT_URI.buildUpon()
                        .appendPath(id).build();
                Log.i(TAG, "Scheduling delete: " + deleteUri);
                batch.add(ContentProviderOperation.newDelete(deleteUri).build());
                syncResult.stats.numDeletes++;
            }
        }
        c.close();


        for (Patient e : patientsMap.values()) {
            Log.i(TAG, "Scheduling insert: entry_id=" + e.id);
            batch.add(ContentProviderOperation.newInsert(PatientContract.PatientMeta.CONTENT_URI)
                    .withValue(PatientContract.PatientMeta._ID, e.id)
                    .withValue(PatientContract.PatientMeta.COLUMN_NAME_GIVEN_NAME, e.given_name)
                    .withValue(PatientContract.PatientMeta.COLUMN_NAME_FAMILY_NAME, e.family_name)
                    .withValue(PatientContract.PatientMeta.COLUMN_NAME_UUID, e.uuid)
                    .withValue(PatientContract.PatientMeta.COLUMN_NAME_STATUS, e.status)
                    .withValue(PatientContract.PatientMeta.COLUMN_NAME_ADMISSION_TIMESTAMP, e.admission_timestamp)
                    .withValue(PatientContract.PatientMeta.COLUMN_NAME_LOCATION_ZONE, e.assigned_location.zone)
                    .build());
            syncResult.stats.numInserts++;
        }
        Log.i(TAG, "Merge solution ready. Applying batch update");
        mContentResolver.applyBatch(PatientContract.CONTENT_AUTHORITY, batch);
        mContentResolver.notifyChange(PatientContract.PatientMeta.CONTENT_URI, null, false);
        mContentResolver.notifyChange(PatientContract.PatientMeta.CONTENT_URI_PATIENT_ZONES, null, false);




        //TODO(giljulio) update the server as well as the client
    }

}
