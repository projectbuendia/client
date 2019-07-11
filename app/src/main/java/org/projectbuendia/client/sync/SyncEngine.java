package org.projectbuendia.client.sync;

import android.content.ContentProviderClient;
import android.content.SyncResult;
import android.os.Bundle;

/** Performs the heavy lifting of a sync (network and database operations). */
interface SyncEngine {
    /** Runs a sync as a long blocking operation. */
    void sync(Bundle options, ContentProviderClient client, SyncResult result);

    /** Called by other threads to request cancellation of any currently running sync. */
    void cancel();
}
