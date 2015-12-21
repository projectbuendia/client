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

import org.projectbuendia.client.AppSettings;
import org.projectbuendia.client.events.sync.SyncCanceledEvent;
import org.projectbuendia.client.events.sync.SyncFailedEvent;
import org.projectbuendia.client.events.sync.SyncProgressEvent;
import org.projectbuendia.client.events.sync.SyncStartedEvent;
import org.projectbuendia.client.events.sync.SyncSucceededEvent;
import org.projectbuendia.client.providers.Contracts;
import org.projectbuendia.client.utils.Logger;

import javax.annotation.Nullable;

import de.greenrobot.event.EventBus;

/** Manages the sync process and responds to sync events. */
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

    @Nullable private final AppSettings mSettings;

    public SyncManager(@Nullable AppSettings settings) {
        mSettings = settings;
    }

    /** Cancels an in-flight, non-periodic sync. */
    public void cancelOnDemandSync() {
        ContentResolver.cancelSync(
            SyncAccountService.getAccount(), Contracts.CONTENT_AUTHORITY);

        // If sync was pending, it should now be idle and we can consider the sync immediately
        // canceled.
        if (!isSyncPending() && !isSyncActive()) {
            LOG.i("Sync was canceled before it began -- immediately firing SyncCanceledEvent.");
            EventBus.getDefault().post(new SyncCanceledEvent());
        }
    }

    /** Returns {@code true} if a sync is pending. */
    public boolean isSyncPending() {
        return ContentResolver.isSyncPending(
            SyncAccountService.getAccount(), Contracts.CONTENT_AUTHORITY);
    }

    /** Returns {@code true} if a sync is active. */
    public boolean isSyncActive() {
        return ContentResolver.isSyncActive(
            SyncAccountService.getAccount(), Contracts.CONTENT_AUTHORITY);
    }

    /** Starts a full sync as soon as possible. */
    public void startFullSync() {
        SyncAccountService.startFullSync();
    }

    /** Starts a sync of only observations. */
    public void startObservationsSync() {
        SyncAccountService.startObservationsSync();
    }

    /**
     * A {@link BroadcastReceiver} that listens for sync status broadcasts sent by
     * {@link SyncAdapter}.
     */
    public static class SyncStatusBroadcastReceiver extends BroadcastReceiver {

        @Override public void onReceive(Context context, Intent intent) {
            int syncStatus = intent.getIntExtra(SYNC_STATUS, -1 /*defaultValue*/);
            switch (syncStatus) {
                case STARTED:
                    LOG.i("Sync started");
                    EventBus.getDefault().post(new SyncStartedEvent());
                    break;
                case COMPLETED:
                    LOG.i("Sync completed");
                    EventBus.getDefault().post(new SyncSucceededEvent());
                    break;
                case FAILED:
                    LOG.i("Sync failed");
                    EventBus.getDefault().post(new SyncFailedEvent());
                    break;
                case IN_PROGRESS:
                    int progress = intent.getIntExtra(SYNC_PROGRESS, 0);
                    String label = intent.getStringExtra(SYNC_PROGRESS_LABEL);
                    LOG.d("Sync in progress (%d%%, %s)", progress, label);
                    EventBus.getDefault().post(new SyncProgressEvent(progress, label));
                    break;
                case CANCELED:
                    LOG.i("Sync was canceled.");
                    EventBus.getDefault().post(new SyncCanceledEvent());
                    break;
                case -1:
                    LOG.i("Sync status broadcast intent received without a status code.");
                    break;
                default:
                    LOG.i("Sync status broadcast intent received with unknown status %1$d.",
                        syncStatus);
            }
        }
    }
}
