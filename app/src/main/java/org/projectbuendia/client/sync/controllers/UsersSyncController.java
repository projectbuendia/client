package org.projectbuendia.client.sync.controllers;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.SyncResult;

import org.projectbuendia.client.App;

/**
 * Syncs users from the server to the local database.
 */
public class UsersSyncController implements SyncController {
    @Override
    public void sync(
            SyncResult syncResult,
            ContentProviderClient providerClient,
            ContentResolver contentResolver) throws Throwable {
        App.getUserManager().syncKnownUsersSynchronously();
    }
}
