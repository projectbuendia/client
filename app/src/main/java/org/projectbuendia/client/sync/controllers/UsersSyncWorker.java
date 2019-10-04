package org.projectbuendia.client.sync.controllers;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.os.RemoteException;

import org.projectbuendia.client.App;
import org.projectbuendia.client.user.UserManager;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * Handles syncing users. This logic always fetches all users, which is okay because the set of
 * users is fairly small.
 */
public class UsersSyncWorker implements SyncWorker {
    @Override public boolean sync(
        ContentResolver resolver, SyncResult result, ContentProviderClient client
    ) throws InterruptedException, ExecutionException, TimeoutException,
        UserManager.UserSyncException, RemoteException, OperationApplicationException {
        App.getUserManager().syncKnownUsersSynchronously();
        return true;
    }
}
