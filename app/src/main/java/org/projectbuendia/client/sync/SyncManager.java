// Copyright 2015 The Project Buendia Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at: http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distrib-
// uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
// specific language governing permissions and limitations under the License.

package org.projectbuendia.client.sync;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import org.projectbuendia.client.events.sync.SyncCanceledEvent;
import org.projectbuendia.client.events.sync.SyncFailedEvent;
import org.projectbuendia.client.events.sync.SyncProgressEvent;
import org.projectbuendia.client.events.sync.SyncStartedEvent;
import org.projectbuendia.client.events.sync.SyncSucceededEvent;
import org.projectbuendia.client.sync.BuendiaSyncEngine.SyncOption;
import org.projectbuendia.client.sync.BuendiaSyncEngine.SyncPhase;
import org.projectbuendia.client.utils.Logger;

import de.greenrobot.event.EventBus;

/** Provides app-facing methods for requesting and cancelling sync operations. */
public class SyncManager {
    private static final Logger LOG = Logger.create();

    static final String SYNC_STATUS = "sync-status";
    static final int STARTED = 1;
    static final int COMPLETED = 2;
    static final int FAILED = 3;
    static final int IN_PROGRESS = 4;
    static final int CANCELED = 5;
    /**
     * Intent extras using this key are integers representing the sync progress completed so far,
     * as a percentage.
     */
    static final String SYNC_PROGRESS = "sync-progress";
    /**
     * Intent extras using this key are nullable strings representing the current sync status.
     * They are localized and are suitable for presentation to the user.
     */
    static final String SYNC_PROGRESS_LABEL = "sync-progress-label";

    private final SyncScheduler mScheduler;

    public SyncManager(SyncScheduler scheduler) {
        mScheduler = scheduler;
    }

    /** Cancels an in-flight, non-periodic sync. */
    public void cancelOnDemandSync() {
        mScheduler.stopSyncing();

        // If sync was pending, it should now be idle and we can consider the sync immediately canceled.
        if (!isSyncRunningOrPending()) {
            LOG.i("Sync was canceled before it began -- immediately firing SyncCanceledEvent.");
            EventBus.getDefault().post(new SyncCanceledEvent());
        }
    }

    public boolean isSyncRunningOrPending() {
        return mScheduler.isRunningOrPending();
    }

    /** Starts a full sync as soon as possible. */
    public void startFullSync() {
        Bundle options = new Bundle();
        // Request aggressively that the sync should start straight away.
        options.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        options.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);

        // Fetch everything
        options.putBoolean(SyncOption.FULL_SYNC.name(), true);
        LOG.i("Requesting full sync");
        mScheduler.requestSync(options);
    }

    /**
     * Starts, changes, or stops the periodic sync schedule.  There can be at most
     * one such repeating loop; any periodic sync from a previous call is replaced
     * with this new one.  Specifying a period of zero stops the periodic sync.
     */
    public void setPeriodicSync(Bundle options, int periodSec) {
        mScheduler.setPeriodicSync(options, periodSec);
    }

    /** Starts a sync of only observations and orders. */
    public void startObservationsAndOrdersSync() {
        // Start by canceling any existing syncs, which may delay this one.
        mScheduler.stopSyncing();

        Bundle options = new Bundle();
        // Request aggressively that the sync should start straight away.
        options.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        options.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);

        // Fetch just the newly added observations.
        options.putBoolean(SyncPhase.SYNC_OBSERVATIONS.name(), true);
        options.putBoolean(SyncPhase.SYNC_ORDERS.name(), true);

        LOG.i("Requesting incremental observations / orders sync");
        mScheduler.requestSync(options);
    }

    /** Listens for sync status events that are broadcast by the BuendiaSyncEngine. */
    public static class SyncStatusBroadcastReceiver extends BroadcastReceiver {
        @Override public void onReceive(Context context, Intent intent) {
            int syncStatus = intent.getIntExtra(SYNC_STATUS, -1 /*defaultValue*/);
            switch (syncStatus) {
                case STARTED:
                    LOG.i("SyncStatus: STARTED");
                    EventBus.getDefault().post(new SyncStartedEvent());
                    break;
                case COMPLETED:
                    LOG.i("SyncStatus: COMPLETED");
                    EventBus.getDefault().post(new SyncSucceededEvent());
                    break;
                case FAILED:
                    LOG.i("SyncStatus: FAILED");
                    EventBus.getDefault().post(new SyncFailedEvent());
                    break;
                case IN_PROGRESS:
                    int progress = intent.getIntExtra(SYNC_PROGRESS, 0);
                    String label = intent.getStringExtra(SYNC_PROGRESS_LABEL);
                    LOG.i("SyncStatus: IN_PROGRESS (%d%%, %s)", progress, label);
                    EventBus.getDefault().post(new SyncProgressEvent(progress, label));
                    break;
                case CANCELED:
                    LOG.i("SyncStatus: CANCELED");
                    EventBus.getDefault().post(new SyncCanceledEvent());
                    break;
                default:
                    LOG.i("SyncStatus: unknown code %d", syncStatus);
            }
        }
    }
}
