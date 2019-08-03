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
import org.projectbuendia.client.ui.OdkActivityLauncher;
import org.projectbuendia.client.utils.Logger;
import org.projectbuendia.client.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Handles syncing forms. All forms are always fetched, which is okay because there are only a few
 * forms; usually less than 10.
 */
public class FormsSyncWorker implements SyncWorker {
    private static final Logger LOG = Logger.create();
    private static boolean isDisabled = false;
    private static final Object lock = new Object();

    @Override public boolean sync(
        ContentResolver resolver, SyncResult result, ContentProviderClient client
    ) throws Throwable {
        synchronized (lock) {
            if (isDisabled) {
                LOG.w("Form sync is temporarily disabled; skipping this phase");
                return true;
            }

            ArrayList<ContentProviderOperation> ops = new ArrayList<>();
            ops.addAll(getFormUpdateOps(result));
            client.applyBatch(ops);
            LOG.i("Finished updating forms (" + ops.size() + " db ops)");
            resolver.notifyChange(Contracts.Forms.URI, null, false);

            OdkActivityLauncher.fetchAndCacheAllXforms();
            return true;
        }
    }

    public static void setDisabled(boolean newValue) {
        LOG.i("Received a request to %s form sync", newValue ? "disable" : "enable");
        synchronized (lock) {
            isDisabled = newValue;
            LOG.w("Form sync is now %s", isDisabled ? "disabled" : "enabled");
        }
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
        Cursor c = resolver.query(Contracts.Forms.URI, new String[] {Contracts.Forms.UUID},
                null, null, null);
        LOG.i("Examining forms: " + c.getCount() + " local, " + cvs.size() + " from server");
        try {
            while (c.moveToNext()) {
                String uuid = Utils.getString(c, Contracts.Forms.UUID);
                Uri uri = Contracts.Forms.URI.buildUpon().appendPath(uuid).build();
                LOG.i("  - will delete form " + uuid);
                ops.add(ContentProviderOperation.newDelete(uri).build());
            }
        } finally {
            c.close();
        }

        for (ContentValues values : cvs.values()) {  // server has a new record
            LOG.i("  - will insert form " + values.getAsString(Contracts.Forms.UUID));
            ops.add(ContentProviderOperation.newInsert(Contracts.Forms.URI).withValues(values).build());
            syncResult.stats.numInserts++;
        }
        return ops;
    }
}
