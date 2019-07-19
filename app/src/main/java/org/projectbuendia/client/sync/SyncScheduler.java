package org.projectbuendia.client.sync;

import android.os.Bundle;

/** SyncManager-facing interface for scheduling and cancelling sync operations. */
public interface SyncScheduler<T> {
    /** Queues a request for a sync to begin when the sync engine is available. */
    void requestSync(Bundle options);

    /** Aborts any sync in progress and cancels any currently queued requests. */
    void stopSyncing();

    /**
     * Starts, changes, or stops a periodic sync schedule.  There can be at most
     * one such repeating loop for each set of options; if this bundle of options
     * has the same contents as that from a previous call, the repeating loop set
     * by the previous call is terminated and a new loop is started with the given
     * period.  When a loop starts, the first sync occurs after the first period
     * has elapsed.  Specifying a period of zero stops the loop.
     */
    void setPeriodicSync(int periodSec, Bundle options);

    /** Returns true if a sync is currently running or pending on the queue. */
    boolean isRunningOrPending();
}
