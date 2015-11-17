package org.projectbuendia.client.sync.controllers;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.os.RemoteException;

import org.projectbuendia.client.App;
import org.projectbuendia.client.user.UserManager;

import java.util.concurrent.ExecutionException;

/**
 * Handles syncing users. This logic always fetches all users, which is okay because the set of
 * users is fairly small.
 */
public class UsersSyncPhaseRunnable implements SyncPhaseRunnable {
    @Override
    public void sync(ContentResolver contentResolver, SyncResult syncResult, ContentProviderClient providerClient)
            throws InterruptedException, ExecutionException, UserManager.UserSyncException,
            RemoteException, OperationApplicationException {
        App.getUserManager().syncKnownUsersSynchronously();
    }
}
