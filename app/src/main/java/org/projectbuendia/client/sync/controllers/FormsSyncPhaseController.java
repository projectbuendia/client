package org.projectbuendia.client.sync.controllers;

import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.SyncResult;

import org.projectbuendia.client.providers.Contracts;
import org.projectbuendia.client.sync.DbSyncHelper;
import org.projectbuendia.client.utils.Logger;

import java.util.ArrayList;

/**
 * Handles syncing forms. All forms are always fetched, which is okay because there are only a few
 * forms; usually less than 10.
 */
public class FormsSyncPhaseController implements SyncPhaseController {
    private static final Logger LOG = Logger.create();

    @Override
    public void sync(ContentResolver contentResolver, SyncResult syncResult,
            ContentProviderClient providerClient)
            throws Throwable {
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        ops.addAll(DbSyncHelper.getFormUpdateOps(syncResult));
        providerClient.applyBatch(ops);
        LOG.i("Finished updating forms (" + ops.size() + " db ops)");
        contentResolver.notifyChange(Contracts.Forms.CONTENT_URI, null, false);
    }
}
