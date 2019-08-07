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

import org.projectbuendia.client.App;
import org.projectbuendia.client.events.CrudEventBus;
import org.projectbuendia.client.events.actions.PatientChartRequestedEvent;
import org.projectbuendia.client.events.data.ItemCreatedEvent;
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
import org.projectbuendia.client.models.AppModel;
import org.projectbuendia.client.models.LocationForest;
import org.projectbuendia.client.models.Patient;
import org.projectbuendia.client.models.TypedCursor;
import org.projectbuendia.client.sync.SyncManager;
import org.projectbuendia.client.utils.EventBusRegistrationInterface;
import org.projectbuendia.client.utils.Utils;

import java.util.HashSet;
import java.util.Set;

import de.greenrobot.event.EventBus;

import static org.projectbuendia.client.filter.matchers.MatchingFilterGroup.FilterType.OR;

/** Controller for {@link BaseSearchablePatientListActivity}. */
public class PatientSearchController {

    private static final String TAG = PatientSearchController.class.getSimpleName();
    private static final boolean DEBUG = true;
    private final Ui mUi;
    private final CrudEventBus mCrudEventBus;
    private final EventBusRegistrationInterface mGlobalEventBus;
    private final AppModel mModel;
    private final SyncManager mSyncManager;
    private final Set<FragmentUi> mFragmentUis = new HashSet<>();
    private final String mLocale;
    private LocationForest mForest;
    private String mRootLocationUuid;
    private SimpleSelectionFilter mFilter;
    private String mFilterQueryTerm = "";
    private FilterSubscriber mFilterSubscriber;
    private final Object mFilterSubscriberLock = new Object();
    private final MatchingFilter<Patient> mSearchFilter =
        new MatchingFilterGroup<>(OR, new IdFilter(), new NameFilter());
    private TypedCursor<Patient> mPatientsCursor;
    private final SyncSubscriber mSyncSubscriber;
    private final CreationSubscriber mCreationSubscriber;

    public interface Ui {
        void setPatients(TypedCursor<Patient> patients);
        void goToPatientChart(String patientUuid);
    }

    public interface FragmentUi {
        void setPatients(TypedCursor<Patient> patients, LocationForest forest);
        void showSpinner(boolean show);
    }

    /**
     * Instantiates a {@link PatientSearchController} with the given UI implementation, event bus,
     * app model, and locale. The global event bus must also be passed in in order to reload results
     * after a sync.
     * @param ui             a {@link Ui} that will respond to UI events
     * @param crudEventBus   a {@link CrudEventBus} that will listen for patient and location fetch
     *                       events
     * @param globalEventBus a {@link EventBusRegistrationInterface} that will listen for sync
     *                       events
     * @param model          an {@link AppModel} for fetching patient and location data
     * @param syncManager    a {@link SyncManager} for listening for canceling syncs
     * @param locale         a language code/locale for presenting localized information (e.g. en)
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
        mCreationSubscriber = new CreationSubscriber();
    }

    /**
     * Requests resources required by this controller. Note that some resources may be fetched
     * asynchronously after this function returns.
     */
    public void init() {
        mGlobalEventBus.register(mSyncSubscriber);
        mCrudEventBus.register(mCreationSubscriber);
        mForest = mModel.getForest(mLocale);
        mModel.setForestRebuiltListener(this::onForestRebuilt);
    }

    /** Releases resources required by this controller. */
    public void suspend() {
        mGlobalEventBus.unregister(mSyncSubscriber);
        mCrudEventBus.unregister(mCreationSubscriber);
        // Close any outstanding cursors. New results will be fetched when requested.
        if (mPatientsCursor != null) {
            mPatientsCursor.close();
        }
        mModel.setForestRebuiltListener(null);
    }

    /** Registers a {@link FragmentUi} with this controller. */
    public void attachFragmentUi(FragmentUi fragmentUi) {
        mFragmentUis.add(fragmentUi);

        if (mPatientsCursor != null) {
            FilteredCursorWrapper<Patient> filteredCursorWrapper =
                new FilteredCursorWrapper<>(
                    mPatientsCursor, mSearchFilter, mFilterQueryTerm);
            fragmentUi.setPatients(filteredCursorWrapper, mForest);
        }

        // If all data is loaded, no need for a spinner.
        if (mForest != null && mPatientsCursor != null) {
            fragmentUi.showSpinner(false);
        }
    }

    /** Unregisters a {@link FragmentUi} with this controller. */
    public void detachFragmentUi(FragmentUi fragmentUi) {
        mFragmentUis.remove(fragmentUi);
    }

    public void onPatientSelected(Patient patient) {
        EventBus.getDefault().post(new PatientChartRequestedEvent(patient.uuid));
    }

    public void onQuerySubmitted(String constraint) {
        App.getServer().cancelPendingRequests();

        mFilterQueryTerm = constraint;

        if (mPatientsCursor == null) {
            loadSearchResults();
            return;
        }
        updatePatients();
    }

    public void loadSearchResults() {
        loadSearchResults(true); // By default, show spinner.
    }

    private void updatePatients() {
        FilteredCursorWrapper<Patient> filteredCursorWrapper =
            new FilteredCursorWrapper<>(
                mPatientsCursor, mSearchFilter, mFilterQueryTerm);
        mUi.setPatients(filteredCursorWrapper);
        for (FragmentUi fragmentUi : mFragmentUis) {
            fragmentUi.setPatients(filteredCursorWrapper, mForest);
            fragmentUi.showSpinner(false);
        }
    }

    /**
     * Asynchronously loads or reloads the search results based on previously specified filter and
     * root location. If no filter is specified, all results are shown be default.
     * @param showSpinner whether or not to show a spinner until operation is complete
     */
    public void loadSearchResults(boolean showSpinner) {
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

    private SimpleSelectionFilter getLocationSubfilter() {
        SimpleSelectionFilter filter;

        // Tack on a location filter to the filter to show only known locations.
        if (mForest == null || mRootLocationUuid == null) {
            filter = mFilter;
        } else {
            filter = new SimpleSelectionFilterGroup(new LocationUuidFilter(
                mForest, mForest.get(mRootLocationUuid)), mFilter);
        }

        return filter;
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
     * @param filter the {@link SimpleSelectionFilter} that will be applied to search results
     */
    public void setFilter(SimpleSelectionFilter filter) {
        mFilter = filter;
    }

    private class CreationSubscriber {
        public void onEventMainThread(ItemCreatedEvent<Patient> event) {
            Utils.logEvent("add_patient_succeeded");
            mUi.goToPatientChart(event.item.uuid);
        }
    }

    private class SyncSubscriber {
        public void onEventMainThread(SyncSucceededEvent event) {
            // Load search results, but don't show the spinner, as the user may be in the middle
            // of performing an operation.
            loadSearchResults(false);
        }
    }

    private void onForestRebuilt(LocationForest forest) {
        mForest = forest;
        updatePatients();
    }

    private final class FilterSubscriber {
        public void onEventMainThread(TypedCursorFetchedEvent<Patient> event) {
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
