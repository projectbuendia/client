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
import org.projectbuendia.client.events.data.AppPatientsLoadedEvent;
import org.projectbuendia.client.events.data.ItemCreatedEvent;
import org.projectbuendia.client.events.sync.SyncSucceededEvent;
import org.projectbuendia.client.filter.db.SimpleSelectionFilter;
import org.projectbuendia.client.filter.db.SimpleSelectionFilterGroup;
import org.projectbuendia.client.filter.db.patient.LocationUuidFilter;
import org.projectbuendia.client.filter.db.patient.PatientDbFilters;
import org.projectbuendia.client.filter.matchers.FilteredCursor;
import org.projectbuendia.client.filter.matchers.MatchingFilter;
import org.projectbuendia.client.filter.matchers.MatchingFilterGroup;
import org.projectbuendia.client.filter.matchers.patient.IdFilter;
import org.projectbuendia.client.filter.matchers.patient.NameFilter;
import org.projectbuendia.models.AppModel;
import org.projectbuendia.models.LocationForest;
import org.projectbuendia.models.Patient;
import org.projectbuendia.models.TypedCursor;
import org.projectbuendia.client.sync.SyncManager;
import org.projectbuendia.client.utils.EventBusRegistrationInterface;
import org.projectbuendia.client.utils.Utils;

import java.util.HashSet;
import java.util.Set;

import de.greenrobot.event.EventBus;

import static org.projectbuendia.client.filter.matchers.MatchingFilterGroup.FilterType.OR;

/** Controller for {@link PatientListActivity}. */
public class PatientSearchController {

    private static final String TAG = PatientSearchController.class.getSimpleName();
    private static final boolean DEBUG = true;
    private final Ui mUi;
    private final CrudEventBus mCrudEventBus;
    private final EventBusRegistrationInterface mGlobalEventBus;
    private final AppModel mModel;
    private final SyncManager mSyncManager;
    private final Set<FragmentUi> mFragmentUis = new HashSet<>();
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
        void setPatients(TypedCursor<Patient> patients, LocationForest forest, String rootLocationUuid);
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
        SyncManager syncManager
    ) {
        mUi = ui;
        mCrudEventBus = crudEventBus;
        mGlobalEventBus = globalEventBus;
        mModel = model;
        mSyncManager = syncManager;
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
        mForest = mModel.getForest();
        mModel.setOnForestReplacedListener(() -> {
            mForest = mModel.getForest();
            updatePatients();
        });
    }

    /** Releases resources required by this controller. */
    public void suspend() {
        mGlobalEventBus.unregister(mSyncSubscriber);
        mCrudEventBus.unregister(mCreationSubscriber);
        // Close any outstanding cursors. New results will be fetched when requested.
        if (mPatientsCursor != null) {
            mPatientsCursor.close();
            mPatientsCursor = null;
        }
        mModel.setOnForestReplacedListener(null);
    }

    /** Updates the list of patients in the UI. */
    private void updatePatients() {
        if (mPatientsCursor != null) {
            mUi.setPatients(getFilteredCursor());
        }
        for (FragmentUi fragmentUi : mFragmentUis) {
            updateFragmentUi(fragmentUi);
        }
    }

    /** Registers a {@link FragmentUi} with this controller. */
    public void attachFragmentUi(FragmentUi fragmentUi) {
        mFragmentUis.add(fragmentUi);
        updateFragmentUi(fragmentUi);
    }

    private void updateFragmentUi(FragmentUi fragmentUi) {
        if (mPatientsCursor != null) {
            fragmentUi.setPatients(getFilteredCursor(), mForest, mRootLocationUuid);
            fragmentUi.showSpinner(false);
        }
    }

    /** Unregisters a {@link FragmentUi} with this controller. */
    public void detachFragmentUi(FragmentUi fragmentUi) {
        mFragmentUis.remove(fragmentUi);
    }

    /** Gets a cursor that returns the filtered list of patients. */
    private TypedCursor<Patient> getFilteredCursor() {
        return new FilteredCursor<>(mPatientsCursor, mSearchFilter, mFilterQueryTerm);
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
        mModel.loadPatients(mCrudEventBus, getLocationSubfilter(), mFilterQueryTerm);
    }

    private SimpleSelectionFilter getLocationSubfilter() {
        SimpleSelectionFilter filter;

        // Tack on a location filter to the filter to show only known locations.
        if (mRootLocationUuid == null) {
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
        public void onEventMainThread(ItemCreatedEvent<?> event) {
            if (event.item instanceof Patient) {
                Utils.logEvent("add_patient_succeeded");
                mUi.goToPatientChart(((Patient) event.item).uuid);
            }
        }
    }

    private class SyncSubscriber {
        public void onEventMainThread(SyncSucceededEvent event) {
            // Load search results, but don't show the spinner, as the user may be in the middle
            // of performing an operation.
            loadSearchResults(false);
        }
    }

    private final class FilterSubscriber {
        public void onEventMainThread(AppPatientsLoadedEvent event) {
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
