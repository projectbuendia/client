package org.projectbuendia.client.sync.controllers;

import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;

import com.android.volley.toolbox.RequestFuture;

import org.projectbuendia.client.App;
import org.projectbuendia.client.json.JsonForm;
import org.projectbuendia.client.models.Form;
import org.projectbuendia.client.providers.Contracts;
import org.projectbuendia.client.utils.Logger;
import org.projectbuendia.client.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Syncs forms from the server to the local database. Note that this is a full sync every time;
 * but seeing as the size of the dataset is small, this shouldn't have a significant performance
 * impact.
 */
public class FormsSyncController implements SyncController {
    private static final Logger LOG = Logger.create();

    @Override
    public void sync(
            SyncResult syncResult,
            ContentProviderClient providerClient,
            ContentResolver contentResolver) throws Throwable {
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        ops.addAll(getFormUpdateOps(syncResult));
        contentResolver.applyBatch(Contracts.CONTENT_AUTHORITY, ops);
        LOG.i("Finished updating forms (" + ops.size() + " db ops)");
        contentResolver.notifyChange(Contracts.Forms.CONTENT_URI, null, false);
    }

    private static List<ContentProviderOperation> getFormUpdateOps(SyncResult syncResult)
            throws ExecutionException, InterruptedException {
        LOG.i("Listing all forms on server");
        RequestFuture<List<JsonForm>> future = RequestFuture.newFuture();
        App.getServer().listForms(future, future);
        Map<String, ContentValues> cvs = new HashMap<>();
        for (JsonForm form : future.get()) {
            cvs.put(form.id, Form.fromJson(form).toContentValues());
        }

        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        final ContentResolver resolver = App.getInstance().getContentResolver();
        Cursor c = resolver.query(Contracts.Forms.CONTENT_URI, null, null, null, null);
        LOG.i("Examining forms: " + c.getCount() + " local, " + cvs.size() + " from server");
        try {
            while (c.moveToNext()) {
                String uuid = Utils.getString(c, Contracts.Forms.UUID);
                Uri uri = Contracts.Forms.CONTENT_URI.buildUpon().appendPath(uuid).build();
                LOG.i("  - will delete form " + uuid);
                ops.add(ContentProviderOperation.newDelete(uri).build());
            }
        } finally {
            c.close();
        }

        for (ContentValues values : cvs.values()) {  // server has a new record
            LOG.i("  - will insert form " + values.getAsString(Contracts.Forms.UUID));
            ops.add(ContentProviderOperation.newInsert(Contracts.Forms.CONTENT_URI).withValues(values).build());
            syncResult.stats.numInserts++;
        }
        return ops;
    }
}
