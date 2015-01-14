package org.msf.records.ui.patientlist;

import android.test.AndroidTestCase;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.msf.records.sync.SyncManager;
import org.msf.records.ui.FakeEventBus;

public class PatientListControllerTest extends AndroidTestCase {
    private PatientListController mController;
    private FakeEventBus mFakeEventBus;
    @Mock private SyncManager mMockSyncManager;
    @Mock private PatientListController.Ui mMockUi;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        // TODO: Create a fake event bus so we can check whether the controller
        // unregistered its event handler.
        mFakeEventBus = new FakeEventBus();
        mController = new PatientListController(mMockUi, mMockSyncManager);
    }

    // TODO(akalachman): Implement the following test cases:

    // GIVEN initialized PatientListController
    // WHEN PatientListController is refreshed
    // THEN SyncManager performs sync

    // GIVEN initialized PatientListController
    // WHEN PatientListController is refreshed multiple times in quick succession
    // THEN SyncManager performs one, and only one, sync

    // GIVEN initialized PatientListController
    // WHEN PatientListController is refreshed, then later refreshed again
    // THEN SyncManager performs sync each time

    // GIVEN suspended PatientListController
    // WHEN PatientListController is refreshed
    // THEN nothing happens
}
