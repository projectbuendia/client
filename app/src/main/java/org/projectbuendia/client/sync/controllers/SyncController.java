package org.projectbuendia.client.sync.controllers;

import android.content.ContentProvider;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.SyncResult;

/**
 * A {@link SyncController} performs synchronization for an individual synchronization phase.
 */
public interface SyncController {
    void sync(
            SyncResult syncResult,
            ContentProviderClient providerClient,
            ContentResolver contentResolver) throws Throwable;
}
