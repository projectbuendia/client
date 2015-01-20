package org.msf.records.ui.patientlist;

import android.util.Log;

import com.google.common.eventbus.EventBus;

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
import org.msf.records.events.sync.SyncSucceededEvent;
import org.msf.records.filter.db.PatientDbFilters;
import org.msf.records.filter.db.SimpleSelectionFilterGroup;
import org.msf.records.filter.db.LocationUuidFilter;
import org.msf.records.filter.db.SimpleSelectionFilter;
import org.msf.records.filter.matchers.FilteredCursorWrapper;
import org.msf.records.filter.matchers.IdFilter;
import org.msf.records.filter.matchers.MatchingFilter;
import org.msf.records.filter.matchers.MatchingFilterGroup;
import org.msf.records.filter.matchers.NameFilter;
import org.msf.records.utils.EventBusRegistrationInterface;

import java.util.HashSet;
import java.util.Set;

import static org.msf.records.filter.matchers.MatchingFilterGroup.FilterType.OR;

/**
 * Controller for {@link PatientSearchActivity}.
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
    private final EventBusRegistrationInterface mGlobalEventBus;
    private final AppModel mModel;
    private final Set<FragmentUi> mFragmentUis = new HashSet<>();

    private AppLocationTree mLocationTree;
    private String mRootLocationUuid;

    private SimpleSelectionFilter mFilter;
    private String mFilterQueryTerm = "";
    private FilterSubscriber mFilterSubscriber;
    private final Object mFilterSubscriberLock = new Object();

    private boolean mWaitingOnLocationTree = false;

    private final MatchingFilter<AppPatient> mSearchFilter =
            new MatchingFilterGroup<AppPatient>(OR, new IdFilter(), new NameFilter());
    private TypedCursor<AppPatient> mPatientsCursor;
    private final SyncSubscriber mSyncSubscriber;

    /**
     * Instantiates a {@link PatientSearchController} with the given UI implementation, event bus,
     * app model, and locale. The global event bus must also be passed in in order to reload results
     * after a sync.
     *
     * @param ui a {@link Ui} that will respond to UI events
     * @param crudEventBus a {@link CrudEventBus} that will listen for patient and location fetch
     *                     events
     * @param globalEventBus a {@link EventBusRegistrationInterface} that will listen for sync
     *                       events
     * @param model an {@link AppModel} for fetching patient and location data
     * @param locale a language code/locale for presenting localized information (e.g. en)
     */
    public PatientSearchController(
            Ui ui,
            CrudEventBus crudEventBus,
            EventBusRegistrationInterface globalEventBus,
            AppModel model, String locale) {
        mUi = ui;
        mCrudEventBus = crudEventBus;
        mGlobalEventBus = globalEventBus;
        mModel = model;

        mCrudEventBus.register(new LocationTreeUpdatedSubscriber());
        mModel.fetchLocationTree(mCrudEventBus, locale);

        mFilter = PatientDbFilters.getDefaultFilter();

        mSyncSubscriber = new SyncSubscriber();
    }

    public void init() {
        mGlobalEventBus.register(mSyncSubscriber);
    }

    public void suspend() {
        mGlobalEventBus.unregister(mSyncSubscriber);
        // Close any outstanding patient cursor. New results will be fetched when requested.
        if (mPatientsCursor != null) {
            mPatientsCursor.close();
        }
    }

    private class SyncSubscriber {
        public void onEventMainThread(SyncSucceededEvent event) {
            // Load search results, but don't show the spinner, as the user may be in the middle
            // of performing an operation.
            loadSearchResults(false);
        }
    }

    private class LocationTreeUpdatedSubscriber {
        public synchronized void onEventMainThread(AppLocationTreeFetchedEvent event) {
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

        public synchronized void onEventMainThread(LocationsLoadFailedEvent event) {
            mUi.showErrorMessage(R.string.location_load_error);
            for (FragmentUi fragmentUi : mFragmentUis) {
                fragmentUi.showSpinner(false);
            }
        }
    }

    /**
     * Registers a {@link FragmentUi} with this controller for the purposes of subscribing to
     * events.
     */
    public void attachFragmentUi(FragmentUi fragmentUi) {
        if (DEBUG) {
            Log.d(TAG, "Attached new fragment UI: " + fragmentUi);
        }
        mFragmentUis.add(fragmentUi);
    }

    /**
     * Unregisters a {@link FragmentUi} with this controller for the purposes of subscribing to
     * events.
     */
    public void detachFragmentUi(FragmentUi fragmentUi) {
        if (DEBUG) {
            Log.d(TAG, "Detached fragment UI: " + fragmentUi);
        }
        mFragmentUis.remove(fragmentUi);
    }

    /**
     * Responds to a patient being selected.
     *
     * @param patient the selected {@link org.msf.records.data.app.AppPatient}
     */
    public void onPatientSelected(AppPatient patient) {
        mUi.launchChartActivity(patient.uuid, patient.givenName, patient.familyName, patient.id);
    }

    /**
     * Responds to a change in the search query.
     *
     * @param constraint the search query
     */
    public void onQuerySubmitted(String constraint) {
        App.getServer().cancelPendingRequests();

        mFilterQueryTerm = constraint;

        if (mPatientsCursor == null) {
            loadSearchResults();
            return;
        }
        updatePatients();
    }

    /**
     * Sets a root location for the purposes of filtering.
     *
     * @param locationUuid UUID of the location to filter by
     */
    public void setLocationFilter(String locationUuid) {
        mRootLocationUuid = locationUuid;
    }

    /**
     * Sets the filter to filter by, which may be (optionally) in conjunction with a root location
     * specified by {@link PatientSearchController#setLocationFilter(String)}.
     *
     * @param filter the {@link SimpleSelectionFilter} that will be applied to search results
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
                filter = new SimpleSelectionFilterGroup(new LocationUuidFilter(mLocationTree), mFilter);
            } else {
                filter = new SimpleSelectionFilterGroup(new LocationUuidFilter(
                        mLocationTree, mLocationTree.findByUuid(mRootLocationUuid)), mFilter);
            }
        }

        return filter;
    }

    public void loadSearchResults() {
        loadSearchResults(true); // By default, show spinner.
    }

    /**
     * Asynchronously loads or reloads the search results based on previously specified filter and
     * root location. If no filter is specified, all results are shown be default.
     * @param showSpinner whether or not to show a spinner until operation is complete
     */
    public void loadSearchResults(boolean showSpinner) {
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

        if (showSpinner) {
            for (FragmentUi fragmentUi : mFragmentUis) {
                fragmentUi.showSpinner(true);
            }
        }
        mCrudEventBus.register(mFilterSubscriber);
        mModel.fetchPatients(mCrudEventBus, getLocationSubfilter(), mFilterQueryTerm);
    }

    private void updatePatients() {
        FilteredCursorWrapper<AppPatient> filteredCursorWrapper =
                new FilteredCursorWrapper<AppPatient>(
                        mPatientsCursor, mSearchFilter, mFilterQueryTerm);
        mUi.setPatients(filteredCursorWrapper);
        for (FragmentUi fragmentUi : mFragmentUis) {
            fragmentUi.setPatients(filteredCursorWrapper);
            fragmentUi.showSpinner(false);
        }
    }

    private final class FilterSubscriber {
        public void onEventMainThread(TypedCursorFetchedEvent<AppPatient> event) {
            mCrudEventBus.unregister(this);

            // If a patient cursor was already open, close it.
            if (mPatientsCursor != null) {
                mPatientsCursor.close();
            }

            // Replace the patient cursor with the newly-fetched results.
            mPatientsCursor = event.cursor;
            updatePatients();
        }
    }
}