package org.msf.records.ui.patientlist;

import android.util.Log;

import org.msf.records.App;
import org.msf.records.R;
import org.msf.records.data.app.AppLocationTree;
import org.msf.records.data.app.AppModel;
import org.msf.records.data.app.AppPatient;
import org.msf.records.data.app.TypedCursor;
import org.msf.records.events.CrudEventBus;
import org.msf.records.events.data.AppLocationTreeFetchedEvent;
import org.msf.records.events.data.SingleItemCreatedEvent;
import org.msf.records.events.data.TypedCursorFetchedEvent;
import org.msf.records.events.location.LocationsLoadFailedEvent;
import org.msf.records.events.sync.SyncFailedEvent;
import org.msf.records.events.sync.SyncFinishedEvent;
import org.msf.records.filter.FilterGroup;
import org.msf.records.filter.LocationUuidFilter;
import org.msf.records.filter.PatientFilters;
import org.msf.records.filter.SimpleSelectionFilter;

import java.util.HashSet;
import java.util.Set;

/**
 * Controller for {@link org.msf.records.ui.patientlist.PatientListActivity}.
 *
 * Avoid adding untestable dependencies to this class.
 */
public class PatientSearchController {

    private static final String TAG = PatientSearchController.class.getSimpleName();
    private static final boolean DEBUG = true;

    public interface Ui {
        void launchChartActivity(String uuid, String givenName, String familyName, String id);
        void showErrorMessage(int message);
        void showErrorMessage(String message);
    }

    public interface FragmentUi {
        void notifyDataSetChanged();
        void setLocations(AppLocationTree locationTree);
        void setPatients(TypedCursor<AppPatient> patients);
        void showSpinner(boolean show);
        void showRefreshIndicator(boolean show);
    }

    private final Ui mUi;
    private final CrudEventBus mCrudEventBus;
    private final AppModel mModel;
    private final EventSubscriber mEventBusSubscriber;
    private final Set<FragmentUi> mFragmentUis = new HashSet<>();

    private AppLocationTree mLocationTree;
    private String mRootLocationUuid;

    private SimpleSelectionFilter mFilter;
    private String mFilterQueryTerm = "";
    private FilterSubscriber mFilterSubscriber;
    private final Object mFilterSubscriberLock = new Object();

    public PatientSearchController(
            Ui ui, CrudEventBus crudEventBus, AppModel model, String locale) {
        mUi = ui;
        mCrudEventBus = crudEventBus;
        mModel = model;

        mCrudEventBus.register(new LocationTreeUpdatedSubscriber());
        mModel.fetchLocationTree(mCrudEventBus, locale);

        // TODO(dxchen): Inject this.
        mEventBusSubscriber = new EventSubscriber();

        mFilter = PatientFilters.getDefaultFilter();
        // TODO(akalachman): Immediately load search results?
    }

    private class LocationTreeUpdatedSubscriber {
        public synchronized void onEvent(AppLocationTreeFetchedEvent event) {
            synchronized(mFilterSubscriberLock) {
                mCrudEventBus.unregister(this);
                mFilterSubscriber = null;
            }
            mLocationTree = event.tree;
            for (FragmentUi fragmentUi : mFragmentUis) {
                fragmentUi.showSpinner(false);
            }
        }

        public synchronized void onEvent(LocationsLoadFailedEvent event) {
            mUi.showErrorMessage(R.string.location_load_error);
            for (FragmentUi fragmentUi : mFragmentUis) {
                fragmentUi.showSpinner(false);
            }
        }
    }

    /** Initializes the controller, setting async operations going to collect data required by the UI. */
    public void init() {
        mCrudEventBus.register(mEventBusSubscriber);
    }

    public void attachFragmentUi(FragmentUi fragmentUi) {
        if (DEBUG) {
            Log.d(TAG, "Attached new fragment UI: " + fragmentUi);
        }
        mFragmentUis.add(fragmentUi);
    }

    public void detachFragmentUi(FragmentUi fragmentUi) {
        if (DEBUG) {
            Log.d(TAG, "Detached fragment UI: " + fragmentUi);
        }
        mFragmentUis.remove(fragmentUi);
    }

    /** Releases any resources used by the controller. */
    public void suspend() {
        mCrudEventBus.unregister(mEventBusSubscriber);
    }

    public void onPatientSelected(AppPatient patient) {
        mUi.launchChartActivity(patient.uuid, patient.givenName, patient.familyName, patient.id);
    }

    public void onQuerySubmitted(String constraint) {
        App.getServer().cancelPendingRequests();
        mFilterQueryTerm = constraint;
        loadSearchResults();
    }

    public void applyLocationFilter(String locationUuid) {
        mRootLocationUuid = locationUuid;
        loadSearchResults();
    }

    public void applyFilter(SimpleSelectionFilter filter) {
        mFilter = filter;
        loadSearchResults();
    }

    private SimpleSelectionFilter getLocationSubfilter() {
        SimpleSelectionFilter filter;

        // Tack on a location filter to the filter to show only known locations.
        if (mLocationTree == null || mLocationTree.getRoot() == null) {
            filter = mFilter;
        } else {
            // Tack on a location filter to the filter to show only known locations in the subtree
            // of the current root.
            if (mRootLocationUuid == null) {
                filter = new FilterGroup(new LocationUuidFilter(mLocationTree), mFilter);
            } else {
                filter = new FilterGroup(new LocationUuidFilter(
                        mLocationTree, mLocationTree.findByUuid(mRootLocationUuid)), mFilter);
            }
        }

        return filter;
    }

    private void loadSearchResults() {
        // Ensure only one subscriber is listening to filter events.
        synchronized (mFilterSubscriberLock) {
            if (mFilterSubscriber != null) {
                mCrudEventBus.unregister(mFilterSubscriber);
            }
        }
        mFilterSubscriber = new FilterSubscriber();

        // TODO(akalachman): Sub-filter on query term rather than re-filtering with each keypress.
        for (FragmentUi fragmentUi : mFragmentUis) {
            fragmentUi.showSpinner(true);
        }
        mCrudEventBus.register(mFilterSubscriber);
        mModel.fetchPatients(mCrudEventBus, getLocationSubfilter(), mFilterQueryTerm);
    }

    private final class FilterSubscriber {
        public void onEventMainThread(TypedCursorFetchedEvent<AppPatient> event) {
            mCrudEventBus.unregister(this);
            for (FragmentUi fragmentUi : mFragmentUis) {
                fragmentUi.setPatients(event.cursor);
                if (mLocationTree != null) {
                    fragmentUi.showSpinner(false);
                }
            }
            event.cursor.close();
        }
    }

    @SuppressWarnings("unused") // Called by reflection from EventBus.
    private final class EventSubscriber {

        public void onEvent(SingleItemCreatedEvent<AppPatient> event) {
            loadSearchResults();
        }

        public synchronized void onEvent(SyncFinishedEvent event) {
            for (FragmentUi fragmentUi : mFragmentUis) {
                fragmentUi.showRefreshIndicator(false);
            }
        }

        public synchronized void onEvent(SyncFailedEvent event) {
            for (FragmentUi fragmentUi : mFragmentUis) {
                fragmentUi.showRefreshIndicator(false);
            }
            mUi.showErrorMessage(R.string.patient_sync_failed);
            Log.e(TAG, "Sync event failed");
        }
    }
}