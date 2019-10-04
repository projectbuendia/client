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

import org.projectbuendia.client.App;
import org.projectbuendia.client.AppSettings;
import org.projectbuendia.client.R;
import org.projectbuendia.client.events.sync.SyncCancelledEvent;
import org.projectbuendia.client.events.sync.SyncFailedEvent;
import org.projectbuendia.client.events.sync.SyncProgressEvent;
import org.projectbuendia.client.events.sync.SyncSucceededEvent;
import org.projectbuendia.client.sync.BuendiaSyncEngine.Phase;
import org.projectbuendia.client.utils.Logger;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

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

    private boolean newSyncsSuppressed = false;
    private final SyncScheduler mScheduler;
    private final List<Runnable> syncStoppedCallbacks = new ArrayList<>();

    public SyncManager(SyncScheduler scheduler) {
        mScheduler = scheduler;
        App.getContext()
            .registerReceiver(new StatusReceiver(), new IntentFilter(STATUS_ACTION));
    }

    public boolean getNewSyncsSuppressed() {
        return newSyncsSuppressed;
    }

    /**
     * Sets whether all new syncs should be suppressed.  While this flag is true,
     * any attempt to start a new sync (by an explicit call to sync() or by any
     * periodic sync loop) becomes a no-op.  Any loops started by setPeriodicSync
     * continue to loop but do not trigger any syncs.  Setting this flag has no
     * effect on any already running sync.
     */
    public void setNewSyncsSuppressed(boolean suppressed) {
        newSyncsSuppressed = suppressed;
    }

    /** Stops any currently running sync; invokes a callback when stopped or if already stopped. */
    public void stopSyncing(@Nullable Runnable syncStoppedCallback) {
        if (syncStoppedCallback != null) {
            syncStoppedCallbacks.add(syncStoppedCallback);
        }
        if (isSyncRunningOrPending()) {
            mScheduler.stopSyncing();  // let the SyncStoppedEvent trigger the callback
        } else {
            runSyncStoppedCallbacks();
        }
    }

    public boolean isSyncRunningOrPending() {
        return mScheduler.isRunningOrPending();
    }

    /** Starts or cancels regularly repeating syncs, according to the settings. */
    public void applyPeriodicSyncSettings() {
        AppSettings settings = App.getSettings();
        if (settings.getPeriodicSyncDisabled()) {
            mScheduler.clearAllPeriodicSyncs();
        } else {
            setPeriodicSync(settings.getSmallSyncInterval(), SMALL_PHASES);
            setPeriodicSync(settings.getMediumSyncInterval(), MEDIUM_PHASES);
            setPeriodicSync(settings.getLargeSyncInterval(), LARGE_PHASES);
        }
    }

    /** Starts a sync now. */
    public void sync(Phase... phases) {
        mScheduler.stopSyncing();  // cancel any running syncs to avoid delaying this one
        mScheduler.requestSync(buildOptions(phases));
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

    // TODO(ping): This receiver will receive events from any instance of the
    // Buendia app, so if there are two instances running (e.g. dev and prod)
    // sync errors in one will abort the other and cause mayhem.
    /** Listens for sync status events that are broadcast by the BuendiaSyncEngine. */
    public class StatusReceiver extends BroadcastReceiver {
        @Override public void onReceive(Context context, Intent intent) {
            SyncStatus status = (SyncStatus) intent.getSerializableExtra(SYNC_STATUS);
            if (status == SyncStatus.IN_PROGRESS) {
                int numerator = intent.getIntExtra(SYNC_NUMERATOR, 0);
                int denominator = intent.getIntExtra(SYNC_DENOMINATOR, 1);
                int messageId = intent.getIntExtra(SYNC_MESSAGE_ID, R.string.sync_in_progress);
                LOG.d("SyncStatus: IN_PROGRESS: %d/%d", numerator, denominator);
                EventBus.getDefault().postSticky(new SyncProgressEvent(numerator, denominator, messageId));
            } else {
                // All three other statuses indicate that sync has stopped.
                LOG.d("SyncStatus: %s", status);
                runSyncStoppedCallbacks();
                EventBus.getDefault().post(
                    status == SyncStatus.SUCCEEDED ? new SyncSucceededEvent() :
                    status == SyncStatus.FAILED ? new SyncFailedEvent() :
                    /* status == SyncStatus.CANCELLED */ new SyncCancelledEvent());
            }
        }
    }

    /** Invokes any callbacks that are waiting for sync to stop. */
    private void runSyncStoppedCallbacks() {
        for (Runnable callback : syncStoppedCallbacks) {
            try {
                callback.run();
            } catch (Throwable t) {
                LOG.e(t, "Exception in stopSyncing callback");
            }
        }
        syncStoppedCallbacks.clear();
    }
}
