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

import android.util.Log;

import org.projectbuendia.client.App;
import org.projectbuendia.client.data.app.AppLocationTree;
import org.projectbuendia.client.data.app.AppModel;
import org.projectbuendia.client.data.app.AppPatient;
import org.projectbuendia.client.data.app.TypedCursor;
import org.projectbuendia.client.events.CrudEventBus;
import org.projectbuendia.client.events.actions.PatientChartRequestedEvent;
import org.projectbuendia.client.events.actions.SyncCancelRequestedEvent;
import org.projectbuendia.client.events.data.AppLocationTreeFetchedEvent;
import org.projectbuendia.client.events.data.TypedCursorFetchedEvent;
import org.projectbuendia.client.events.sync.SyncSucceededEvent;
import org.projectbuendia.client.filter.db.SimpleSelectionFilter;
import org.projectbuendia.client.filter.db.SimpleSelectionFilterGroup;
import org.projectbuendia.client.filter.db.patient.LocationUuidFilter;
import org.projectbuendia.client.filter.db.patient.PatientDbFilters;
import org.projectbuendia.client.filter.matchers.FilteredCursorWrapper;
import org.projectbuendia.client.filter.matchers.MatchingFilter;
import org.projectbuendia.client.filter.matchers.MatchingFilterGroup;
import org.projectbuendia.client.filter.matchers.patient.IdFilter;
import org.projectbuendia.client.filter.matchers.patient.NameFilter;
import org.projectbuendia.client.sync.SyncManager;
import org.projectbuendia.client.utils.EventBusRegistrationInterface;

import java.util.HashSet;
import java.util.Set;

import de.greenrobot.event.EventBus;

import static org.projectbuendia.client.filter.matchers.MatchingFilterGroup.FilterType.OR;

/** Controller for {@link BaseSearchablePatientListActivity}. */
public class PatientSearchController {

    private static final String TAG = PatientSearchController.class.getSimpleName();
    private static final boolean DEBUG = true;

    public interface Ui {
        void setPatients(TypedCursor<AppPatient> patients);
    }

    public interface FragmentUi {
        void setLocationTree(AppLocationTree locationTree);
        void setPatients(TypedCursor<AppPatient> patients);
        void showSpinner(boolean show);
    }

    private final Ui mUi;
    private final CrudEventBus mCrudEventBus;
    private final EventBusRegistrationInterface mGlobalEventBus;
    private final AppModel mModel;
    private final SyncManager mSyncManager;
    private final Set<FragmentUi> mFragmentUis = new HashSet<>();
    private final String mLocale;

    private AppLocationTree mLocationTree;
    private String mRootLocationUuid;

    private SimpleSelectionFilter mFilter;
    private String mFilterQueryTerm = "";
    private FilterSubscriber mFilterSubscriber;
    private final Object mFilterSubscriberLock = new Object();

    private final LocationTreeUpdatedSubscriber mLocationTreeUpdatedSubscriber;

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
     * @param syncManager a {@link SyncManager} for listening for canceling syncs
     * @param locale a language code/locale for presenting localized information (e.g. en)
     */
    public PatientSearchController(
            Ui ui,
            CrudEventBus crudEventBus,
            EventBusRegistrationInterface globalEventBus,
            AppModel model,
            SyncManager syncManager,
            String locale) {
        mUi = ui;
        mCrudEventBus = crudEventBus;
        mGlobalEventBus = globalEventBus;
        mModel = model;
        mSyncManager = syncManager;
        mLocale = locale;

        mFilter = PatientDbFilters.getDefaultFilter();

        mSyncSubscriber = new SyncSubscriber();
        mLocationTreeUpdatedSubscriber = new LocationTreeUpdatedSubscriber();
    }

    /**
     * Requests resources required by this controller. Note that some resources may be fetched
     * asynchronously after this function returns.
     */
    public void init() {
        mGlobalEventBus.register(mSyncSubscriber);
        mCrudEventBus.register(mLocationTreeUpdatedSubscriber);
        mModel.fetchLocationTree(mCrudEventBus, mLocale);
    }

    /** Releases resources required by this controller. */
    public void suspend() {
        mGlobalEventBus.unregister(mSyncSubscriber);
        mCrudEventBus.unregister(mLocationTreeUpdatedSubscriber);
        // Close any outstanding cursors. New results will be fetched when requested.
        if (mPatientsCursor != null) {
            mPatientsCursor.close();
        }
        if (mLocationTree != null) {
            mLocationTree.close();
        }
    }

    private class SyncSubscriber {
        public void onEventMainThread(SyncCancelRequestedEvent event) {
            mSyncManager.cancelOnDemandSync();
        }

        public void onEventMainThread(SyncSucceededEvent event) {
            // Load search results, but don't show the spinner, as the user may be in the middle
            // of performing an operation.
            loadSearchResults(false);
        }
    }

    private class LocationTreeUpdatedSubscriber {
        public synchronized void onEventMainThread(AppLocationTreeFetchedEvent event) {
            synchronized (mFilterSubscriberLock) {
                mFilterSubscriber = null;
            }
            for (FragmentUi fragmentUi : mFragmentUis) {
                fragmentUi.setLocationTree(event.tree);
            }
            if (mLocationTree != null) {
                mLocationTree.close();
            }
            mLocationTree = event.tree;

            // If showing results was blocked on having a location tree, request results
            // immediately.
            if (mWaitingOnLocationTree) {
                mWaitingOnLocationTree = false;
                loadSearchResults();
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

        // Initialize fragment with locations and patients, as necessary.
        if (mLocationTree != null) {
            fragmentUi.setLocationTree(mLocationTree);
        }

        if (mPatientsCursor != null) {
            FilteredCursorWrapper<AppPatient> filteredCursorWrapper =
                    new FilteredCursorWrapper<AppPatient>(
                            mPatientsCursor, mSearchFilter, mFilterQueryTerm);
            fragmentUi.setPatients(filteredCursorWrapper);
        }

        // If all data is loaded, no need for a spinner.
        if (mLocationTree != null && mPatientsCursor != null) {
            fragmentUi.showSpinner(false);
        }
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
     * @param patient the selected {@link org.projectbuendia.client.data.app.AppPatient}
     */
    public void onPatientSelected(AppPatient patient) {
        EventBus.getDefault().post(new PatientChartRequestedEvent(patient.uuid));
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

    /**
     * Manually sets the locations for this controller, which is useful if locations have been
     * updated from an outside context.
     */
    public void setLocations(AppLocationTree appLocationTree) {
        mLocationTree = appLocationTree;
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
                filter = new SimpleSelectionFilterGroup(
                        new LocationUuidFilter(mLocationTree), mFilter);
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
