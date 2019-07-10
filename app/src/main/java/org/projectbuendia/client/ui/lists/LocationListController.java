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

import org.projectbuendia.client.events.CrudEventBus;
import org.projectbuendia.client.events.actions.SyncCancelRequestedEvent;
import org.projectbuendia.client.events.data.AppLocationTreeFetchedEvent;
import org.projectbuendia.client.events.sync.SyncCanceledEvent;
import org.projectbuendia.client.events.sync.SyncFailedEvent;
import org.projectbuendia.client.events.sync.SyncProgressEvent;
import org.projectbuendia.client.events.sync.SyncStartedEvent;
import org.projectbuendia.client.events.sync.SyncSucceededEvent;
import org.projectbuendia.client.models.AppModel;
import org.projectbuendia.client.models.Location;
import org.projectbuendia.client.models.LocationTree;
import org.projectbuendia.client.models.Zones;
import org.projectbuendia.client.sync.SyncManager;
import org.projectbuendia.client.ui.LoadingState;
import org.projectbuendia.client.utils.EventBusRegistrationInterface;
import org.projectbuendia.client.utils.LocaleSelector;
import org.projectbuendia.client.utils.Logger;
import org.projectbuendia.client.utils.Utils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

/** Controller for {@link LocationListActivity}. */
final class LocationListController {

    private static final Logger LOG = Logger.create();

    // TODO/feature: Allow LOCATIONS_DEPTH to be specified.
    /**
     * The depth of locations to display. Only locations at this depth in the location tree will be
     * displayed.
     */
    private static final int LOCATIONS_DEPTH = LocationTree.ABSOLUTE_DEPTH_TENT;
    private final AppModel mAppModel;
    private final CrudEventBus mCrudEventBus;
    private final Ui mUi;
    private final Set<LocationFragmentUi> mFragmentUis = new HashSet<>();
    private final EventBusRegistrationInterface mEventBus;
    private final EventBusSubscriber mEventBusSubscriber = new EventBusSubscriber();
    private final SyncManager mSyncManager;
    private final PatientSearchController mPatientSearchController;
    @Nullable private LocationTree mLocationTree;
    @Nullable private Location mTriageZone;
    @Nullable private Location mDischargedZone;
    // True when the data model is unavailable and either a sync is already in progress or has been
    // requested by this controller.
    private boolean mWaitingOnSync = false;
    // True when the user has explicitly requested that a sync be canceled (e.g. via the sync cancel
    // button). Sync operations may be cancelled and rescheduled by Android without the user
    // requesting a sync cancellation. In these cases, this flag will remain false.
    private boolean mWaitingOnSyncCancel = false;
    private final Object mSyncCancelLock = new Object();

    public interface Ui {

        void switchToLocationList();

        void switchToPatientList();

        void openSingleLocation(Location location);

        void showSyncFailedDialog(boolean show);

        void setLoadingState(LoadingState loadingState);

        void finish();
    }

    public interface LocationFragmentUi {

        void setLocations(LocationTree locationTree, List<Location> locations);

        void setPresentPatientCount(long patientCount);

        void setTriagePatientCount(long patientCount);

        void setDischargedPatientCount(long dischargedPatientCount);

        void setBusyLoading(boolean busy);

        void showIncrementalSyncProgress(int progress, String label);

        void resetSyncProgress();

        void showSyncCancelRequested();
    }

    public LocationListController(
        AppModel appModel,
        CrudEventBus crudEventBus,
        Ui ui,
        EventBusRegistrationInterface eventBus,
        SyncManager syncManager,
        PatientSearchController patientSearchController) {
        mAppModel = appModel;
        mCrudEventBus = crudEventBus;
        mUi = ui;
        mEventBus = eventBus;
        mSyncManager = syncManager;
        mPatientSearchController = patientSearchController;
    }

    public void init() {
        mWaitingOnSyncCancel = false;
        mEventBus.register(mEventBusSubscriber);
        mCrudEventBus.register(mEventBusSubscriber);
        LOG.d("init: isLocationTreeValid() = " + isLocationTreeValid());

        // Get or update mLocationTree.
        if (mAppModel.isFullModelAvailable()) {
            LOG.i("Data model is available in init(); loading location tree from local DB");
            mWaitingOnSync = false;
            mAppModel.fetchLocationTree(
                mCrudEventBus, LocaleSelector.getCurrentLocale().getLanguage());
        } else {
            LOG.i("Data model unavailable; waiting on sync.");
            mWaitingOnSync = true;
            if (!mSyncManager.isSyncRunningOrPending()) {
                LOG.i("No sync detected, forcing new sync.");
                onSyncRetry();
            }
        }

        updateUi();
    }

    /** Returns true if a non-empty LocationTree has been loaded from the local database. */
    private boolean isLocationTreeValid() {
        return mLocationTree != null && mLocationTree.getRoot() != null;
    }

    public void onSyncRetry() {
        mWaitingOnSync = true;
        mUi.setLoadingState(LoadingState.SYNCING);
        for (LocationFragmentUi fragmentUi : mFragmentUis) {
            fragmentUi.resetSyncProgress();
        }
        mSyncManager.startFullSync();
    }

    private void updateUi() {
        boolean hasValidTree = isLocationTreeValid();
        updateLoadingState();
        for (LocationFragmentUi fragmentUi : mFragmentUis) {
            fragmentUi.setBusyLoading(!hasValidTree);

            if (hasValidTree) {
                long dischargedPatientCount = (mDischargedZone == null)
                    ? 0 : mLocationTree.getTotalPatientCount(mDischargedZone);
                long totalPatientCount =
                    mLocationTree.getTotalPatientCount(mLocationTree.getRoot());
                fragmentUi.setLocations(
                    mLocationTree,
                    mLocationTree.getDescendantsAtDepth(LOCATIONS_DEPTH).asList());
                fragmentUi.setPresentPatientCount(totalPatientCount - dischargedPatientCount);
                fragmentUi.setDischargedPatientCount(
                    (mDischargedZone == null)
                        ? 0 : mLocationTree.getTotalPatientCount(mDischargedZone));
                fragmentUi.setTriagePatientCount(
                    (mTriageZone == null)
                        ? 0 : mLocationTree.getTotalPatientCount(mTriageZone));
            }
        }
    }

    private void updateLoadingState() {
        boolean hasLocationTree = isLocationTreeValid();
        if (hasLocationTree) {
            mUi.setLoadingState(LoadingState.LOADED);
            return;
        }

        if (mWaitingOnSync) {
            mUi.setLoadingState(LoadingState.SYNCING);
            return;
        }

        mUi.setLoadingState(LoadingState.LOADING);
    }

    public void attachFragmentUi(LocationFragmentUi fragmentUi) {
        LOG.d("Attached new fragment UI: " + fragmentUi);
        mFragmentUis.add(fragmentUi);
        updateUi();
    }

    public void detachFragmentUi(LocationFragmentUi fragmentUi) {
        LOG.d("Detached fragment UI: " + fragmentUi);
        mFragmentUis.remove(fragmentUi);
    }

    /** Frees any resources used by the controller. */
    public void suspend() {
        LOG.d("Controller suspended.");

        if (mLocationTree != null) {
            mLocationTree.close();
        }
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

    /** Call when the user presses the discharged zone. */
    public void onDischargedPressed() {
        Utils.logUserAction("location_pressed", "location", mDischargedZone.name);
        mUi.openSingleLocation(mDischargedZone);
    }

    /** Call when the user presses the triage zone. */
    public void onTriagePressed() {
        Utils.logUserAction("location_pressed", "location", mTriageZone.name);
        mUi.openSingleLocation(mTriageZone);
    }

    /** Call when the user presses a location. */
    public void onLocationSelected(Location location) {
        Utils.logUserAction("location_pressed", "location", location.name);
        mUi.openSingleLocation(location);
    }

    @SuppressWarnings("unused") // Called by reflection from EventBus
    private final class EventBusSubscriber {

        public void onEventMainThread(SyncCancelRequestedEvent event) {
            if (mWaitingOnSync) {
                synchronized (mSyncCancelLock) {
                    mWaitingOnSyncCancel = true;
                    for (LocationFragmentUi fragmentUi : mFragmentUis) {
                        fragmentUi.showSyncCancelRequested();
                    }
                }
            }
        }

        public void onEventMainThread(SyncCanceledEvent event) {
            // If user-initiated cancellation occurred, close the activity even if we're no longer
            // waiting on a sync (continuing to load the activity might be jarring).
            synchronized (mSyncCancelLock) {
                if (!mWaitingOnSyncCancel) {
                    LOG.d("Detected non-user-initiated sync cancellation, ignoring.");
                    return;
                }

                mWaitingOnSyncCancel = false;
                LOG.d("Detected sync cancellation while waiting on sync, finishing activity.");
                mUi.finish();
            }
        }

        public void onEventMainThread(SyncProgressEvent event) {
            if (mWaitingOnSync) {
                for (LocationFragmentUi fragmentUi : mFragmentUis) {
                    fragmentUi.showIncrementalSyncProgress(event.progress, event.label);
                }
            }
        }

        public void onEventMainThread(SyncStartedEvent event) {
            if (mWaitingOnSync) {
                for (LocationFragmentUi fragmentUi : mFragmentUis) {
                    fragmentUi.resetSyncProgress();
                }
            }
        }

        public void onEventMainThread(SyncSucceededEvent event) {
            mUi.showSyncFailedDialog(false);

            // Reload locations from the local datastore when a full sync completes successfully.
            if (mAppModel.isFullModelAvailable()) {
                LOG.i("Data model is available after sync; loading location tree.");
                mAppModel.fetchLocationTree(
                    mCrudEventBus, LocaleSelector.getCurrentLocale().getLanguage());
                mWaitingOnSync = false;
            } else if (!isLocationTreeValid()) {
                LOG.i("Sync succeeded but was incomplete; forcing a new sync.");
                onSyncRetry();
            }
        }

        public void onEventMainThread(SyncFailedEvent event) {
            if (mWaitingOnSync) {
                for (LocationFragmentUi fragmentUi : mFragmentUis) {
                    fragmentUi.resetSyncProgress();
                }
                mUi.showSyncFailedDialog(true);
                Utils.logEvent("sync_failed_dialog_shown");
            }
        }

        public void onEventMainThread(AppLocationTreeFetchedEvent event) {
            if (mLocationTree != null) {
                mLocationTree.close();
            }
            mLocationTree = event.tree;
            if (!isLocationTreeValid()) {
                LOG.i("Found no locations in the local datastore; continuing to wait on sync.");
                return;
            }
            mWaitingOnSync = false;

            LOG.i("Received a valid location tree.");
            for (Location zone :
                mLocationTree.getChildren(mLocationTree.getRoot())) {
                switch (zone.uuid) {
                    case Zones.TRIAGE_ZONE_UUID:
                        mTriageZone = zone;
                        break;
                    case Zones.DISCHARGED_ZONE_UUID:
                        mDischargedZone = zone;
                        break;
                    default:
                        break;
                }
            }

            updateUi();

            // Update the search controller immediately -- it does not listen for location updates
            // on this controller's bus and would otherwise be unaware of changes.
            // TODO/cleanup: Remove -- likely unnecessary.
            mPatientSearchController.setLocations(mLocationTree);
        }
    }
}
