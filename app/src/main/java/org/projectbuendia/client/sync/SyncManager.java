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
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import org.projectbuendia.client.R;
import org.projectbuendia.client.events.sync.SyncCanceledEvent;
import org.projectbuendia.client.events.sync.SyncFailedEvent;
import org.projectbuendia.client.events.sync.SyncProgressEvent;
import org.projectbuendia.client.events.sync.SyncSucceededEvent;
import org.projectbuendia.client.sync.BuendiaSyncEngine.Phase;
import org.projectbuendia.client.utils.Logger;

import de.greenrobot.event.EventBus;

/** Provides app-facing methods for requesting and cancelling sync operations. */
public class SyncManager {
    private static final Logger LOG = Logger.create();

    /** Key for the current sync status. */
    static final String SYNC_STATUS = "SYNC_STATUS";
    enum SyncStatus {
        IN_PROGRESS, COMPLETED, FAILED, CANCELLED
    }

    /** Keys for the amount of progress so far, expressed as a fraction. */
    static final String SYNC_NUMERATOR = "SYNC_NUMERATOR";
    static final String SYNC_DENOMINATOR = "SYNC_DENOMINATOR";

    /** Key for a nullable string describing the sync status to the user. */
    static final String SYNC_MESSAGE_ID = "sync-message-id";

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

    /** Sets up regularly repeating syncs that run all the time. */
    public void initPeriodicSyncs() {
        setPeriodicSync(30, Phase.PATIENTS);
        setPeriodicSync(60, Phase.OBSERVATIONS, Phase.ORDERS);
        setPeriodicSync(180, Phase.ALL);
    }

    /** Starts a sync now. */
    public void sync(Phase... phases) {
        mScheduler.stopSyncing();  // cancel any running syncs to avoid delaying this one
        mScheduler.requestSync(buildOptions(phases));
    }

    /** Starts a sync of everything now. */
    public void syncAll() {
        sync(Phase.ALL);
    }

    /**
     * Starts, changes, or stops a periodic sync schedule.  There can be at most
     * one such repeating loop for each list of phases; if this list of phases
     * is identical to the list from a previous call, the repeating loop set
     * by the previous call is terminated and a new loop is started with the given
     * period.  When a loop starts, the first sync occurs after the first period
     * has elapsed.  Specifying a period of zero stops the loop.
     */
    public void setPeriodicSync(int periodSec, Phase... phases) {
        mScheduler.setPeriodicSync(periodSec, buildOptions(phases));
    }

    private Bundle buildOptions(Phase... phases) {
        return BuendiaSyncEngine.buildOptions(phases);
    }

    /** Listens for sync status events that are broadcast by the BuendiaSyncEngine. */
    public static class SyncStatusBroadcastReceiver extends BroadcastReceiver {
        @Override public void onReceive(Context context, Intent intent) {
            SyncStatus status = (SyncStatus) intent.getSerializableExtra(SYNC_STATUS);
            switch (status) {
                case IN_PROGRESS:
                    int numerator = intent.getIntExtra(SYNC_NUMERATOR, 0);
                    int denominator = intent.getIntExtra(SYNC_DENOMINATOR, 1);
                    int messageId = intent.getIntExtra(SYNC_MESSAGE_ID, R.string.sync_in_progress);
                    EventBus.getDefault().post(new SyncProgressEvent(numerator, denominator, messageId));
                    break;
                case COMPLETED:
                    LOG.i("SyncStatus: COMPLETED");
                    EventBus.getDefault().post(new SyncSucceededEvent());
                    break;
                case FAILED:
                    LOG.i("SyncStatus: FAILED");
                    EventBus.getDefault().post(new SyncFailedEvent());
                    break;
                case CANCELLED:
                    LOG.i("SyncStatus: CANCELLED");
                    EventBus.getDefault().post(new SyncCanceledEvent());
                    break;
            }
        }
    }
}
