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

import org.projectbuendia.client.AppSettings;
import org.projectbuendia.client.events.CrudEventBus;
import org.projectbuendia.client.events.actions.SyncCancelRequestedEvent;
import org.projectbuendia.client.events.sync.SyncCancelledEvent;
import org.projectbuendia.client.events.sync.SyncFailedEvent;
import org.projectbuendia.client.events.sync.SyncProgressEvent;
import org.projectbuendia.client.events.sync.SyncSucceededEvent;
import org.projectbuendia.models.AppModel;
import org.projectbuendia.models.Location;
import org.projectbuendia.models.LocationForest;
import org.projectbuendia.client.sync.SyncManager;
import org.projectbuendia.client.ui.ReadyState;
import org.projectbuendia.client.utils.EventBusRegistrationInterface;
import org.projectbuendia.client.utils.Logger;
import org.projectbuendia.client.utils.Utils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Controller for {@link LocationListActivity}. */
final class LocationListController {

    private static final Logger LOG = Logger.create();

    private final AppModel mAppModel;
    private final AppSettings mSettings;
    private final CrudEventBus mCrudEventBus;
    private final Ui mUi;
    private final Set<LocationListFragmentUi> mFragmentUis = new HashSet<>();
    private final EventBusRegistrationInterface mEventBus;
    private final EventBusSubscriber mEventBusSubscriber = new EventBusSubscriber();
    private final SyncManager mSyncManager;
    private final PatientSearchController mPatientSearchController;

    @Nullable private LocationForest mForest;

    /**
     * The ready state is SYNCING only when there is no data model yet and the
     * initial sync is running.  LOADING is the default non-ready state.
     */
    private ReadyState mReadyState = ReadyState.LOADING;

    /** True when the user has requested to cancel a sync and the sync hasn't stopped yet. */
    private boolean mUserCancelRequestPending = false;
    private final Object mSyncCancelLock = new Object();

    public interface Ui {

        void switchToLocationList();

        void switchToPatientList();

        void openSingleLocation(Location location);

        void showSyncFailedDialog(boolean show);

        void setReadyState(ReadyState state);

        void finish();
    }

    public interface LocationListFragmentUi {

        void setLocations(LocationForest forest, Collection<Location> locations);

        void setReadyState(ReadyState state);

        void setSyncProgress(int numerator, int denominator, Integer messageId);

        void showSyncCancelRequested();
    }

    public LocationListController(
        AppModel appModel,
        AppSettings settings,
        CrudEventBus crudEventBus,
        Ui ui,
        EventBusRegistrationInterface eventBus,
        SyncManager syncManager,
        PatientSearchController patientSearchController) {
        mAppModel = appModel;
        mSettings = settings;
        mCrudEventBus = crudEventBus;
        mUi = ui;
        mEventBus = eventBus;
        mSyncManager = syncManager;
        mPatientSearchController = patientSearchController;
    }

    public void init() {
        mUserCancelRequestPending = false;
        mEventBus.register(mEventBusSubscriber);
        mCrudEventBus.register(mEventBusSubscriber);
        if (mAppModel.isReady()) {
            loadForest();
        } else {
            LOG.w("Model has not been synced successfully yet; initiating full sync.");
            startInitialSync();
        }
    }

    public void loadForest() {
        setReadyState(ReadyState.LOADING);
        setForest(mAppModel.getForest());
        setReadyState(ReadyState.READY);
    }

    public void startInitialSync() {
        setReadyState(ReadyState.SYNCING);
        if (!mSyncManager.isSyncRunningOrPending()) {
            mSyncManager.syncAll();
        }
    }

    private void setForest(@Nonnull LocationForest forest) {
        mForest = forest;
        for (LocationListFragmentUi fragmentUi : mFragmentUis) {
            updateFragmentUi(fragmentUi);
        }
    }

    private void updateFragmentUi(LocationListFragmentUi fragmentUi) {
        if (mForest != null) {
            fragmentUi.setLocations(mForest, mForest.allNodes());
        }
        fragmentUi.setReadyState(mReadyState);
    }

    private void setReadyState(ReadyState state) {
        mReadyState = state;
        mUi.setReadyState(state);
        for (LocationListFragmentUi fragmentUi : mFragmentUis) {
            fragmentUi.setReadyState(state);
        }
    }

    public void attachFragmentUi(LocationListFragmentUi fragmentUi) {
        mFragmentUis.add(fragmentUi);
        updateFragmentUi(fragmentUi);
    }

    public void detachFragmentUi(LocationListFragmentUi fragmentUi) {
        mFragmentUis.remove(fragmentUi);
    }

    /** Frees any resources used by the controller. */
    public void suspend() {
        mCrudEventBus.unregister(mEventBusSubscriber);
        mEventBus.unregister(mEventBusSubscriber);
    }

    /** Call when the user presses the search button. */
    public void onSearchPressed() {
        Utils.logUserAction("search_pressed");
        mUi.switchToPatientList();
    }

    /** Call when the user exits search mode. */
    public void onSearchCancelled() {
        Utils.logUserAction("search_cancelled");
        mUi.switchToLocationList();
    }

    /** Call when the user presses a location. */
    public void onLocationSelected(Location location) {
        Utils.logUserAction("location_pressed", "location", location.name);
        mUi.openSingleLocation(location);
    }

    @SuppressWarnings("unused") // Called by reflection from EventBus
    private final class EventBusSubscriber {

        public void onEventMainThread(SyncCancelRequestedEvent event) {
            if (mReadyState == ReadyState.SYNCING) {
                synchronized (mSyncCancelLock) {
                    mUserCancelRequestPending = true;
                    for (LocationListFragmentUi fragmentUi : mFragmentUis) {
                        fragmentUi.showSyncCancelRequested();
                    }
                }
            }
        }

        public void onEventMainThread(SyncCancelledEvent event) {
            // If user-initiated cancellation occurred, close the activity even if we're no longer
            // waiting on a sync (continuing to load the activity might be jarring).
            synchronized (mSyncCancelLock) {
                if (mUserCancelRequestPending) {
                    LOG.d("User-initiated sync cancellation completed; finishing activity.");
                    mUi.finish();
                } else {
                    LOG.d("Sync cancelled, but not by the user.");
                }
            }
        }

        public void onEventMainThread(SyncProgressEvent event) {
            for (LocationListFragmentUi fragmentUi : mFragmentUis) {
                fragmentUi.setSyncProgress(event.numerator, event.denominator, event.messageId);
            }
        }

        public void onEventMainThread(SyncSucceededEvent event) {
            mUi.showSyncFailedDialog(false);
            loadForest();
        }

        public void onEventMainThread(SyncFailedEvent event) {
            if (mReadyState == ReadyState.SYNCING) {
                // Show the dialog only if the UI is waiting for this sync.
                mUi.showSyncFailedDialog(true);
                Utils.logEvent("sync_failed_dialog_shown");
                for (LocationListFragmentUi fragmentUi : mFragmentUis) {
                    fragmentUi.setSyncProgress(0, 0, null);
                }
                setReadyState(ReadyState.ERROR);
            }
        }
    }
}
