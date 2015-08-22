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

import android.test.AndroidTestCase;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.projectbuendia.client.FakeAppLocationTreeFactory;
import org.projectbuendia.client.FakeTypedCursor;
import org.projectbuendia.client.data.app.AppLocationTree;
import org.projectbuendia.client.data.app.AppModel;
import org.projectbuendia.client.data.app.AppPatient;
import org.projectbuendia.client.data.app.TypedCursor;
import org.projectbuendia.client.events.CrudEventBus;
import org.projectbuendia.client.events.data.AppLocationTreeFetchedEvent;
import org.projectbuendia.client.events.data.TypedCursorFetchedEvent;
import org.projectbuendia.client.events.data.TypedCursorFetchedEventFactory;
import org.projectbuendia.client.filter.db.patient.PatientDbFilters;
import org.projectbuendia.client.filter.db.SimpleSelectionFilter;
import org.projectbuendia.client.events.sync.SyncSucceededEvent;
import org.projectbuendia.client.model.Zone;
import org.projectbuendia.client.sync.SyncManager;
import org.projectbuendia.client.ui.FakeEventBus;
import org.projectbuendia.client.ui.matchers.SimpleSelectionFilterMatchers;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/** Tests for {@link PatientSearchController}. */
public class PatientSearchControllerTest extends AndroidTestCase {
    private PatientSearchController mController;
    private FakeEventBus mFakeCrudEventBus;
    private FakeEventBus mFakeGlobalEventBus;
    @Mock private SyncManager mSyncManager;
    @Mock private AppModel mMockAppModel;
    @Mock private PatientSearchController.Ui mMockUi;
    @Mock private PatientSearchController.FragmentUi mFragmentMockUi;

    private static final String LOCALE = "en";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        mFakeCrudEventBus = new FakeEventBus();
        mFakeGlobalEventBus = new FakeEventBus();
        mController = new PatientSearchController(
                mMockUi, mFakeCrudEventBus, mFakeGlobalEventBus, mMockAppModel,
                mSyncManager, LOCALE);
        mController.attachFragmentUi(mFragmentMockUi);
        mController.init();
    }

    /** Tests that results are reloaded when a sync event occurs. */
    public void testSyncSubscriber_reloadsResults() {
        // GIVEN initialized PatientSearchController
        // WHEN a sync event completes
        mFakeGlobalEventBus.post(new SyncSucceededEvent());
        // THEN results should be reloaded
        verify(mMockAppModel).fetchPatients(
                any(CrudEventBus.class), any(SimpleSelectionFilter.class), anyString());
    }

    /** Tests that results are reloaded when a sync event occurs. */
    public void testSyncSubscriber_doesNotShowSpinnerDuringReload() {
        // GIVEN initialized PatientSearchController
        // WHEN a sync event completes
        mFakeGlobalEventBus.post(new SyncSucceededEvent());
        // THEN the spinner is not shown
        verify(mFragmentMockUi, times(0)).showSpinner(true);
    }

    /** Tests that patients are passed to fragment UI's after retrieval. */
    public void testFilterSubscriber_passesPatientsToFragments() {
        // GIVEN initialized PatientSearchController
        mController.loadSearchResults();
        // WHEN patients are retrieved
        TypedCursorFetchedEvent event = TypedCursorFetchedEventFactory.createEvent(
                AppPatient.class, getFakeAppPatientCursor());
        mFakeCrudEventBus.post(event);
        // THEN patients are passed to fragment UI's
        verify(mFragmentMockUi).setPatients(any(TypedCursor.class));
    }

    /** Tests that patients are passed to the activity UI after retrieval. */
    public void testFilterSubscriber_passesPatientsToActivity() {
        // GIVEN initialized PatientSearchController
        mController.loadSearchResults();
        // WHEN patients are retrieved
        TypedCursorFetchedEvent event = TypedCursorFetchedEventFactory.createEvent(
                AppPatient.class, getFakeAppPatientCursor());
        mFakeCrudEventBus.post(event);
        // THEN patients are passed to activity UI
        verify(mMockUi).setPatients(any(TypedCursor.class));
    }

    /** Tests that any old patient cursor is closed after results are reloaded. */
    public void testFilterSubscriber_closesExistingPatientCursor() {
        // GIVEN initialized PatientSearchController with existing results
        mController.loadSearchResults();
        TypedCursorFetchedEvent event = TypedCursorFetchedEventFactory.createEvent(
                AppPatient.class, getFakeAppPatientCursor());
        mFakeCrudEventBus.post(event);
        // WHEN new results are retrieved
        mController.loadSearchResults();
        TypedCursorFetchedEvent reloadEvent = TypedCursorFetchedEventFactory.createEvent(
                AppPatient.class, getFakeAppPatientCursor());
        mFakeCrudEventBus.post(reloadEvent);
        // THEN old patients cursor is closed
        assertTrue(((FakeTypedCursor<AppPatient>) event.cursor).isClosed());
    }

    /** Tests that retrieving a new cursor results in the closure of any existing cursor. */
    public void testSuspend_closesExistingPatientCursor() {
        // GIVEN initialized PatientSearchController with existing results
        mController.loadSearchResults();
        TypedCursorFetchedEvent event = TypedCursorFetchedEventFactory.createEvent(
                AppPatient.class, getFakeAppPatientCursor());
        mFakeCrudEventBus.post(event);
        // WHEN controller is suspended
        mController.suspend();
        // THEN patient cursor is closed
        assertTrue(((FakeTypedCursor<AppPatient>) event.cursor).isClosed());
    }

    /** Tests that suspend() does not attempt to close a null cursor. */
    public void testSuspend_ignoresNullPatientCursor() {
        // GIVEN initialized PatientSearchController with no search results
        // WHEN controller is suspended
        mController.suspend();
        // THEN nothing happens (no runtime exception thrown)
    }

    /** Tests that search results are loaded properly after a cycle of init() and suspend(). */
    public void testLoadSearchResults_functionalAfterInitSuspendCycle() {
        // GIVEN initialized PatientSearchController with existing results
        mController.loadSearchResults();
        TypedCursorFetchedEvent event = TypedCursorFetchedEventFactory.createEvent(
                AppPatient.class, getFakeAppPatientCursor());
        mFakeCrudEventBus.post(event);
        // WHEN a suspend()/init() cycle occurs
        mController.suspend();
        mController.init();
        // THEN search results can be loaded successfully
        mController.loadSearchResults();
        TypedCursorFetchedEvent reloadEvent = TypedCursorFetchedEventFactory.createEvent(
                AppPatient.class, getFakeAppPatientCursor());
        mFakeCrudEventBus.post(reloadEvent);
        verify(mFragmentMockUi, times(2)).setPatients(any(TypedCursor.class));
    }

    /**
     * Tests that loadSearchResults() is a no-op for controllers with a specified root location
     * and no location tree--loadSearchResults() is automatically called when the location tree
     * is retrieved.
     */
    public void testLoadSearchResults_waitsOnLocations() {
        // GIVEN PatientSearchController with a location filter and no locations available
        mController.setLocationFilter(Zone.TRIAGE_ZONE_UUID);
        // WHEN search results are requested
        mController.loadSearchResults();
        // THEN nothing is returned
        verify(mMockAppModel, times(0)).fetchPatients(
                any(CrudEventBus.class), any(SimpleSelectionFilter.class), anyString());
    }

    /**
     * Tests that loadSearchResults() is called, and patients correctly filtered, when the location
     * tree is retrieved.
     */
    public void testLoadSearchResults_fetchesFilteredPatientsOnceLocationsPresent() {
        // GIVEN PatientSearchController with locations available and specified Triage root
        mController.setLocationFilter(Zone.TRIAGE_ZONE_UUID);
        AppLocationTree tree = FakeAppLocationTreeFactory.build();
        AppLocationTreeFetchedEvent event = new AppLocationTreeFetchedEvent(tree);
        mFakeCrudEventBus.post(event);
        // WHEN search results are requested
        mController.loadSearchResults();
        // THEN patients are fetched from Triage
        verify(mMockAppModel).fetchPatients(
                any(CrudEventBus.class),
                argThat(new SimpleSelectionFilterMatchers.IsFilterGroupWithLocationFilter(
                        Zone.TRIAGE_ZONE_UUID)),
                anyString());
    }

    /** Tests that the spinner is shown when loadSearchResults() is called. */
    public void testLoadSearchResults_showsSpinner() {
        // GIVEN initialized PatientSearchController
        // WHEN search results are requested
        mController.loadSearchResults();
        // THEN spinner is shown
        verify(mFragmentMockUi).showSpinner(true);
    }

    /** Tests that the spinner is shown when loadSearchResults() is called. */
    public void testLoadSearchResults_hidesSpinnerWhenRequested() {
        // GIVEN initialized PatientSearchController
        // WHEN search results are requested with no spinner
        mController.loadSearchResults(false);
        // THEN spinner is shown
        verify(mFragmentMockUi, times(0)).showSpinner(true);
    }

    /** Tests that the spinner is hidden after results are retrieved. */
    public void testFilterSubscriber_hidesSpinner() {
        // GIVEN initialized PatientSearchController
        mController.loadSearchResults();
        // WHEN patients are retrieved
        TypedCursorFetchedEvent event =
                TypedCursorFetchedEventFactory.createEvent(
                        AppPatient.class, getFakeAppPatientCursor());
        mFakeCrudEventBus.post(event);
        // THEN patients are passed to fragment UI's
        verify(mFragmentMockUi).showSpinner(false);
    }

    /** Tests that the controller correctly filters when the search term changes. */
    public void testOnQuerySubmitted_filtersBySearchTerm() {
        // GIVEN initialized PatientSearchController with no root location
        // WHEN search term changes
        mController.onQuerySubmitted("foo");
        // THEN results are requested with that search term
        verify(mMockAppModel).fetchPatients(
                mFakeCrudEventBus, PatientDbFilters.getDefaultFilter(), "foo");
    }

    private TypedCursor<AppPatient> getFakeAppPatientCursor() {
        AppPatient patient = AppPatient.builder().build();
        return new FakeTypedCursor<AppPatient>(new AppPatient[] { patient });
    }
}
