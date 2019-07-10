package org.projectbuendia.client.sync;

import android.content.ContentProviderClient;
import android.content.SyncResult;
import android.os.Bundle;

/** Performs the heavy lifting of a sync (network and database operations). */
interface SyncEngine {
    void sync(Bundle options, ContentProviderClient client, SyncResult result);
    void cancel();
}
