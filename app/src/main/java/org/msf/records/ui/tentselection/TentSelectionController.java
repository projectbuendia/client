package org.msf.records.ui.tentselection;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.msf.records.data.app.AppLocation;
import org.msf.records.data.app.AppLocationTree;
import org.msf.records.data.app.AppModel;
import org.msf.records.events.CrudEventBus;
import org.msf.records.events.actions.SyncCancelRequestedEvent;
import org.msf.records.events.data.AppLocationTreeFetchedEvent;
import org.msf.records.events.sync.SyncCanceledEvent;
import org.msf.records.events.sync.SyncFailedEvent;
import org.msf.records.events.sync.SyncProgressEvent;
import org.msf.records.events.sync.SyncStartedEvent;
import org.msf.records.events.sync.SyncSucceededEvent;
import org.msf.records.model.Zone;
import org.msf.records.sync.SyncManager;
import org.msf.records.ui.LoadingState;
import org.msf.records.ui.patientlist.PatientSearchController;
import org.msf.records.utils.EventBusRegistrationInterface;
import org.msf.records.utils.LocaleSelector;
import org.msf.records.utils.Logger;

/**
 * Controller for {@link TentSelectionActivity}.
 *
 * Avoid adding untestable dependencies to this class.
 */
final class TentSelectionController {

    private static final Logger LOG = Logger.create();

    private static final boolean DEBUG = true;

    public interface Ui {

        void switchToTentSelectionScreen();

        void switchToPatientListScreen();

        void launchActivityForLocation(AppLocation location);

        void showErrorMessage(int stringResourceId);

        void showSyncFailedDialog(boolean show);

        void setLoadingState(LoadingState loadingState);

        void finish();
    }

    public interface TentFragmentUi {

        void setTents(AppLocationTree locationTree, List<AppLocation> tents);

        void setPresentPatientCount(int patientCount);

        void setTriagePatientCount(int patientCount);

        void setDischargedPatientCount(int dischargedPatientCount);

        void setBusyLoading(boolean busy);

        void showIncrementalSyncProgress(int progress, String label);

        void resetSyncProgress();

        void showSyncCancelRequested();
    }

    private final AppModel mAppModel;
    private final CrudEventBus mCrudEventBus;
    private final Ui mUi;
    private final Set<TentFragmentUi> mFragmentUis = new HashSet<>();
    private final EventBusRegistrationInterface mEventBus;
    private final EventBusSubscriber mEventBusSubscriber = new EventBusSubscriber();
    private final SyncManager mSyncManager;
    private final PatientSearchController mPatientSearchController;

    @Nullable private AppLocationTree mAppLocationTree;
    @Nullable private AppLocation mTriageZone;
    @Nullable private AppLocation mDischargedZone;

    public TentSelectionController(
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
        mEventBus.register(mEventBusSubscriber);
        mCrudEventBus.register(mEventBusSubscriber);
        LOG.d("init: isLocationTreeValid() = " + isLocationTreeValid());

        // Get or update mAppLocationTree.
        if (mAppModel.isFullModelAvailable()) {
            LOG.i("Data model is available in init(); loading location tree from local DB");
            mAppModel.fetchLocationTree(
                    mCrudEventBus, LocaleSelector.getCurrentLocale().getLanguage());
        } else {
            LOG.i("Data model unavailable; forcing sync.");
            onSyncRetry();
        }

        updateUi();
    }

    /** Returns true if a non-empty AppLocationTree has been loaded from the local database. */
    private boolean isLocationTreeValid() {
        return mAppLocationTree != null && mAppLocationTree.getRoot() != null;
    }

    public void onSyncRetry() {
        mUi.setLoadingState(LoadingState.SYNCING);
        for (TentFragmentUi fragmentUi : mFragmentUis) {
            fragmentUi.resetSyncProgress();
        }
        mSyncManager.forceSync();
    }

    public void attachFragmentUi(TentFragmentUi fragmentUi) {
        LOG.d("Attached new fragment UI: " + fragmentUi);
        mFragmentUis.add(fragmentUi);
        updateUi();
    }

    public void detachFragmentUi(TentFragmentUi fragmentUi) {
        LOG.d("Detached fragment UI: " + fragmentUi);
        mFragmentUis.remove(fragmentUi);
    }

    /** Frees any resources used by the controller. */
    public void suspend() {
        LOG.d("Controller suspended.");

        if (mAppLocationTree != null) {
            mAppLocationTree.close();
        }
        mCrudEventBus.unregister(mEventBusSubscriber);
        mEventBus.unregister(mEventBusSubscriber);
	}

    /** Call when the user presses the search button. */
    public void onSearchPressed() {
        mUi.switchToPatientListScreen();
    }

    /** Call when the user exits search mode. */
    public void onSearchCancelled() {
        mUi.switchToTentSelectionScreen();
    }

    /** Call when the user presses the discharged zone. */
    public void onDischargedPressed() {
        mUi.launchActivityForLocation(mDischargedZone);
    }

	/** Call when the user presses the triage zone. */
    public void onTriagePressed() {
        mUi.launchActivityForLocation(mTriageZone);
    }

    /** Call when the user presses a tent. */
    public void onTentSelected(AppLocation tent) {
        mUi.launchActivityForLocation(tent);
    }

    private void updateUi() {
        boolean hasValidTree = isLocationTreeValid();
        updateLoadingState();
        for (TentFragmentUi fragmentUi : mFragmentUis) {
            fragmentUi.setBusyLoading(!hasValidTree);

            if (hasValidTree) {
                int dischargedPatientCount = (mDischargedZone == null)
                        ? 0 : mAppLocationTree.getTotalPatientCount(mDischargedZone);
                int totalPatientCount =
                        mAppLocationTree.getTotalPatientCount(mAppLocationTree.getRoot());
                fragmentUi.setTents(
                        mAppLocationTree,
                        mAppLocationTree.getDescendantsAtDepth(
                                AppLocationTree.ABSOLUTE_DEPTH_TENT).asList());
                fragmentUi.setPresentPatientCount(totalPatientCount - dischargedPatientCount);
                fragmentUi.setDischargedPatientCount(
                        (mDischargedZone == null)
                                ? 0 : mAppLocationTree.getTotalPatientCount(mDischargedZone));
                fragmentUi.setTriagePatientCount(
                        (mTriageZone == null)
                                ? 0 : mAppLocationTree.getTotalPatientCount(mTriageZone));
            }
        }
    }

    private void updateLoadingState() {
        boolean hasLocationTree = isLocationTreeValid();
        if (hasLocationTree) {
            mUi.setLoadingState(LoadingState.LOADED);
            return;
        }

        if (mSyncManager.isSyncing() || mSyncManager.isSyncPending()) {
            mUi.setLoadingState(LoadingState.SYNCING);
            return;
        }

        mUi.setLoadingState(LoadingState.LOADING);
    }

    @SuppressWarnings("unused") // Called by reflection from EventBus
    private final class EventBusSubscriber {

        public void onEventMainThread(SyncCancelRequestedEvent event) {
            for (TentFragmentUi fragmentUi : mFragmentUis) {
                fragmentUi.showSyncCancelRequested();
            }
        }

        public void onEventMainThread(SyncCanceledEvent event) {
            mUi.finish();
        }

        public void onEventMainThread(SyncProgressEvent event) {
            for (TentFragmentUi fragmentUi : mFragmentUis) {
                fragmentUi.showIncrementalSyncProgress(event.progress, event.label);
            }
        }

        public void onEventMainThread(SyncStartedEvent event) {
            for (TentFragmentUi fragmentUi : mFragmentUis) {
                fragmentUi.resetSyncProgress();
            }
        }

        public void onEventMainThread(SyncSucceededEvent event) {
            mUi.showSyncFailedDialog(false);

            // Reload locations from the local datastore when a full sync completes successfully.
            if (mAppModel.isFullModelAvailable()) {
                LOG.i("Data model is available after sync; loading location tree.");
                mAppModel.fetchLocationTree(
                        mCrudEventBus, LocaleSelector.getCurrentLocale().getLanguage());
            } else if (!isLocationTreeValid()) {
                LOG.i("Sync succeeded but was incomplete; forcing a new sync.");
                onSyncRetry();
            }
        }

        public void onEventMainThread(SyncFailedEvent event) {
            for (TentFragmentUi fragmentUi : mFragmentUis) {
                fragmentUi.resetSyncProgress();
            }
            mUi.showSyncFailedDialog(true);
        }

        public void onEventMainThread(AppLocationTreeFetchedEvent event) {
            if (mAppLocationTree != null) {
                mAppLocationTree.close();
            }
            mAppLocationTree = event.tree;
            if (!isLocationTreeValid()) {
                LOG.i("Found no locations in the local datastore; waiting on sync.");
                mUi.setLoadingState(LoadingState.SYNCING); // Ensure cancel button shows up.
                return;
            }

            LOG.i("Received a valid location tree.");
            for (AppLocation zone :
                    mAppLocationTree.getChildren(mAppLocationTree.getRoot())) {
                switch (zone.uuid) {
                    case Zone.TRIAGE_ZONE_UUID:
                        mTriageZone = zone;
                        break;
                    // TODO(akalachman): Revisit if discharged should be treated differently.
                    case Zone.DISCHARGED_ZONE_UUID:
                        mDischargedZone = zone;
                        break;
                    default:
                        break;
                }
            }

            updateUi();

            // Update the search controller immediately -- it does not listen for location updates
            // on this controller's bus and would otherwise be unaware of changes.
            mPatientSearchController.setLocations(mAppLocationTree);
        }
    }
}
