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

import android.content.ContentResolver;

import org.projectbuendia.client.events.sync.SyncCanceledEvent;
import org.projectbuendia.client.providers.Contracts;
import org.projectbuendia.client.utils.EventBusInterface;
import org.projectbuendia.client.utils.Logger;

/** Manages the sync process and responds to sync events. */
public class SyncManager {

    private static final Logger LOG = Logger.create();

    private final EventBusInterface mEventBus;

    public SyncManager(EventBusInterface eventBus) {
        mEventBus = eventBus;
    }

    /** Cancels an in-flight, non-periodic sync. */
    public void cancelOnDemandSync() {
        ContentResolver.cancelSync(
            SyncAccountService.getAccount(), Contracts.CONTENT_AUTHORITY);

        // If sync was pending, it should now be idle and we can consider the sync immediately
        // canceled.
        if (!isSyncPending() && !isSyncActive()) {
            LOG.i("Sync was canceled before it began -- immediately firing SyncCanceledEvent.");
            mEventBus.post(new SyncCanceledEvent());
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

    /** Starts a sync of only observations and orders. */
    public static void startObservationsAndOrdersSync() {
        SyncAccountService.startObservationsAndOrdersSync();
    }

}
