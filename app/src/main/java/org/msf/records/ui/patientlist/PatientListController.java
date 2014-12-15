package org.msf.records.ui.patientlist;

import javax.annotation.Nullable;

import org.msf.records.R;
import org.msf.records.data.app.AppModel;
import org.msf.records.data.app.AppPatient;
import org.msf.records.data.app.TypedCursor;
import org.msf.records.events.CreatePatientSucceededEvent;
import org.msf.records.events.CrudEventBus;
import org.msf.records.events.data.TypedCursorFetchedEvent;
import org.msf.records.events.location.LocationsLoadFailedEvent;
import org.msf.records.events.location.LocationsLoadedEvent;
import org.msf.records.events.sync.SyncFinishedEvent;
import org.msf.records.filter.FilterGroup;
import org.msf.records.filter.LocationUuidFilter;
import org.msf.records.filter.SimpleSelectionFilter;
import org.msf.records.location.LocationManager;
import org.msf.records.location.LocationTree;
import org.msf.records.sync.SyncManager;

import de.greenrobot.event.EventBus;

final class PatientListController {

	private static final String TAG = PatientListController.class.getSimpleName();

	public interface Ui {
		void setCursor(TypedCursor<AppPatient> cursor);
		void showErrorToast(int stringResourceId);
		void showRefreshSpinner(boolean show);
	}

	private final SyncManager mSyncManager;
	private final AppModel mAppModel;
    private final EventBusSubscriber mEventBusSubscriber = new EventBusSubscriber();
    private final CrudEventBus mCrudEventBus;
    private final EventBus mEventBus;
    private final LocationManager mLocationManager;

    private final Ui mUi;

    private boolean mIsSyncing;
    private boolean mIsLoadingCursor;

    private SimpleSelectionFilter mFilter;
    private String mFilterQueryTerm = "";
    private String mConstraint = "";

    @Nullable private LocationTree mLocationTree;

    public PatientListController(
    		SyncManager syncManager,
    		AppModel appModel,
    		LocationManager locationManager,
    		CrudEventBus crudEventBus,
    		EventBus eventBus,
    		Ui ui,
    	    SimpleSelectionFilter initialFilter,
    	    LocationTree locationTree) {
    	mSyncManager = syncManager;
    	mAppModel = appModel;
    	mLocationManager = locationManager;
    	mCrudEventBus = crudEventBus;
    	mEventBus = eventBus;
    	mUi = ui;
    	mFilter = initialFilter;
    	mLocationTree = locationTree;
	}

	public void init() {
		mEventBus.register(mEventBusSubscriber);
		mLocationManager.loadLocations();
    }

    public void suspend() {
    	mEventBus.unregister(mEventBusSubscriber);
    }

    /**
     * Sets the current patient filter.
     */
    public void setFilter(SimpleSelectionFilter filter) {
        // Tack on a location filter to the filter to show only known locations.
        if (mLocationTree == null || mLocationTree.getRoot().getLocation() == null) {
            mFilter = filter;
        } else {
            // Tack on a location filter to the filter to show only known locations.
            mFilter = new FilterGroup(new LocationUuidFilter(mLocationTree.getRoot()), filter);
        }
        loadSearchResults();
    }

    public void setConstraint(String constraint) {
    	mConstraint = constraint;
    	loadSearchResults();
    }

    private void loadSearchResults() {
    	mIsLoadingCursor = true;
        mAppModel.fetchPatients(mCrudEventBus, mFilter, mConstraint);
        updateUi();
    }

    @SuppressWarnings("unused") // Called by reflection from event bus.
    private final class EventBusSubscriber {
	    public void onEventMainThread(LocationsLoadedEvent event) {
	    	mLocationTree = event.locationTree;
	    }

	    public void onEventMainThread(LocationsLoadFailedEvent event) {
	    	mUi.showErrorToast(R.string.location_load_error);
	    }

	    public void onEventMainThread(SyncFinishedEvent event) {
	    	mUi.showRefreshSpinner(false);
	    }

	    public void onEventMainThread(CreatePatientSucceededEvent event) {
	    	resync();
	    }

	    public void onEventMainThread(TypedCursorFetchedEvent<AppPatient> event) {

	    }
    }

	public void onRefreshRequested() {
		resync();
	}


    private void resync() {
		if (!mIsSyncing) {
            mSyncManager.forceSync();
        }
		mIsSyncing = true;
		updateUi();
    }

	private void updateUi() {
		mUi.showRefreshSpinner(mIsLoadingCursor || mIsSyncing);
	}
}
