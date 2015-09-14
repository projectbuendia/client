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

package org.projectbuendia.client.ui.lists;

import org.projectbuendia.client.App;
import org.projectbuendia.client.diagnostics.HealthMonitor;
import org.projectbuendia.client.events.sync.SyncFailedEvent;
import org.projectbuendia.client.events.sync.SyncSucceededEvent;
import org.projectbuendia.client.sync.SyncManager;
import org.projectbuendia.client.utils.EventBusRegistrationInterface;
import org.projectbuendia.client.utils.Logger;

import javax.inject.Inject;

/** Controller for non-inherited parts of {@link PatientListFragment}. */
public class PatientListController {

    private static final Logger LOG = Logger.create();
    private final SyncSubscriber mSyncSubscriber = new SyncSubscriber();
    private final Ui mUi;
    private final SyncManager mSyncManager;
    @Inject HealthMonitor mHealthMonitor;
    /** True if a full sync initiated by this activity is in progress. */
    private boolean mInitiatedFullSync;
    private EventBusRegistrationInterface mEventBus;

    public interface Ui {
        /** Stops the refresh-in-progress animation. */
        void stopRefreshAnimation();

        /** Notifies the user that the refresh failed. */
        void showRefreshError();

        /** Notifies the user that the API is unhealthy. */
        void showApiHealthProblem();
    }

    /**
     * Initializes this with the given UI, sync manager, and event bus.
     * @param ui          {@link Ui} that will respond to list refresh events
     * @param syncManager a {@link SyncManager} for performing sync operations
     * @param eventBus    the {@link EventBusRegistrationInterface} that will listen for sync events
     */
    public PatientListController(
        Ui ui, SyncManager syncManager, EventBusRegistrationInterface eventBus) {
        mUi = ui;
        mSyncManager = syncManager;
        mEventBus = eventBus;
        App.getInstance().inject(this);
    }

    public void init() {
        mEventBus.register(mSyncSubscriber);
    }

    public void suspend() {
        mEventBus.unregister(mSyncSubscriber);
    }

    /**
     * Forces a new sync of all data from server, unless this activity is already
     * waiting for a previously initiated sync.
     */
    public void onRefreshRequested() {
        if (mHealthMonitor.isApiUnavailable()) {
            mUi.stopRefreshAnimation();
            mUi.showApiHealthProblem();
        } else if (!mInitiatedFullSync) {
            LOG.d("onRefreshRequested");
            mSyncManager.startFullSync();
            mInitiatedFullSync = true;
        }
    }

    private void onSyncFinished(boolean success) {
        mUi.stopRefreshAnimation();
        if (mInitiatedFullSync && !success) {
            mUi.showRefreshError();
        }
        mInitiatedFullSync = false;
    }

    private final class SyncSubscriber {
        public synchronized void onEventMainThread(SyncSucceededEvent event) {
            onSyncFinished(true);
        }

        public synchronized void onEventMainThread(SyncFailedEvent event) {
            onSyncFinished(false);
        }
    }
}
