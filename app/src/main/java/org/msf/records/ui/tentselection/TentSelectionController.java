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
import org.msf.records.model.Zone;
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
	}

	public interface TentFragmentUi {
		void setTents(AppLocationTree locationTree, List<AppLocation> tents);
		void setPatientCount(int patientCount);
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

	private boolean mLoadedLocationTree;
	private long mLoadRequestTimeMs;
	@Nullable private AppLocationTree mAppLocationTree;
	@Nullable private AppLocation mTriageZone;
	@Nullable private AppLocation mDischargedZone;

	public TentSelectionController(
            AppModel appModel,
            CrudEventBus crudEventBus,
            Ui ui,
            EventBusRegistrationInterface eventBus) {
        mAppModel = appModel;
        mCrudEventBus = crudEventBus;
        mUi = ui;
		mEventBus = eventBus;
	}

	public void init() {
		mEventBus.register(mEventBusSubscriber);
        mCrudEventBus.register(mEventBusSubscriber);

        LOG.d("Controller inited. Loaded tree: "
                + mLoadedLocationTree + ". Tree: " + mAppLocationTree);

        mAppModel.fetchLocationTree(mCrudEventBus, LocaleSelector.getCurrentLocale().getLanguage());

		if (!mLoadedLocationTree) {
			mLoadRequestTimeMs = SystemClock.elapsedRealtime();
		}
		for (TentFragmentUi fragmentUi : mFragmentUis) {
			populateFragmentUi(fragmentUi);
		}
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
		fragmentUi.showSpinner(!mLoadedLocationTree);
		if (mAppLocationTree != null) {
			fragmentUi.setTents(
                    mAppLocationTree,
                    mAppLocationTree.getDescendantsAtDepth(
                            AppLocationTree.ABSOLUTE_DEPTH_TENT).asList());
	    	fragmentUi.setPatientCount(
                    mAppLocationTree == null ?
                            0 : mAppLocationTree.getTotalPatientCount(mAppLocationTree.getRoot()));
	        fragmentUi.setDischargedPatientCount(
                    mDischargedZone == null ?
                            0 : mAppLocationTree.getTotalPatientCount(mDischargedZone));
	    	fragmentUi.setTriagePatientCount(
                    mTriageZone == null ? 0 : mAppLocationTree.getTotalPatientCount(mTriageZone));
		}
	}


	@SuppressWarnings("unused") // Called by reflection from EventBus
	private final class EventBusSubscriber {

        public void onEventMainThread(AppLocationTreeFetchedEvent event) {
            mLoadedLocationTree = true;
            mAppLocationTree = event.tree;
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
        }
	}
}