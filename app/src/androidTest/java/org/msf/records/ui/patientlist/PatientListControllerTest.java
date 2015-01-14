package org.msf.records.ui.patientlist;

import android.test.AndroidTestCase;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.msf.records.events.sync.SyncFailedEvent;
import org.msf.records.events.sync.SyncFinishedEvent;
import org.msf.records.events.sync.SyncSucceededEvent;
import org.msf.records.sync.SyncManager;
import org.msf.records.ui.FakeEventBus;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class PatientListControllerTest extends AndroidTestCase {
    private PatientListController mController;
    private FakeEventBus mFakeEventBus;
    @Mock private SyncManager mMockSyncManager;
    @Mock private PatientListController.Ui mMockUi;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        mFakeEventBus = new FakeEventBus();
        mController = new PatientListController(mMockUi, mMockSyncManager, mFakeEventBus);
    }

    public void testRefresh_RequestsSync() {
        // GIVEN initialized PatientListController
        mController.init();
        // WHEN PatientListController is refreshed
        mController.getOnRefreshListener().onRefresh();
        // THEN SyncManager performs sync
        verify(mMockSyncManager).forceSync();
    }

    public void testRefresh_PreventsMultipleSimultaneousSyncs() {
        // GIVEN initialized PatientListController
        mController.init();
        // WHEN PatientListController is refreshed multiple times in quick succession
        mController.getOnRefreshListener().onRefresh();
        mController.getOnRefreshListener().onRefresh();
        // THEN SyncManager performs one, and only one, sync
        verify(mMockSyncManager, times(1)).forceSync();
    }

    public void testRefresh_AllowsMultipleSequentialSyncsAfterSuccess() {
        // GIVEN initialized PatientListController
        mController.init();
        // WHEN PatientListController is refreshed successfully, then is later refreshed again
        mController.getOnRefreshListener().onRefresh();
        mFakeEventBus.post(new SyncSucceededEvent());
        mController.getOnRefreshListener().onRefresh();
        // THEN SyncManager performs sync each time
        verify(mMockSyncManager, times(2)).forceSync();
    }

    public void testRefresh_AllowsMultipleSequentialSyncsAfterFailure() {
        // GIVEN initialized PatientListController
        mController.init();
        // WHEN PatientListController fails to refresh, then is later refreshed again
        mController.getOnRefreshListener().onRefresh();
        mFakeEventBus.post(new SyncFailedEvent());
        mController.getOnRefreshListener().onRefresh();
        // THEN SyncManager performs sync each time
        verify(mMockSyncManager, times(2)).forceSync();
    }

    public void testInit_EnablesEventBusListener() {
        // GIVEN PatientListController
        // WHEN initialized
        mController.init();
        // THEN nothing happens
        assertEquals(1, mFakeEventBus.countRegisteredReceivers());
    }

    public void testSuspend_DisablesEventBusListener() {
        // GIVEN suspended PatientListController
        mController.init();
        mController.suspend();
        // WHEN a SyncSucceededEvent occurs
        SyncFinishedEvent event = new SyncSucceededEvent();
        mFakeEventBus.post(event);
        // THEN nothing happens
        assertEquals(0, mFakeEventBus.countRegisteredReceivers());
    }
}
