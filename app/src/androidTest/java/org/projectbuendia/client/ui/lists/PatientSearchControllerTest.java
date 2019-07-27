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

import android.support.test.annotation.UiThreadTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.projectbuendia.client.FakeAppLocationTreeFactory;
import org.projectbuendia.client.FakeTypedCursor;
import org.projectbuendia.client.events.CrudEventBus;
import org.projectbuendia.client.events.data.AppLocationTreeFetchedEvent;
import org.projectbuendia.client.events.data.TypedCursorFetchedEvent;
import org.projectbuendia.client.events.data.TypedCursorFetchedEventFactory;
import org.projectbuendia.client.events.sync.SyncSucceededEvent;
import org.projectbuendia.client.filter.db.SimpleSelectionFilter;
import org.projectbuendia.client.filter.db.patient.PatientDbFilters;
import org.projectbuendia.client.models.AppModel;
import org.projectbuendia.client.models.LocationTree;
import org.projectbuendia.client.models.Patient;
import org.projectbuendia.client.models.TypedCursor;
import org.projectbuendia.client.models.Zones;
import org.projectbuendia.client.sync.SyncManager;
import org.projectbuendia.client.ui.FakeEventBus;
import org.projectbuendia.client.ui.matchers.SimpleSelectionFilterMatchers;

import static junit.framework.TestCase.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/** Tests for {@link PatientSearchController}. */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class PatientSearchControllerTest {
    private static final String LOCALE = "en";
    private PatientSearchController mController;
    private FakeEventBus mFakeCrudEventBus;
    private FakeEventBus mFakeGlobalEventBus;
    @Mock private SyncManager mSyncManager;
    @Mock private AppModel mMockAppModel;
    @Mock private PatientSearchController.Ui mMockUi;
    @Mock private PatientSearchController.FragmentUi mFragmentMockUi;

    /** Tests that results are reloaded when a sync event occurs. */
    @Test
    @UiThreadTest
    public void testSyncSubscriber_reloadsResults() {
        // GIVEN initialized PatientSearchController
        // WHEN a sync event completes
        mFakeGlobalEventBus.post(new SyncSucceededEvent());
        // THEN results should be reloaded
        verify(mMockAppModel).fetchPatients(
            any(CrudEventBus.class), any(SimpleSelectionFilter.class), anyString());
    }

    /** Tests that results are reloaded when a sync event occurs. */
    @Test
    @UiThreadTest
    public void testSyncSubscriber_doesNotShowSpinnerDuringReload() {
        // GIVEN initialized PatientSearchController
        // WHEN a sync event completes
        mFakeGlobalEventBus.post(new SyncSucceededEvent());
        // THEN the spinner is not shown
        verify(mFragmentMockUi, times(0)).showSpinner(true);
    }

    /** Tests that patients are passed to fragment UI's after retrieval. */
    @Test
    @UiThreadTest
    public void testFilterSubscriber_passesPatientsToFragments() {
        // GIVEN initialized PatientSearchController
        mController.loadSearchResults();
        // WHEN patients are retrieved
        TypedCursorFetchedEvent event = TypedCursorFetchedEventFactory.createEvent(
            Patient.class, getFakeAppPatientCursor());
        mFakeCrudEventBus.post(event);
        // THEN patients are passed to fragment UI's
        verify(mFragmentMockUi).setPatients(any(TypedCursor.class));
    }

    private TypedCursor<Patient> getFakeAppPatientCursor() {
        Patient patient = Patient.builder().build();
        return new FakeTypedCursor<>(new Patient[] {patient});
    }

    /** Tests that patients are passed to the activity UI after retrieval. */
    @Test
    @UiThreadTest
    public void testFilterSubscriber_passesPatientsToActivity() {
        // GIVEN initialized PatientSearchController
        mController.loadSearchResults();
        // WHEN patients are retrieved
        TypedCursorFetchedEvent event = TypedCursorFetchedEventFactory.createEvent(
            Patient.class, getFakeAppPatientCursor());
        mFakeCrudEventBus.post(event);
        // THEN patients are passed to activity UI
        verify(mMockUi).setPatients(any(TypedCursor.class));
    }

    /** Tests that any old patient cursor is closed after results are reloaded. */
    @Test
    @UiThreadTest
    public void testFilterSubscriber_closesExistingPatientCursor() {
        // GIVEN initialized PatientSearchController with existing results
        mController.loadSearchResults();
        TypedCursorFetchedEvent event = TypedCursorFetchedEventFactory.createEvent(
            Patient.class, getFakeAppPatientCursor());
        mFakeCrudEventBus.post(event);
        // WHEN new results are retrieved
        mController.loadSearchResults();
        TypedCursorFetchedEvent reloadEvent = TypedCursorFetchedEventFactory.createEvent(
            Patient.class, getFakeAppPatientCursor());
        mFakeCrudEventBus.post(reloadEvent);
        // THEN old patients cursor is closed
        assertTrue(((FakeTypedCursor<Patient>) event.cursor).isClosed());
    }

    /** Tests that retrieving a new cursor results in the closure of any existing cursor. */
    @Test
    @UiThreadTest
    public void testSuspend_closesExistingPatientCursor() {
        // GIVEN initialized PatientSearchController with existing results
        mController.loadSearchResults();
        TypedCursorFetchedEvent event = TypedCursorFetchedEventFactory.createEvent(
            Patient.class, getFakeAppPatientCursor());
        mFakeCrudEventBus.post(event);
        // WHEN controller is suspended
        mController.suspend();
        // THEN patient cursor is closed
        assertTrue(((FakeTypedCursor<Patient>) event.cursor).isClosed());
    }

    /** Tests that suspend() does not attempt to close a null cursor. */
    @Test
    @UiThreadTest
    public void testSuspend_ignoresNullPatientCursor() {
        // GIVEN initialized PatientSearchController with no search results
        // WHEN controller is suspended
        mController.suspend();
        // THEN nothing happens (no runtime exception thrown)
    }

    /** Tests that search results are loaded properly after a cycle of init() and suspend(). */
    @Test
    @UiThreadTest
    public void testLoadSearchResults_functionalAfterInitSuspendCycle() {
        // GIVEN initialized PatientSearchController with existing results
        mController.loadSearchResults();
        TypedCursorFetchedEvent event = TypedCursorFetchedEventFactory.createEvent(
            Patient.class, getFakeAppPatientCursor());
        mFakeCrudEventBus.post(event);
        // WHEN a suspend()/init() cycle occurs
        mController.suspend();
        mController.init();
        // THEN search results can be loaded successfully
        mController.loadSearchResults();
        TypedCursorFetchedEvent reloadEvent = TypedCursorFetchedEventFactory.createEvent(
            Patient.class, getFakeAppPatientCursor());
        mFakeCrudEventBus.post(reloadEvent);
        verify(mFragmentMockUi, times(2)).setPatients(any(TypedCursor.class));
    }

    /**
     * Tests that loadSearchResults() is a no-op for controllers with a specified root location
     * and no location tree--loadSearchResults() is automatically called when the location tree
     * is retrieved.
     */
    @Test
    @UiThreadTest
    public void testLoadSearchResults_waitsOnLocations() {
        // GIVEN PatientSearchController with a location filter and no locations available
        mController.setLocationFilter(Zones.TRIAGE_ZONE_UUID);
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
    @Test
    @UiThreadTest
    public void testLoadSearchResults_fetchesFilteredPatientsOnceLocationsPresent() {
        // GIVEN PatientSearchController with locations available and specified Triage root
        mController.setLocationFilter(Zones.TRIAGE_ZONE_UUID);
        LocationTree tree = FakeAppLocationTreeFactory.build();
        AppLocationTreeFetchedEvent event = new AppLocationTreeFetchedEvent(tree);
        mFakeCrudEventBus.post(event);
        // WHEN search results are requested
        mController.loadSearchResults();
        // THEN patients are fetched from Triage
        verify(mMockAppModel).fetchPatients(
            any(CrudEventBus.class),
            argThat(new SimpleSelectionFilterMatchers.IsFilterGroupWithLocationFilter(
                Zones.TRIAGE_ZONE_UUID)),
            anyString());
    }

    /** Tests that the spinner is shown when loadSearchResults() is called. */
    @Test
    @UiThreadTest
    public void testLoadSearchResults_showsSpinner() {
        // GIVEN initialized PatientSearchController
        // WHEN search results are requested
        mController.loadSearchResults();
        // THEN spinner is shown
        verify(mFragmentMockUi).showSpinner(true);
    }

    /** Tests that the spinner is shown when loadSearchResults() is called. */
    @Test
    @UiThreadTest
    public void testLoadSearchResults_hidesSpinnerWhenRequested() {
        // GIVEN initialized PatientSearchController
        // WHEN search results are requested with no spinner
        mController.loadSearchResults(false);
        // THEN spinner is shown
        verify(mFragmentMockUi, times(0)).showSpinner(true);
    }

    /** Tests that the spinner is hidden after results are retrieved. */
    @Test
    @UiThreadTest
    public void testFilterSubscriber_hidesSpinner() {
        // GIVEN initialized PatientSearchController
        mController.loadSearchResults();
        // WHEN patients are retrieved
        TypedCursorFetchedEvent event =
            TypedCursorFetchedEventFactory.createEvent(
                Patient.class, getFakeAppPatientCursor());
        mFakeCrudEventBus.post(event);
        // THEN patients are passed to fragment UI's
        verify(mFragmentMockUi).showSpinner(false);
    }

    /** Tests that the controller correctly filters when the search term changes. */
    @Test
    @UiThreadTest
    public void testOnQuerySubmitted_filtersBySearchTerm() {
        // GIVEN initialized PatientSearchController with no root location
        // WHEN search term changes
        mController.onQuerySubmitted("foo");
        // THEN results are requested with that search term
        verify(mMockAppModel).fetchPatients(
            mFakeCrudEventBus, PatientDbFilters.getDefaultFilter(), "foo");
    }

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        mFakeCrudEventBus = new FakeEventBus();
        mFakeGlobalEventBus = new FakeEventBus();
        mController = new PatientSearchController(
            mMockUi, mFakeCrudEventBus, mFakeGlobalEventBus, mMockAppModel,
            mSyncManager, LOCALE);
        mController.attachFragmentUi(mFragmentMockUi);
        mController.init();
    }
}
