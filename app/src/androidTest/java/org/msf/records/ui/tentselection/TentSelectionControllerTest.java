package org.msf.records.ui.tentselection;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.test.AndroidTestCase;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.msf.records.FakeAppLocationTreeFactory;
import org.msf.records.FakeSyncManager;
import org.msf.records.data.app.AppLocationTree;
import org.msf.records.data.app.AppModel;
import org.msf.records.events.actions.SyncCancelRequestedEvent;
import org.msf.records.events.data.AppLocationTreeFetchedEvent;
import org.msf.records.events.sync.SyncCanceledEvent;
import org.msf.records.events.sync.SyncFailedEvent;
import org.msf.records.events.sync.SyncProgressEvent;
import org.msf.records.events.sync.SyncStartedEvent;
import org.msf.records.events.sync.SyncSucceededEvent;
import org.msf.records.ui.FakeEventBus;
import org.msf.records.ui.patientlist.PatientSearchController;

/**
 * Tests for {@link TentSelectionController}.
 */
public final class TentSelectionControllerTest extends AndroidTestCase {

    private TentSelectionController mController;
    private FakeEventBus mFakeEventBus;
    private FakeSyncManager mFakeSyncManager;
    @Mock private AppModel mMockAppModel;
    @Mock private TentSelectionController.Ui mMockUi;
    @Mock private TentSelectionController.TentFragmentUi mMockFragmentUi;
    @Mock private PatientSearchController mMockSearchController;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        mFakeEventBus = new FakeEventBus();
        mFakeSyncManager = new FakeSyncManager();
        mController = new TentSelectionController(
                mMockAppModel,
                mFakeEventBus,
                mMockUi,
                mFakeEventBus,
                mFakeSyncManager,
                mMockSearchController);
	}

    /** Tests that locations are loaded during initialization, when available. */
    public void testInit_RequestsLoadLocationsWhenDataModelAvailable() {
        // GIVEN initialized data model and the controller hasn't previously fetched the location
        // tree
        when(mMockAppModel.isFullModelAvailable()).thenReturn(true);
        // WHEN the controller is initialized
        mController.init();
        // THEN the controller asks the location manager to provide the location tree
        verify(mMockAppModel).fetchLocationTree(mFakeEventBus, "en");
    }

    /** Tests that init does not result in a new sync if data model is available. */
    public void testInit_DoesNotStartSyncWhenDataModelAvailable() {
        // GIVEN initialized data model and the controller hasn't previously fetched the location
        // tree
        when(mMockAppModel.isFullModelAvailable()).thenReturn(true);
        // WHEN the controller is initialized
        mController.init();
        // THEN the controller does not start a new sync
        assertFalse(mFakeSyncManager.isSyncing());
    }

    /** Tests that init kicks off a sync if the data model is unavailable. */
    public void testInit_StartsSyncWhenDataModelUnavailable() {
        // GIVEN uninitialized data model,the controller hasn't previously fetched the location
        // tree, and no sync is already in progress
        mFakeSyncManager.setSyncing(false);
        when(mMockAppModel.isFullModelAvailable()).thenReturn(false);
        // WHEN the controller is initialized
        mController.init();
        // THEN the controller requests a sync
        assertTrue(mFakeSyncManager.isSyncing());
    }

    /** Tests that suspend() unregisters any subscribers from the event bus. */
    public void testSuspend_UnregistersFromEventBus() {
        // GIVEN an initialized controller
        mController.init();
        // WHEN the controller is suspended
        mController.suspend();
        // THEN the controller unregisters from the event bus
        assertEquals(0, mFakeEventBus.countRegisteredReceivers());
    }

    /** Tests that the spinner is hidden after locations are loaded. */
    public void testLoadLocations_HidesSpinner() {
        // GIVEN an initialized controller with a fragment attached
        mController.init();
        mController.attachFragmentUi(mMockFragmentUi);
        // WHEN the location tree is loaded and sync is not in progress
        mFakeSyncManager.setSyncing(false);
        AppLocationTree locationTree = FakeAppLocationTreeFactory.build();
        mFakeEventBus.post(new AppLocationTreeFetchedEvent(locationTree));
        // THEN the controller hides the progress spinner
        verify(mMockFragmentUi).setBusyLoading(false);
    }

    /** Tests that the spinner is not hidden if locations are loaded but a sync is in progress. */
    public void testLoadLocations_DoesNotHideSpinnerWhenSyncInProgress() {
        // GIVEN an initialized controller with a fragment attached
        mController.init();
        mController.attachFragmentUi(mMockFragmentUi);
        // WHEN the location tree is loaded but sync is still in progress
        mFakeSyncManager.setSyncing(true);
        AppLocationTree locationTree = FakeAppLocationTreeFactory.build();
        mFakeEventBus.post(new AppLocationTreeFetchedEvent(locationTree));
        // THEN the controller does not hide the progress spinner
        verify(mMockFragmentUi).setBusyLoading(true);
    }

    /** Tests that the spinner is hidden if locations are loaded and a sync is completed. */
    public void testSpinnerHiddenAfterSyncCompletes() {
        // GIVEN an initialized controller with a fragment attached
        mController.init();
        mController.attachFragmentUi(mMockFragmentUi);
        // WHEN the location tree is loaded AND sync has completed
        mFakeSyncManager.setSyncing(true);
        AppLocationTree locationTree = FakeAppLocationTreeFactory.build();
        mFakeEventBus.post(new AppLocationTreeFetchedEvent(locationTree));
        mFakeEventBus.post(new SyncSucceededEvent());
        // THEN the controller hides the progress spinner
        verify(mMockFragmentUi).setBusyLoading(false);
    }

    /**
     * Tests that a sync failure causes the error dialog to appear when no locations are present.
     */
    public void testSyncFailureShowsErrorDialog_noLocations() {
        // GIVEN an initialized controller with a fragment attached
        mController.init();
        mController.attachFragmentUi(mMockFragmentUi);
        // WHEN the location tree is loaded BUT sync has failed
        mFakeSyncManager.setSyncing(true);
        mFakeEventBus.post(new SyncFailedEvent());
        // THEN the controller shows the sync failure dialog
        verify(mMockUi).showSyncFailedDialog(true);
    }

    /**
     * Tests that if, for some reason, a sync succeeds while the sync dialog is showing, the
     * sync dialog disappears and the tents are usable.
     */
    public void testSyncSuccessHidesSyncDialog() {
        // GIVEN an initialized controller with a fragment attached and a failed sync
        mController.init();
        mController.attachFragmentUi(mMockFragmentUi);
        mFakeEventBus.post(new SyncFailedEvent());
        // WHEN the location tree is loaded and a sync succeeds
        AppLocationTree locationTree = FakeAppLocationTreeFactory.build();
        mFakeEventBus.post(new AppLocationTreeFetchedEvent(locationTree));
        mFakeEventBus.post(new SyncSucceededEvent());
        // THEN the controller hides the sync failed dialog
        verify(mMockUi).showSyncFailedDialog(false);
    }

    /**
     * Tests that loading an empty location tree results in a new sync if sync has completed.
     */
    public void testFetchingIncompleteLocationTree_causesNewSync() {
        // GIVEN an initialized controller with a fragment attached
        mController.init();
        mController.attachFragmentUi(mMockFragmentUi);
        // WHEN an empty location tree is loaded after sync completed
        mFakeEventBus.post(new SyncSucceededEvent());
        AppLocationTree locationTree = FakeAppLocationTreeFactory.emptyTree();
        mFakeEventBus.post(new AppLocationTreeFetchedEvent(locationTree));
        // THEN the controller starts a new sync
        assertTrue(mFakeSyncManager.isSyncing());
    }

    /** Tests that loading an empty location tree does not hide the sync failed dialog. */
    public void testFetchingIncompleteLocationTree_retainsSyncFailedDialog() {
        // GIVEN an initialized controller with a fragment attached
        mController.init();
        mController.attachFragmentUi(mMockFragmentUi);
        // WHEN an empty location tree is loaded
        AppLocationTree locationTree = FakeAppLocationTreeFactory.emptyTree();
        mFakeEventBus.post(new AppLocationTreeFetchedEvent(locationTree));
        // THEN the loading dialog is not hidden
        verify(mMockUi, times(0)).showSyncFailedDialog(false);
    }

    /** Tests that loading an empty location tree does not hide the loading dialog. */
    public void testFetchingIncompleteLocationTree_retainsLoadingDialog() {
        // GIVEN an initialized controller with a fragment attached
        mController.init();
        mController.attachFragmentUi(mMockFragmentUi);
        // WHEN an empty location tree is loaded
        AppLocationTree locationTree = FakeAppLocationTreeFactory.emptyTree();
        mFakeEventBus.post(new AppLocationTreeFetchedEvent(locationTree));
        // THEN the loading dialog is not hidden
        verify(mMockFragmentUi).setBusyLoading(true);
    }

    /** Tests that loading a populated location tree does not result in a new sync. */
    public void testFetchingPopulatedLocationTree_doesNotCauseNewSync() {
        // GIVEN an initialized controller with a fragment attached
        when(mMockAppModel.isFullModelAvailable()).thenReturn(true);
        mController.init();
        mController.attachFragmentUi(mMockFragmentUi);
        // WHEN a populated location tree is loaded
        AppLocationTree locationTree = FakeAppLocationTreeFactory.build();
        mFakeEventBus.post(new AppLocationTreeFetchedEvent(locationTree));
        // THEN the controller does not start a new sync
        assertTrue(!mFakeSyncManager.isSyncing());
    }

    /**
     * Tests that attaching a fragment UI does not show the spinner when locations are present,
     * even if a sync is occurring.
     */
    public void testAttachFragmentUi_doesNotShowSpinnerDuringSyncWhenLocationsPresent() {
        // GIVEN an initialized controller with a location tree, with a sync in progress
        mFakeSyncManager.setSyncing(true);
        mController.init();
        AppLocationTree locationTree = FakeAppLocationTreeFactory.build();
        mFakeEventBus.post(new AppLocationTreeFetchedEvent(locationTree));
        // WHEN a fragment is attached
        mController.attachFragmentUi(mMockFragmentUi);
        // THEN the loading dialog is not displayed
        verify(mMockFragmentUi).setBusyLoading(false);
    }

    /**
     * Tests that attaching a fragment UI shows the spinner if performing during a sync,
     * when location tree is empty.
     */
    public void testAttachFragmentUi_showsSpinnerDuringSyncWhenLocationTreeEmpty() {
        // GIVEN an initialized controller with a location tree, with a sync in progress
        mFakeSyncManager.setSyncing(true);
        mController.init();
        AppLocationTree locationTree = FakeAppLocationTreeFactory.emptyTree();
        mFakeEventBus.post(new AppLocationTreeFetchedEvent(locationTree));
        // WHEN a fragment is attached
        mController.attachFragmentUi(mMockFragmentUi);
        // THEN the loading dialog is displayed
        verify(mMockFragmentUi).setBusyLoading(true);
    }

    /**
     * Tests that attaching a fragment UI shows the spinner if performing during a sync,
     * when location tree is not present.
     */
    public void testAttachFragmentUi_showsSpinnerDuringSyncWhenLocationsNotPresent() {
        // GIVEN an initialized controller with a sync in progress and no location tree
        mFakeSyncManager.setSyncing(true);
        mController.init();
        // WHEN a fragment is attached
        mController.attachFragmentUi(mMockFragmentUi);
        // THEN the loading dialog is displayed
        verify(mMockFragmentUi).setBusyLoading(true);
    }

    /** Tests that user-initiated sync cancellation closes the activity. */
    public void testSyncCancellation_closesActivityWhenUserInitiated() {
        // GIVEN an initialized controller and no location tree
        mController.init();
        // WHEN user initiates and completes a sync cancellation
        mFakeEventBus.post(new SyncCancelRequestedEvent());
        mFakeEventBus.post(new SyncCanceledEvent());
        // THEN the activity is closed
        verify(mMockUi).finish();
    }

    /**
     * Tests that user-initiated sync cancellation closes the activity even when the data model has
     * become available since cancellation was requested.
     */
    public void testSyncCancellation_closesActivityWhenUserInitiatedAndDataModelAvailable() {
        // GIVEN an initialized controller
        mController.init();
        // WHEN user initiates a sync cancellation right before the data model is fetched
        mFakeEventBus.post(new SyncCancelRequestedEvent());
        AppLocationTree locationTree = FakeAppLocationTreeFactory.build();
        mFakeEventBus.post(new AppLocationTreeFetchedEvent(locationTree));
        mFakeEventBus.post(new SyncCanceledEvent());
        // THEN the activity is closed
        verify(mMockUi).finish();
    }

    /** Tests that sync cancellations requested by the Android OS do not close the activity. */
    public void testSyncCancellation_doesNotCloseActivityIfNotUserInitiated() {
        // GIVEN an initialized controller and no location tree
        mController.init();
        // WHEN a sync is canceled, but not by the user
        mFakeEventBus.post(new SyncCanceledEvent());
        // THEN the activity is not closed
        verify(mMockUi, times(0)).finish();
    }

    /** Tests that 'sync progress' messages are ignored when the data model is already available. */
    public void testSyncProgress_ignoredWhenDataModelAvailable() {
        // GIVEN an initialized controller with a location tree
        mController.init();
        AppLocationTree locationTree = FakeAppLocationTreeFactory.build();
        mFakeEventBus.post(new AppLocationTreeFetchedEvent(locationTree));
        // WHEN a periodic sync reports progress
        mFakeEventBus.post(new SyncProgressEvent(10, "Foo synced"));
        // THEN the activity does not notify the UI
        verify(mMockFragmentUi, times(0)).showIncrementalSyncProgress(10, "Foo synced");
    }

    /** Tests that 'sync failed' messages are ignored when the data model is already available. */
    public void testSyncFailed_ignoredWhenDataModelAvailable() {
        // GIVEN an initialized controller with a location tree
        mController.init();
        AppLocationTree locationTree = FakeAppLocationTreeFactory.build();
        mFakeEventBus.post(new AppLocationTreeFetchedEvent(locationTree));
        // WHEN a periodic sync fails
        mFakeEventBus.post(new SyncFailedEvent());
        // THEN the activity does not notify the UI
        verify(mMockUi, times(0)).showSyncFailedDialog(true);
    }

    /** Tests that 'sync started' messages are ignored when the data model is already available. */
    public void testSyncStarted_ignoredWhenDataModelAvailable() {
        // GIVEN an initialized controller with a location tree
        mController.init();
        AppLocationTree locationTree = FakeAppLocationTreeFactory.build();
        mFakeEventBus.post(new AppLocationTreeFetchedEvent(locationTree));
        // WHEN a periodic sync starts
        mFakeEventBus.post(new SyncStartedEvent());
        // THEN the activity does not notify the UI
        verify(mMockFragmentUi, times(0)).resetSyncProgress();
    }
}
