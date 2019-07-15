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

import org.projectbuendia.client.events.sync.SyncCanceledEvent;
import org.projectbuendia.client.events.sync.SyncFailedEvent;
import org.projectbuendia.client.events.sync.SyncProgressEvent;
import org.projectbuendia.client.events.sync.SyncStartedEvent;
import org.projectbuendia.client.events.sync.SyncSucceededEvent;
import org.projectbuendia.client.sync.BuendiaSyncEngine.Phase;
import org.projectbuendia.client.utils.Logger;

import de.greenrobot.event.EventBus;

/** Provides app-facing methods for requesting and cancelling sync operations. */
public class SyncManager {
    private static final Logger LOG = Logger.create();

    private static final int FULL_SYNC_PERIOD_SEC = 2 * 60;  // 2 minutes

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
        setPeriodicSync(FULL_SYNC_PERIOD_SEC, BuendiaSyncEngine.buildOptions(Phase.ALL));
    }

    /** Starts a full sync as soon as possible. */
    public void startFullSync() {
        LOG.i("Requesting full sync");
        mScheduler.requestSync(BuendiaSyncEngine.buildOptions(Phase.ALL));
    }

    /**
     * Starts, changes, or stops the periodic sync schedule.  There can be at most
     * one such repeating loop; any periodic sync from a previous call is replaced
     * with this new one.  Specifying a period of zero stops the periodic sync.
     */
    public void setPeriodicSync(int periodSec, Bundle options) {
        mScheduler.setPeriodicSync(periodSec, options);
    }

    /** Starts a sync of only observations, orders, and patients. */
    public void startObservationsAndOrdersSync() {
        // Cancel any existing syncs, which may delay this one.
        mScheduler.stopSyncing();
        LOG.i("Requesting incremental observations / orders / patients sync");
        mScheduler.requestSync(BuendiaSyncEngine.buildOptions(
            Phase.OBSERVATIONS, Phase.ORDERS, Phase.PATIENTS));
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
                    int messageId = intent.getIntExtra(SYNC_MESSAGE_ID, 0);
                    LOG.i("SyncStatus: IN_PROGRESS (%d%%, %s)", progress, context.getString(messageId));
                    EventBus.getDefault().post(new SyncProgressEvent(progress, messageId));
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
