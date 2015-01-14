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
import org.msf.records.events.data.TypedCursorFetchedEvent;
import org.msf.records.events.location.LocationsLoadFailedEvent;
import org.msf.records.filter.FilterGroup;
import org.msf.records.filter.LocationUuidFilter;
import org.msf.records.filter.PatientFilters;
import org.msf.records.filter.SimpleSelectionFilter;

import java.util.HashSet;
import java.util.Set;

/**
 * Controller for {@link org.msf.records.ui.patientlist.PatientSearchActivity}.
 *
 * <p>Avoid adding untestable dependencies to this class.
 */
public class PatientSearchController {

    private static final String TAG = PatientSearchController.class.getSimpleName();
    private static final boolean DEBUG = true;

    public interface Ui {

        void launchChartActivity(String uuid, String givenName, String familyName, String id);

        void setPatients(TypedCursor<AppPatient> patients);

        void showErrorMessage(int message);
    }

    public interface FragmentUi {

        void setPatients(TypedCursor<AppPatient> patients);

        void showSpinner(boolean show);
    }

    private final Ui mUi;
    private final CrudEventBus mCrudEventBus;
    private final AppModel mModel;
    private final Set<FragmentUi> mFragmentUis = new HashSet<>();

    private AppLocationTree mLocationTree;
    private String mRootLocationUuid;

    private SimpleSelectionFilter mFilter;
    private String mFilterQueryTerm = "";
    private FilterSubscriber mFilterSubscriber;
    private final Object mFilterSubscriberLock = new Object();

    private boolean mWaitingOnLocationTree = false;

    /**
     * Instantiates a {@link org.msf.records.ui.patientlist.PatientSearchController} with the
     * given UI implementation, event bus, app model, and locale.
     * @param ui a {@link org.msf.records.ui.patientlist.PatientSearchController.Ui} that will
     *           respond to UI events
     * @param crudEventBus a {@link org.msf.records.events.CrudEventBus} that will listen for
     *                     patient and location fetch events
     * @param model an {@link org.msf.records.data.app.AppModel} for fetching patient and location
     *              data
     * @param locale a language code/locale for presenting localized information (e.g. en)
     */
    public PatientSearchController(
            Ui ui, CrudEventBus crudEventBus, AppModel model, String locale) {
        mUi = ui;
        mCrudEventBus = crudEventBus;
        mModel = model;

        mCrudEventBus.register(new LocationTreeUpdatedSubscriber());
        mModel.fetchLocationTree(mCrudEventBus, locale);

        mFilter = PatientFilters.getDefaultFilter();
    }

    private class LocationTreeUpdatedSubscriber {
        public synchronized void onEvent(AppLocationTreeFetchedEvent event) {
            synchronized (mFilterSubscriberLock) {
                mCrudEventBus.unregister(this);
                mFilterSubscriber = null;
            }
            mLocationTree = event.tree;

            // If showing results was blocked on having a location tree, request results
            // immediately.
            if (mWaitingOnLocationTree) {
                mWaitingOnLocationTree = false;
                loadSearchResults();
            }
        }

        public synchronized void onEvent(LocationsLoadFailedEvent event) {
            mUi.showErrorMessage(R.string.location_load_error);
            for (FragmentUi fragmentUi : mFragmentUis) {
                fragmentUi.showSpinner(false);
            }
        }
    }

    /**
     * Registers a {@link org.msf.records.ui.patientlist.PatientSearchController.FragmentUi} with
     * this controller for the purposes of subscribing to events.
     * @param fragmentUi a {@link org.msf.records.ui.patientlist.PatientSearchController.FragmentUi}
     *                   to add as a subscriber
     */
    public void attachFragmentUi(FragmentUi fragmentUi) {
        if (DEBUG) {
            Log.d(TAG, "Attached new fragment UI: " + fragmentUi);
        }
        mFragmentUis.add(fragmentUi);
    }

    /**
     * Un-registers a {@link org.msf.records.ui.patientlist.PatientSearchController.FragmentUi} with
     * this controller for the purposes of subscribing to events.
     * @param fragmentUi a {@link org.msf.records.ui.patientlist.PatientSearchController.FragmentUi}
     *                   to remove as a subscriber
     */
    public void detachFragmentUi(FragmentUi fragmentUi) {
        if (DEBUG) {
            Log.d(TAG, "Detached fragment UI: " + fragmentUi);
        }
        mFragmentUis.remove(fragmentUi);
    }

    /**
     * Responds to a patient being selected.
     * @param patient the selected {@link org.msf.records.data.app.AppPatient}
     */
    public void onPatientSelected(AppPatient patient) {
        mUi.launchChartActivity(patient.uuid, patient.givenName, patient.familyName, patient.id);
    }

    /**
     * Responds to a change in the search query.
     * @param constraint the search query
     */
    public void onQuerySubmitted(String constraint) {
        App.getServer().cancelPendingRequests();
        mFilterQueryTerm = constraint;
        loadSearchResults();
    }

    /**
     * Sets a root location for the purposes of filtering.
     * @param locationUuid UUID of the location to filter by
     */
    public void setLocationFilter(String locationUuid) {
        mRootLocationUuid = locationUuid;
    }

    /**
     * Sets the filter to filter by, which may be (optionally) in conjunction with a root location
     * specified by {@link PatientSearchController#setLocationFilter(String)}.
     * @param filter the {@link org.msf.records.filter.SimpleSelectionFilter} that will be applied
     *               to search results
     */
    public void setFilter(SimpleSelectionFilter filter) {
        mFilter = filter;
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

    /**
     * Asynchronously loads or reloads the search results based on previously specified filter and
     * root location. If no filter is specified, all results are shown be default.
     */
    public void loadSearchResults() {
        // If a location filter is applied but no location tree is present, wait.
        if (mRootLocationUuid != null && mLocationTree == null) {
            mWaitingOnLocationTree = true;
            return;
        }

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
            mUi.setPatients(event.cursor);
            for (FragmentUi fragmentUi : mFragmentUis) {
                fragmentUi.setPatients(event.cursor);
                fragmentUi.showSpinner(false);
            }
            event.cursor.close();
        }
    }
}