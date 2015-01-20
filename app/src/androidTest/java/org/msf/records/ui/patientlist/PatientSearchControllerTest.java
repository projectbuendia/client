package org.msf.records.ui.patientlist;

import android.test.AndroidTestCase;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.msf.records.FakeAppLocationTreeFactory;
import org.msf.records.FakeTypedCursor;
import org.msf.records.data.app.AppLocationTree;
import org.msf.records.data.app.AppModel;
import org.msf.records.data.app.AppPatient;
import org.msf.records.data.app.TypedCursor;
import org.msf.records.events.CrudEventBus;
import org.msf.records.events.data.AppLocationTreeFetchedEvent;
import org.msf.records.events.data.TypedCursorFetchedEvent;
import org.msf.records.events.data.TypedCursorFetchedEventFactory;
import org.msf.records.filter.db.PatientDbFilters;
import org.msf.records.filter.db.SimpleSelectionFilter;
import org.msf.records.model.Zone;
import org.msf.records.ui.FakeEventBus;
import org.msf.records.ui.matchers.SimpleSelectionFilterMatchers;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class PatientSearchControllerTest extends AndroidTestCase {
    private PatientSearchController mController;
    private FakeEventBus mFakeCrudEventBus;
    @Mock private AppModel mMockAppModel;
    @Mock private PatientSearchController.Ui mMockUi;
    @Mock private PatientSearchController.FragmentUi mFragmentMockUi;

    private static final String LOCALE = "en";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        mFakeCrudEventBus = new FakeEventBus();
        mController = new PatientSearchController(
                mMockUi, mFakeCrudEventBus, mMockAppModel, LOCALE);
        mController.attachFragmentUi(mFragmentMockUi);
    }

    /** Tests that patients are passed to fragment UI's after retrieval. */
    public void testFilterSubscriber_passesPatientsToFragments() {
        // GIVEN initialized PatientSearchController
        mController.loadSearchResults();
        // WHEN patients are retrieved
        TypedCursorFetchedEvent event =
                TypedCursorFetchedEventFactory.createEvent(
                        AppPatient.class, getFakeAppPatientCursor());
        mFakeCrudEventBus.post(event);
        // THEN patients are passed to fragment UI's
        verify(mFragmentMockUi).setPatients(event.cursor);
    }

    /** Tests that patients are passed to the activity UI after retrieval. */
    public void testFilterSubscriber_passesPatientsToActivity() {
        // GIVEN initialized PatientSearchController
        mController.loadSearchResults();
        // WHEN patients are retrieved
        TypedCursorFetchedEvent event =
                TypedCursorFetchedEventFactory.createEvent(
                        AppPatient.class, getFakeAppPatientCursor());
        mFakeCrudEventBus.post(event);
        // THEN patients are passed to activity UI
        verify(mMockUi).setPatients(event.cursor);
    }

    /** Tests that the patient cursor is closed after patients are passed to UI's. */
    public void testFilterSubscriber_closesPatientCursor() {
        // GIVEN initialized PatientSearchController
        mController.loadSearchResults();
        // WHEN patients are retrieved
        TypedCursorFetchedEvent event =
                TypedCursorFetchedEventFactory.createEvent(
                        AppPatient.class, getFakeAppPatientCursor());
        mFakeCrudEventBus.post(event);
        // THEN patients cursor is closed
        assertTrue(((FakeTypedCursor<AppPatient>) event.cursor).isClosed());
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

    /** Tests that the chart activity is launched for a patient when that patient is selected. */
    public void testOnPatientSelected_launchesChartActivity() {
        // GIVEN initialized PatientSearchController
        // WHEN a patient is selected
        AppPatient patient = AppPatient.builder()
                .setUuid("1234")
                .setGivenName("Bob")
                .setFamilyName("Smith")
                .setId("KH.1234")
                .build();
        mController.onPatientSelected(patient);
        // THEN launch chart activity for that patient
        verify(mMockUi).launchChartActivity("1234", "Bob", "Smith", "KH.1234");
    }

    private TypedCursor<AppPatient> getFakeAppPatientCursor() {
        AppPatient patient = AppPatient.builder().build();
        return new FakeTypedCursor<AppPatient>(new AppPatient[] { patient });
    }
}
