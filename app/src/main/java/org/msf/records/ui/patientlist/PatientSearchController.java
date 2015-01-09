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
            mCrudEventBus.unregister(this);
            mLocationTree = event.tree;
            for (FragmentUi fragmentUi : mFragmentUis) {
                fragmentUi.setLocations(mLocationTree);
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
        App.getServer().cancelPendingRequests(TAG);
        mFilterQueryTerm = constraint;
        loadSearchResults();
    }

    public void setRootLocationUuid(String locationUuid) {
        mRootLocationUuid = locationUuid;
    }

    public void setFilter(SimpleSelectionFilter filter) {
        // Tack on a location filter to the filter to show only known locations.
        if (mLocationTree == null || mLocationTree.getRoot() == null) {
            mFilter = filter;
        } else {
            // Tack on a location filter to the filter to show only known locations in the subtree
            // of the current root.
            if (mRootLocationUuid == null) {
                mFilter = new FilterGroup(new LocationUuidFilter(mLocationTree), filter);
            } else {
                mFilter = new FilterGroup(new LocationUuidFilter(
                        mLocationTree, mLocationTree.findByUuid(mRootLocationUuid)), filter);
            }
        }

        loadSearchResults();
    }

    private void loadSearchResults() {
        // TODO(akalachman): Sub-filter on query term rather than re-filtering with each keypress.
        for (FragmentUi fragmentUi : mFragmentUis) {
            fragmentUi.showSpinner(true);
        }
        // TODO(akalachman): Need specific filter event bus?
        mCrudEventBus.register(new FilterSubscriber());
        mModel.fetchPatients(mCrudEventBus, mFilter, mFilterQueryTerm);
    }

    private final class FilterSubscriber {
        public void onEventMainThread(TypedCursorFetchedEvent<AppPatient> event) {
            for (FragmentUi fragmentUi : mFragmentUis) {
                fragmentUi.setPatients(event.cursor);
                fragmentUi.showSpinner(false);
            }
            event.cursor.close();

            mCrudEventBus.unregister(this);
        }
    }

    @SuppressWarnings("unused") // Called by reflection from EventBus.
    private final class EventSubscriber {

        public void onEvent(SingleItemCreatedEvent<AppPatient> event) {
            for (FragmentUi fragmentUi : mFragmentUis) {
                fragmentUi.notifyDataSetChanged();
            }
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