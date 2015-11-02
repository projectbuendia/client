package org.projectbuendia.client.sync.controllers;

import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.SyncResult;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;

import com.android.volley.toolbox.RequestFuture;

import org.projectbuendia.client.App;
import org.projectbuendia.client.json.JsonPatient;
import org.projectbuendia.client.models.Patient;
import org.projectbuendia.client.providers.Contracts;
import org.projectbuendia.client.sync.DbSyncHelper;
import org.projectbuendia.client.utils.Logger;
import org.projectbuendia.client.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Syncs patients from the server to the local database.
 */
public class PatientsSyncController implements SyncController {
    private static final Logger LOG = Logger.create();

    @Override
    public void sync(
            SyncResult syncResult,
            ContentProviderClient providerClient,
            ContentResolver contentResolver) throws Throwable {
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        ops.addAll(getPatientUpdateOps(syncResult));
        contentResolver.applyBatch(Contracts.CONTENT_AUTHORITY, ops);
        LOG.i("Finished updating patients (" + ops.size() + " db ops)");
        contentResolver.notifyChange(Contracts.Patients.CONTENT_URI, null, false);
    }

    /**
     * Downloads all patients from the server and produces a list of the operations
     * needed to bring the local database in sync with the server.
     */
    private static List<ContentProviderOperation> getPatientUpdateOps(SyncResult syncResult)
            throws ExecutionException, InterruptedException {
        final ContentResolver resolver = App.getInstance().getContentResolver();

        RequestFuture<List<JsonPatient>> future = RequestFuture.newFuture();
        App.getServer().listPatients("", "", "", future, future);
        Map<String, ContentValues> cvs = new HashMap<>();
        for (JsonPatient patient : future.get()) {
            cvs.put(patient.id, Patient.fromJson(patient).toContentValues());
        }

        Cursor c = resolver.query(Contracts.Patients.CONTENT_URI, null, null, null, null);
        LOG.i("Examining patients: " + c.getCount() + " local, " + cvs.size() + " from server");

        List<ContentProviderOperation> ops = new ArrayList<>();
        try {
            while (c.moveToNext()) {
                String uuid = Utils.getString(c, Contracts.Patients.UUID);
                Uri uri = Contracts.Patients.CONTENT_URI.buildUpon().appendPath(uuid).build();
                syncResult.stats.numEntries++;

                ContentValues cv = cvs.remove(uuid);
                if (cv != null) {
                    ContentValues localCv = new ContentValues();
                    DatabaseUtils.cursorRowToContentValues(c, localCv);
                    if (!cv.equals(localCv)) {  // record has changed on server
                        LOG.i("  - will update patient " + uuid);
                        ops.add(ContentProviderOperation.newUpdate(uri).withValues(cv).build());
                        syncResult.stats.numUpdates++;
                    }
                } else {  // record doesn't exist on server
                    LOG.i("  - will delete patient " + uuid);
                    ops.add(ContentProviderOperation.newDelete(uri).build());
                    syncResult.stats.numDeletes++;
                }
            }
        } finally {
            c.close();
        }

        for (ContentValues values : cvs.values()) {  // server has a new record
            LOG.i("  - will insert patient " + values.getAsString(Contracts.Patients.UUID));
            ops.add(ContentProviderOperation.newInsert(Contracts.Patients.CONTENT_URI).withValues(values).build());
            syncResult.stats.numInserts++;
        }

        return ops;
    }
}
