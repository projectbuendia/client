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
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;

import org.projectbuendia.client.App;
import org.projectbuendia.client.AppSettings;
import org.projectbuendia.client.R;
import org.projectbuendia.client.events.sync.SyncCancelledEvent;
import org.projectbuendia.client.events.sync.SyncFailedEvent;
import org.projectbuendia.client.events.sync.SyncProgressEvent;
import org.projectbuendia.client.events.sync.SyncSucceededEvent;
import org.projectbuendia.client.sync.BuendiaSyncEngine.Phase;
import org.projectbuendia.client.utils.Logger;

import de.greenrobot.event.EventBus;

/** Provides app-facing methods for requesting and cancelling sync operations. */
public class SyncManager {
    private static final Logger LOG = Logger.create();
    public static final String STATUS_ACTION = "org.projectbuendia.client.SYNC_STATUS";

    // "Small" phases are ones that take <100 ms and send <100 bytes of data, >90% of the time.
    public static final Phase[] SMALL_PHASES = {Phase.OBSERVATIONS, Phase.ORDERS, Phase.PATIENTS};

    // "Medium" phases are ones that take <500 ms and send <4 kb of data, >90% of the time.
    public static final Phase[] MEDIUM_PHASES = {Phase.LOCATIONS, Phase.USERS};

    // "Large" phases are ones that take <2000 ms and send <20 kb of data, >90% of the time.
    public static final Phase[] LARGE_PHASES = Phase.ALL_PHASES;

    /** Key for the current sync status. */
    static final String SYNC_STATUS = "SYNC_STATUS";
    enum SyncStatus {
        IN_PROGRESS, SUCCEEDED, FAILED, CANCELLED
    }

    /** Keys for the amount of progress so far, expressed as a fraction. */
    static final String SYNC_NUMERATOR = "SYNC_NUMERATOR";
    static final String SYNC_DENOMINATOR = "SYNC_DENOMINATOR";

    /** Key for a nullable string describing the sync status to the user. */
    static final String SYNC_MESSAGE_ID = "sync-message-id";

    private boolean syncDisabled = false;
    private final SyncScheduler mScheduler;

    public SyncManager(SyncScheduler scheduler) {
        mScheduler = scheduler;
        HandlerThread receiverThread = new HandlerThread("SyncStatus");
        receiverThread.start();
        App.getInstance().getApplicationContext()
            .registerReceiver(new StatusReceiver(),
                new IntentFilter(STATUS_ACTION), null, new Handler(receiverThread.getLooper())
            );
    }

    public void setDisabled(boolean disabled) {
        if (disabled == true) {
            cancelSync();
        }
        syncDisabled = disabled;
    }

    /** Cancels an in-flight, non-periodic sync. */
    public void cancelSync() {
        mScheduler.stopSyncing();

        // If sync was pending, it should now be idle and we can consider the sync immediately canceled.
        if (!isSyncRunningOrPending()) {
            LOG.i("Sync was canceled before it began -- immediately firing SyncCancelledEvent.");
            EventBus.getDefault().post(new SyncCancelledEvent());
        }
    }

    public boolean isSyncRunningOrPending() {
        return mScheduler.isRunningOrPending();
    }

    /** Sets up regularly repeating syncs that run all the time. */
    public void initPeriodicSyncs() {
        AppSettings settings = App.getInstance().getSettings();
        setPeriodicSync(settings.getSmallSyncInterval(), SMALL_PHASES);
        setPeriodicSync(settings.getMediumSyncInterval(), MEDIUM_PHASES);
        setPeriodicSync(settings.getLargeSyncInterval(), LARGE_PHASES);
    }

    /** Starts a sync now. */
    public void sync(Phase... phases) {
        mScheduler.stopSyncing();  // cancel any running syncs to avoid delaying this one
        mScheduler.requestSync(buildOptions(phases));
    }

    /** Starts a small-size sync. */
    public void syncMedium() {
        sync(Phase.OBSERVATIONS, Phase.ORDERS, Phase.PATIENTS);
    }

    /** Starts a sync of everything now. */
    public void syncAll() {
        sync(Phase.ALL_PHASES);
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
    public static class StatusReceiver extends BroadcastReceiver {
        @Override public void onReceive(Context context, Intent intent) {
            SyncStatus status = (SyncStatus) intent.getSerializableExtra(SYNC_STATUS);
            switch (status) {
                case IN_PROGRESS:
                    int numerator = intent.getIntExtra(SYNC_NUMERATOR, 0);
                    int denominator = intent.getIntExtra(SYNC_DENOMINATOR, 1);
                    int messageId = intent.getIntExtra(SYNC_MESSAGE_ID, R.string.sync_in_progress);
                    LOG.d("SyncStatus: IN_PROGRESS: %d/%d", numerator, denominator);
                    EventBus.getDefault().post(new SyncProgressEvent(numerator, denominator, messageId));
                    break;
                case SUCCEEDED:
                    LOG.d("SyncStatus: SUCCEEDED");
                    EventBus.getDefault().post(new SyncSucceededEvent());
                    break;
                case FAILED:
                    LOG.d("SyncStatus: FAILED");
                    EventBus.getDefault().post(new SyncFailedEvent());
                    break;
                case CANCELLED:
                    LOG.d("SyncStatus: CANCELLED");
                    EventBus.getDefault().post(new SyncCancelledEvent());
                    break;
            }
        }
    }
}
