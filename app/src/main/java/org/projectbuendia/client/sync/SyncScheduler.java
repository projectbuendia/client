package org.projectbuendia.client.sync;

import android.os.Bundle;

/** SyncManager-facing interface for scheduling and cancelling sync operations. */
public interface SyncScheduler {
    /** Queues a request for a sync to begin when the sync engine is available. */
    void requestSync(Bundle options);

    /** Aborts any sync in progress and cancels any currently queued requests. */
    void stopSyncing();

    /**
     * Starts, changes, or stops the periodic sync schedule.  There can be at most
     * one such repeating loop; any periodic sync from a previous call is replaced
     * with this new one.  Specifying a period of zero stops the periodic sync.
     */
    void setPeriodicSync(Bundle options, int periodSec);

    /** Returns true if a sync is currently running or pending on the queue. */
    boolean isRunningOrPending();
}
