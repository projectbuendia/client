package org.projectbuendia.client.sync.controllers;

import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.SyncResult;
import android.net.Uri;

import com.android.volley.toolbox.RequestFuture;

import org.projectbuendia.client.App;
import org.projectbuendia.client.json.JsonPatient;
import org.projectbuendia.client.json.JsonPatientsResponse;
import org.projectbuendia.client.models.Patient;
import org.projectbuendia.client.providers.Contracts;
import org.projectbuendia.client.sync.SyncAdapter;
import org.projectbuendia.client.utils.Logger;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

/**
 * Handles syncing patients. Uses an incremental sync mechanism.
 */
// TODO: wrap this class in something that handles the core incremental sync behaviour.
public class PatientsSyncPhaseController implements SyncPhaseController {
    private static final Logger LOG = Logger.create();

    @Override
    public void sync(ContentResolver contentResolver, SyncResult syncResult, ContentProviderClient providerClient) throws Throwable {
        String syncToken = SyncAdapter.getLastSyncToken(providerClient, Contracts.Table.PATIENTS);
        RequestFuture<JsonPatientsResponse> future = RequestFuture.newFuture();
        App.getServer().listPatients(syncToken, future, future);
        JsonPatientsResponse response = future.get();
        ArrayList<ContentProviderOperation> ops = getPatientUpdateOps(response.results, syncResult);
        providerClient.applyBatch(ops);
        LOG.i("Finished updating patients (" + ops.size() + " db ops)");
        contentResolver.notifyChange(Contracts.Patients.CONTENT_URI, null, false);

        SyncAdapter.storeSyncToken(providerClient, Contracts.Table.PATIENTS, response.snapshotTime);
    }

    /**
     * Downloads all patients from the server and produces a list of the operations
     * needed to bring the local database in sync with the server.
     */
    private static ArrayList<ContentProviderOperation> getPatientUpdateOps(
            JsonPatient[] patients, SyncResult syncResult)
            throws ExecutionException, InterruptedException {
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        for (JsonPatient patient : patients) {
            if (patient.voided) {
                syncResult.stats.numDeletes++;
                ops.add(makeDeleteOpForPatientUuid(patient.uuid));
            } else {
                syncResult.stats.numInserts++;
                ops.add(makeInsertOpForPatient(patient));
            }
        }

        return ops;
    }

    private static ContentProviderOperation makeInsertOpForPatient(JsonPatient patient) {
        return ContentProviderOperation.newInsert(Contracts.Patients.CONTENT_URI)
                .withValues(Patient.fromJson(patient).toContentValues()).build();
    }

    private static ContentProviderOperation makeDeleteOpForPatientUuid(String uuid) {
        Uri uri = Contracts.Patients.CONTENT_URI.buildUpon().appendPath(uuid).build();
        return ContentProviderOperation.newDelete(uri).build();
    }
}
