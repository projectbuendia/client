package org.msf.records.ui.tentselection;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.msf.records.data.app.AppLocation;
import org.msf.records.data.app.AppLocationTree;
import org.msf.records.data.app.AppModel;
import org.msf.records.events.CrudEventBus;
import org.msf.records.events.data.AppLocationTreeFetchedEvent;
import org.msf.records.events.sync.SyncFailedEvent;
import org.msf.records.events.sync.SyncFinishedEvent;
import org.msf.records.events.sync.SyncSucceededEvent;
import org.msf.records.model.Zone;
import org.msf.records.sync.SyncManager;
import org.msf.records.ui.patientlist.PatientSearchController;
import org.msf.records.utils.EventBusRegistrationInterface;
import org.msf.records.utils.LocaleSelector;
import org.msf.records.utils.Logger;

import android.os.SystemClock;

import com.google.common.collect.ImmutableSet;

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
    }

    public interface TentFragmentUi {

        void setTents(AppLocationTree locationTree, List<AppLocation> tents);

        void setPresentPatientCount(int patientCount);

        void setTriagePatientCount(int patientCount);

        void setDischargedPatientCount(int dischargedPatientCount);

        void showSpinner(boolean show);
	}

    private final AppModel mAppModel;
    private final CrudEventBus mCrudEventBus;
    private final Ui mUi;
    private final Set<TentFragmentUi> mFragmentUis = new HashSet<>();
    private final EventBusRegistrationInterface mEventBus;
    private final EventBusSubscriber mEventBusSubscriber = new EventBusSubscriber();
    private final SyncManager mSyncManager;
    private final PatientSearchController mPatientSearchController;

    private boolean mLoadedLocationTree;
    private boolean mWaitingForSync = false;
    private long mLoadRequestTimeMs;
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

        LOG.d("Controller inited. Loaded tree: %1$s. Tree: %2$s",
                mLoadedLocationTree, mAppLocationTree);

        mAppModel.fetchLocationTree(mCrudEventBus, LocaleSelector.getCurrentLocale().getLanguage());
        mWaitingForSync = mSyncManager.isSyncing();

        LOG.d("Waiting for sync before showing tents? %b", mWaitingForSync);

        if (!mLoadedLocationTree) {
            mLoadRequestTimeMs = SystemClock.elapsedRealtime();
        }
        for (TentFragmentUi fragmentUi : mFragmentUis) {
            populateFragmentUi(fragmentUi);
        }
    }

    public void onSyncRetry() {
        mSyncManager.forceSync();
    }

    public void attachFragmentUi(TentFragmentUi fragmentUi) {
        LOG.d("Attached new fragment UI: " + fragmentUi);
        mFragmentUis.add(fragmentUi);
        populateFragmentUi(fragmentUi);
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

    private void populateFragmentUi(TentFragmentUi fragmentUi) {
        if (mAppLocationTree != null && !mWaitingForSync) {
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
                    (mTriageZone == null) ? 0 : mAppLocationTree.getTotalPatientCount(mTriageZone));
        }
        fragmentUi.showSpinner(!mLoadedLocationTree);
    }

    @SuppressWarnings("unused") // Called by reflection from EventBus
    private final class EventBusSubscriber {

        public void onEventMainThread(SyncSucceededEvent event) {
            mUi.showSyncFailedDialog(false);

            mWaitingForSync = false;

            // Reload locations when a sync completes.
            mAppModel.fetchLocationTree(
                    mCrudEventBus, LocaleSelector.getCurrentLocale().getLanguage());
        }

        public void onEventMainThread(SyncFailedEvent event) {
            mUi.showSyncFailedDialog(true);
        }

        public void onEventMainThread(AppLocationTreeFetchedEvent event) {
            LOG.i("Location tree fetched.");
            if (mAppLocationTree != null) {
                mAppLocationTree.close();
            }
            mAppLocationTree = event.tree;
            if (mAppLocationTree == null || mAppLocationTree.getRoot() == null) {
                LOG.w("Location tree was null or had null root.");
                mLoadedLocationTree = false;
            } else {
                mLoadedLocationTree = true;
            }
            ImmutableSet<AppLocation> zones =
                    mAppLocationTree.getChildren(mAppLocationTree.getRoot());
            for (AppLocation zone : zones) {
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

            for (TentFragmentUi fragmentUi : mFragmentUis) {
                populateFragmentUi(fragmentUi);
            }

            // Update the search controller immediately -- it does not listen for location updates
            // on this controller's bus and would otherwise be unaware of changes.
            mPatientSearchController.setLocations(mAppLocationTree);
        }
    }
}