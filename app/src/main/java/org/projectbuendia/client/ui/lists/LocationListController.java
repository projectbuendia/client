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
import org.projectbuendia.client.events.sync.SyncCanceledEvent;
import org.projectbuendia.client.events.sync.SyncFailedEvent;
import org.projectbuendia.client.events.sync.SyncProgressEvent;
import org.projectbuendia.client.events.sync.SyncStartedEvent;
import org.projectbuendia.client.events.sync.SyncSucceededEvent;
import org.projectbuendia.client.models.AppModel;
import org.projectbuendia.client.models.NewLocation;
import org.projectbuendia.client.models.NewLocationTree;
import org.projectbuendia.client.models.Zones;
import org.projectbuendia.client.sync.SyncManager;
import org.projectbuendia.client.ui.LoadingState;
import org.projectbuendia.client.utils.EventBusRegistrationInterface;
import org.projectbuendia.client.utils.Logger;
import org.projectbuendia.client.utils.Utils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

/** Controller for {@link LocationListActivity}. */
final class LocationListController {

    private static final Logger LOG = Logger.create();

    private final AppModel mAppModel;
    private final AppSettings mSettings;
    private final CrudEventBus mCrudEventBus;
    private final Ui mUi;
    private final Set<LocationFragmentUi> mFragmentUis = new HashSet<>();
    private final EventBusRegistrationInterface mEventBus;
    private final EventBusSubscriber mEventBusSubscriber = new EventBusSubscriber();
    private final SyncManager mSyncManager;
    private final PatientSearchController mPatientSearchController;
    @Nullable private NewLocationTree mLocationTree;
    @Nullable private NewLocation mTriageZone;
    @Nullable private NewLocation mDischargedZone;
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

        void openSingleLocation(NewLocation location);

        void showSyncFailedDialog(boolean show);

        void setLoadingState(LoadingState loadingState);

        void finish();
    }

    public interface LocationFragmentUi {

        void setLocations(NewLocationTree locationTree, List<NewLocation> locations);

        void setPresentPatientCount(long patientCount);

        void setTriagePatientCount(long patientCount);

        void setDischargedPatientCount(long dischargedPatientCount);

        void setBusyLoading(boolean busy);

        void showIncrementalSyncProgress(int progress, int messageId);

        void resetSyncProgress();

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
        mWaitingOnSyncCancel = false;
        mEventBus.register(mEventBusSubscriber);
        mCrudEventBus.register(mEventBusSubscriber);
        if (mAppModel.isFullModelAvailable()) loadOrSyncLocationTree();
        updateUi();
    }

    public void loadOrSyncLocationTree() {
        NewLocationTree tree = mAppModel.getLocationTree(mSettings.getLocaleTag());
        if (tree.size() > 0) mLocationTree = tree;
        if (mLocationTree != null) {
            mTriageZone = mLocationTree.get(Zones.TRIAGE_ZONE_UUID);
            mDischargedZone = mLocationTree.get(Zones.DISCHARGED_ZONE_UUID);
            updateUi();
        } else startSync();
    }

    public void startSync() {
        mWaitingOnSync = true;
        if (!mSyncManager.isSyncRunningOrPending()) {
            mUi.setLoadingState(LoadingState.SYNCING);
            for (LocationFragmentUi fragmentUi : mFragmentUis) {
                fragmentUi.resetSyncProgress();
            }
            mSyncManager.syncAll();
        }
    }

    private void updateUi() {
        updateLoadingState();
        for (LocationFragmentUi fragmentUi : mFragmentUis) {
            fragmentUi.setBusyLoading(mLocationTree == null);
            if (mLocationTree != null) {
                long dischargedPatientCount = mLocationTree.countPatientsIn(mDischargedZone);
                long totalPatientCount = mLocationTree.countAllPatients();
                fragmentUi.setLocations(mLocationTree, mLocationTree.getLeaves());
                fragmentUi.setPresentPatientCount(totalPatientCount - dischargedPatientCount);
                fragmentUi.setDischargedPatientCount(mLocationTree.countPatientsIn(mDischargedZone));
                fragmentUi.setTriagePatientCount(mLocationTree.countPatientsIn(mTriageZone));
            }
        }
    }

    private void updateLoadingState() {
        mUi.setLoadingState(mLocationTree != null ? LoadingState.LOADED :
            mWaitingOnSync ? LoadingState.SYNCING : LoadingState.LOADING);
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
    public void onLocationSelected(NewLocation location) {
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
                    fragmentUi.showIncrementalSyncProgress(event.progress, event.messageId);
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
            mWaitingOnSync = false;
            loadOrSyncLocationTree();
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

    }
}
