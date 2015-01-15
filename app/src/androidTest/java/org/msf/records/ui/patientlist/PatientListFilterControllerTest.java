package org.msf.records.ui.patientlist;

import android.test.AndroidTestCase;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.msf.records.FakeAppLocationTreeFactory;
import org.msf.records.data.app.AppLocationTree;
import org.msf.records.data.app.AppModel;
import org.msf.records.events.data.AppLocationTreeFetchedEvent;
import org.msf.records.ui.FakeEventBus;
import org.msf.records.ui.matchers.SimpleSelectionFilterMatchers;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;

public class PatientListFilterControllerTest  extends AndroidTestCase {
    private PatientListFilterController mController;
    private FakeEventBus mFakeCrudEventBus;
    @Mock private AppModel mMockAppModel;
    @Mock private PatientListFilterController.Ui mMockUi;

    private static final String LOCALE = "en";

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        mFakeCrudEventBus = new FakeEventBus();
        mController = new PatientListFilterController(
                mMockUi, mFakeCrudEventBus, mMockAppModel, LOCALE);
    }

    /** Tests that requesting an action bar initialization fetches a location tree. */
    public void testSetupActionBarAsync_fetchesLocationTree() {
        // GIVEN initialized PatientListFilterController
        // WHEN setupActionBarAsync called
        mController.setupActionBarAsync();
        // THEN location tree is fetched from model
        verify(mMockAppModel).fetchLocationTree(mFakeCrudEventBus, LOCALE);
    }

    /** Tests that filters are correctly initialized once a location tree is retrieved. */
    public void testSetupActionBarAsync_passesLocationFilters() {
        // GIVEN initialized PatientListFilterController, after setupActionBarAsync called
        mController.setupActionBarAsync();
        // WHEN location tree fetched
        AppLocationTree tree = FakeAppLocationTreeFactory.build();
        AppLocationTreeFetchedEvent event = new AppLocationTreeFetchedEvent(tree);
        mFakeCrudEventBus.post(event);
        // THEN location filters passed to the Ui
        verify(mMockUi).populateActionBar(
                argThat(new SimpleSelectionFilterMatchers.ContainsFilterWithName("Triage")));
    }
}
