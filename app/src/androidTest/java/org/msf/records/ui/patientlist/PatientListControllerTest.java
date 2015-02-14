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

    /** Tests whether refreshing results in a sync. */
    public void testRefresh_RequestsSync() {
        // GIVEN initialized PatientListController
        mController.init();
        // WHEN PatientListController is refreshed
        mController.onRefreshRequested();
        // THEN SyncManager performs sync
        verify(mMockSyncManager).forceSync();
    }

    /** Tests that refreshing multiple times in quick succession results in only one sync. */
    public void testRefresh_PreventsMultipleSimultaneousSyncs() {
        // GIVEN initialized PatientListController
        mController.init();
        // WHEN PatientListController is refreshed multiple times in quick succession
        mController.onRefreshRequested();
        mController.onRefreshRequested();
        // THEN SyncManager performs one, and only one, sync
        verify(mMockSyncManager, times(1)).forceSync();
    }

    /** Tests that refreshing again after a first successful sync results in a new sync. */
    public void testRefresh_AllowsMultipleSequentialSyncsAfterSuccess() {
        // GIVEN initialized PatientListController
        mController.init();
        // WHEN PatientListController is refreshed successfully, then is later refreshed again
        mController.onRefreshRequested();
        mFakeEventBus.post(new SyncSucceededEvent());
        mController.onRefreshRequested();
        // THEN SyncManager performs sync each time
        verify(mMockSyncManager, times(2)).forceSync();
    }

    /** Tests that refreshing again after a first failed sync results in a new sync. */
    public void testRefresh_AllowsMultipleSequentialSyncsAfterFailure() {
        // GIVEN initialized PatientListController
        mController.init();
        // WHEN PatientListController fails to refresh, then is later refreshed again
        mController.onRefreshRequested();
        mFakeEventBus.post(new SyncFailedEvent());
        mController.onRefreshRequested();
        // THEN SyncManager performs sync each time
        verify(mMockSyncManager, times(2)).forceSync();
    }

    /** Tests that the PatientListController listens for events when initialized. */
    public void testInit_EnablesEventBusListener() {
        // GIVEN PatientListController
        // WHEN initialized
        mController.init();
        // THEN nothing happens
        assertEquals(1, mFakeEventBus.countRegisteredReceivers());
    }

    /** Tests that the PatientListController stops listening for events when suspended. */
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

    /** Tests that the failure of a requested sync results in an error being displayed. */
    public void testForcedSyncFailure_DisplaysSyncError() {
        // GIVEN initialized PatientListController with a forced sync
        mController.init();
        mController.onRefreshRequested();
        // WHEN a forced sync fails
        SyncFinishedEvent event = new SyncFailedEvent();
        mFakeEventBus.post(event);
        // THEN an error is shown
        verify(mMockUi).showRefreshError();
    }

    /** Tests that a background sync does not result in a sync error being displayed. */
    public void testBackgroundSyncFailure_DoesNotDisplaySyncError() {
        // GIVEN initialized PatientListController
        mController.init();
        // WHEN a background sync fails
        SyncFinishedEvent event = new SyncFailedEvent();
        mFakeEventBus.post(event);
        // THEN no error is shown
        verify(mMockUi, times(0)).showRefreshError();
    }

    /** Tests that a successful sync hides the refresh indicator. */
    public void testSyncSuccess_StopsRefresh() {
        // GIVEN initialized PatientListController
        mController.init();
        // WHEN a sync succeeds
        SyncFinishedEvent event = new SyncSucceededEvent();
        mFakeEventBus.post(event);
        // THEN the refresh indicator disappears
        verify(mMockUi).stopRefreshAnimation();
    }

    /** Tests that a failed sync hides the refresh indicator. */
    public void testSyncFailure_StopsRefresh() {
        // GIVEN initialized PatientListController
        mController.init();
        // WHEN a sync fails
        SyncFinishedEvent event = new SyncFailedEvent();
        mFakeEventBus.post(event);
        // THEN the refresh indicator disappears
        verify(mMockUi).stopRefreshAnimation();
    }
}
