package org.projectbuendia.client.sync.controllers;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.SyncResult;

/**
 * A SyncPhaseRunnable executes the unit of work for a specific sync phase. Each
 * SyncPhaseRunnable should be a lightweight object that does all work in the {@link
 * #sync(ContentResolver, SyncResult, ContentProviderClient)} method.
 */
public interface SyncPhaseRunnable {
    // TODO: Replace `throws Throwable` with something more focussed.
    void sync(
            ContentResolver contentResolver,
            SyncResult syncResult,
            ContentProviderClient providerClient)
            throws Throwable;
}
