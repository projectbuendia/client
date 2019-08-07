package org.projectbuendia.client.sync.controllers;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.SyncResult;

/**
 * A SyncWorker executes the work for a specific sync phase.  First, initialize()
 * is called; then sync() is called repeatedly until it returns true to indicate
 * completion; and finally finalize() is called.
 *
 * An exception thrown in any of the three methods aborts the entire sync,
 * skipping any remaining phases.
 */
public interface SyncWorker {
    // TODO: Replace Throwable with something more specific.

    default void initialize(
        ContentResolver resolver, SyncResult result, ContentProviderClient client
    ) throws Throwable { }

    boolean sync(
        ContentResolver resolver, SyncResult result, ContentProviderClient client
    ) throws Throwable;

    default void finalize(
        ContentResolver resolver, SyncResult result, ContentProviderClient client
    ) throws Throwable { }
}
