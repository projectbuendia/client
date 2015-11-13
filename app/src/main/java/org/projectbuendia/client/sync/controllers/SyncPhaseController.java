package org.projectbuendia.client.sync.controllers;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.SyncResult;

/**
 * A SyncPhaseController executes the unit of work for a specific sync phase. Each
 * SyncPhaseController should be a lightweight object that does all work in the {@link
 * #sync(ContentResolver, SyncResult, ContentProviderClient)} method. Think of it like you would
 * think of a {@link Runnable}.
 */
public interface SyncPhaseController {
    // TODO: Replace `throws Throwable` with something more focussed.
    void sync(
            ContentResolver contentResolver,
            SyncResult syncResult,
            ContentProviderClient providerClient)
            throws Throwable;
}
