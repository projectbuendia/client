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

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.projectbuendia.client.AppSettings;
import org.projectbuendia.client.FakeForestFactory;
import org.projectbuendia.client.FakeSyncManager;
import org.projectbuendia.client.events.actions.SyncCancelRequestedEvent;
import org.projectbuendia.client.events.sync.SyncCancelledEvent;
import org.projectbuendia.client.events.sync.SyncFailedEvent;
import org.projectbuendia.client.events.sync.SyncSucceededEvent;
import org.projectbuendia.client.models.AppModel;
import org.projectbuendia.client.ui.FakeEventBus;
import org.projectbuendia.client.ui.ReadyState;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Tests for {@link LocationListController}. */
public final class LocationListControllerTest {

    private LocationListController mController;
    private FakeEventBus mFakeEventBus;
    private FakeSyncManager mFakeSyncManager;
    @Mock private AppModel mMockAppModel;
    @Mock private AppSettings mMockSettings;
    @Mock private LocationListController.Ui mMockUi;
    @Mock private LocationListController.LocationListFragmentUi mMockFragmentUi;
    @Mock private PatientSearchController mMockSearchController;

    /** Tests that locations are loaded during initialization, when available. */
    @Test
    @UiThreadTest
    public void testInit_RequestsLoadLocationsWhenDataModelAvailable() {
        // GIVEN initialized data model and the controller hasn't previously fetched the location forest
        when(mMockAppModel.isFullModelAvailable()).thenReturn(true);
        // WHEN the controller is initialized
        mController.init();
        // THEN the controller asks the location manager to provide the location forest
        verify(mMockAppModel).getForest();
    }

    /** Tests that init does not result in a new sync if data model is available. */
    @Test
    @UiThreadTest
    public void testInit_DoesNotStartSyncWhenDataModelAvailable() {
        // GIVEN initialized data model and an available forest
        when(mMockAppModel.isFullModelAvailable()).thenReturn(true);
        when(mMockAppModel.getForest()).thenReturn(FakeForestFactory.build());
        // WHEN the controller is initialized
        mController.init();
        // THEN the controller does not start a new sync
        assertFalse(mFakeSyncManager.isSyncRunningOrPending());
    }

    /** Tests that init kicks off a sync if the data model is unavailable. */
    @Test
    @UiThreadTest
    public void testInit_StartsSyncWhenDataModelUnavailable() {
        // GIVEN uninitialized data model,the controller hasn't previously fetched the location
        // forest, and no sync is already in progress
        mFakeSyncManager.setSyncing(false);
        when(mMockAppModel.isFullModelAvailable()).thenReturn(false);
        // WHEN the controller is initialized
        mController.init();
        // THEN the controller requests a sync
        assertTrue(mFakeSyncManager.isSyncRunningOrPending());
    }

    /** Tests that suspend() unregisters any subscribers from the event bus. */
    @Test
    @UiThreadTest
    public void testSuspend_UnregistersFromEventBus() {
        // GIVEN an initialized controller
        mController.init();
        // WHEN the controller is suspended
        mController.suspend();
        // THEN the controller unregisters from the event bus
        assertEquals(0, mFakeEventBus.countRegisteredReceivers());
    }

    /** Tests that the spinner is hidden after locations are loaded. */
    @Test
    @UiThreadTest
    public void testLoadLocations_HidesSpinner() {
        // GIVEN a valid location forest and sync not in progress
        mFakeSyncManager.setSyncing(false);
        when(mMockAppModel.isFullModelAvailable()).thenReturn(true);
        when(mMockAppModel.getForest()).thenReturn(FakeForestFactory.build());
        // GIVEN an initialized controller with a fragment attached
        mController.init();
        mController.attachFragmentUi(mMockFragmentUi);
        // THEN the controller hides the progress spinner
        verify(mMockFragmentUi).setReadyState(ReadyState.READY);
    }

    /** Tests that the spinner is not shown while a sync is in progress if the model and locations are available. */
    @Test
    @UiThreadTest
    public void testLoadLocations_DoesNotShowSpinnerWhenSyncInProgress() {
        // GIVEN a valid location forest while sync is still in progress
        mFakeSyncManager.setSyncing(true);
        when(mMockAppModel.isFullModelAvailable()).thenReturn(true);
        when(mMockAppModel.getForest()).thenReturn(FakeForestFactory.build());
        // WHEN a controller starts with a fragment attached
        mController.init();
        mController.attachFragmentUi(mMockFragmentUi);
        // THEN the controller does not show the progress spinner
        verify(mMockFragmentUi).setReadyState(ReadyState.READY);
    }

    /** Tests that the spinner is hidden if locations are loaded and a sync is completed. */
    @Test
    @UiThreadTest
    public void testSpinnerHiddenAfterSyncCompletes() {
        // GIVEN an incomplete model while sync is still in progress
        mFakeSyncManager.setSyncing(true);
        when(mMockAppModel.isFullModelAvailable()).thenReturn(false);
        // WHEN a controller starts with a fragment attached
        mController.init();
        mController.attachFragmentUi(mMockFragmentUi);
        // THEN the controller shows the sync progress bar
        verify(mMockFragmentUi).setReadyState(ReadyState.SYNCING);
        // WHEN the sync succeeds
        when(mMockAppModel.getForest()).thenReturn(FakeForestFactory.build());
        mFakeEventBus.post(new SyncSucceededEvent());
        // THEN the controller shows the spinner, loads the tree, and hides the spinner
        verify(mMockFragmentUi, atLeast(0)).setReadyState(ReadyState.LOADING);
        verify(mMockFragmentUi).setReadyState(ReadyState.READY);
    }

    /** Tests that a sync failure causes the error dialog to appear when no locations are present. */
    @Test
    @UiThreadTest
    public void testSyncFailureShowsErrorDialog_noLocations() {
        // GIVEN an initialized controller with a fragment attached
        mController.init();
        mController.attachFragmentUi(mMockFragmentUi);
        // WHEN the location forest is loaded BUT sync has failed
        mFakeSyncManager.setSyncing(true);
        mFakeEventBus.post(new SyncFailedEvent());
        // THEN the controller shows the sync failure dialog
        verify(mMockUi).showSyncFailedDialog(true);
    }

    /**
     * Tests that if, for some reason, a sync succeeds while the sync dialog is showing, the
     * sync dialog disappears and the locations are usable.
     */
    @Test
    @UiThreadTest
    public void testSyncSuccessHidesSyncDialog() {
        // GIVEN an initialized controller with an incomplete model
        when(mMockAppModel.isFullModelAvailable()).thenReturn(false);
        when(mMockAppModel.getForest()).thenReturn(FakeForestFactory.build());
        mController.init();
        mController.attachFragmentUi(mMockFragmentUi);
        // WHEN a sync fails, the sync failed dialog is shown
        mFakeEventBus.post(new SyncFailedEvent());
        verify(mMockUi).showSyncFailedDialog(true);
        verify(mMockFragmentUi).setReadyState(ReadyState.ERROR);
        // WHEN a sync succeeds
        mFakeEventBus.post(new SyncSucceededEvent());
        // THEN the controller hides the sync failed dialog, loads the forest, and is ready for use
        verify(mMockUi).showSyncFailedDialog(false);
        verify(mMockFragmentUi, atLeast(0)).setReadyState(ReadyState.LOADING);
        verify(mMockFragmentUi).setReadyState(ReadyState.READY);
    }

    /** Tests that loading an empty location forest results in a new sync if sync has completed. */
    @Test
    @UiThreadTest
    public void testFetchingIncompleteForest_causesNewSync() {
        // GIVEN an initialized controller with a fragment attached
        mController.init();
        mController.attachFragmentUi(mMockFragmentUi);
        // WHEN an empty location forest is loaded after sync completed
        mFakeEventBus.post(new SyncSucceededEvent());
        when(mMockAppModel.getForest()).thenReturn(FakeForestFactory.build());
        // THEN the controller starts a new sync
        assertTrue(mFakeSyncManager.isSyncRunningOrPending());
    }

    /** Tests that loading an empty location forest does not hide the sync failed dialog. */
    @Test
    @UiThreadTest
    public void testFetchingIncompleteForest_retainsSyncFailedDialog() {
        // GIVEN an empty forest
        when(mMockAppModel.getForest()).thenReturn(FakeForestFactory.emptyForest());
        // WHEN the controller starts up and a fragment is attached
        mController.init();
        mController.attachFragmentUi(mMockFragmentUi);
        // THEN the sync failed dialog is not hidden
        verify(mMockUi, times(0)).showSyncFailedDialog(false);
    }

    /** Tests that loading an empty location forest does not hide the loading dialog. */
    @Test
    @UiThreadTest
    public void testFetchingIncompleteForest_retainsLoadingDialog() {
        // GIVEN an empty forest
        when(mMockAppModel.isFullModelAvailable()).thenReturn(true);
        when(mMockAppModel.getForest()).thenReturn(FakeForestFactory.emptyForest());
        // WHEN the controller starts up and a fragment is attached
        mController.init();
        mController.attachFragmentUi(mMockFragmentUi);
        // THEN the sync progress bar is shown
        verify(mMockFragmentUi).setReadyState(ReadyState.SYNCING);
    }

    /** Tests that loading a populated location forest does not result in a new sync. */
    @Test
    @UiThreadTest
    public void testFetchingPopulatedForest_doesNotCauseNewSync() {
        // GIVEN a valid model and a populated forest
        when(mMockAppModel.isFullModelAvailable()).thenReturn(true);
        when(mMockAppModel.getForest()).thenReturn(FakeForestFactory.build());
        // WHEN the controller starts up and a fragment is attached
        mController.init();
        mController.attachFragmentUi(mMockFragmentUi);
        // THEN the controller does not start a new sync
        assertTrue(!mFakeSyncManager.isSyncRunningOrPending());
    }

    /**
     * Tests that attaching a fragment UI does not show the spinner when locations are present,
     * even if a sync is occurring.
     */
    @Test
    @UiThreadTest
    public void testAttachFragmentUi_doesNotShowSpinnerDuringSyncWhenLocationsPresent() {
        // GIVEN a sync in progress and a valid location forest
        mFakeSyncManager.setSyncing(true);
        when(mMockAppModel.isFullModelAvailable()).thenReturn(true);
        when(mMockAppModel.getForest()).thenReturn(FakeForestFactory.build());
        // WHEN a fragment is attached to a new controller
        mController.init();
        mController.attachFragmentUi(mMockFragmentUi);
        // THEN the loading spinner is hidden
        verify(mMockFragmentUi).setReadyState(ReadyState.READY);
    }

    /**
     * Tests that attaching a fragment UI shows the spinner if performing during a sync,
     * when location forest is empty.
     */
    @Test
    @UiThreadTest
    public void testAttachFragmentUi_showsSpinnerDuringSyncWhenForestEmpty() {
        // GIVEN a sync in progress and an empty location forest
        mFakeSyncManager.setSyncing(true);
        when(mMockAppModel.isFullModelAvailable()).thenReturn(true);
        when(mMockAppModel.getForest()).thenReturn(FakeForestFactory.emptyForest());
        // WHEN a fragment is attached to a new controller
        mController.init();
        mController.attachFragmentUi(mMockFragmentUi);
        // THEN the sync progress bar is shown
        verify(mMockFragmentUi).setReadyState(ReadyState.SYNCING);
    }

    /**
     * Tests that attaching a fragment UI shows the spinner if performing during a sync,
     * when location forest is not present.
     */
    @Test
    @UiThreadTest
    public void testAttachFragmentUi_showsSpinnerDuringSyncWhenLocationsNotPresent() {
        // GIVEN an initialized controller with a sync in progress and no location forest
        mFakeSyncManager.setSyncing(true);
        when(mMockAppModel.isFullModelAvailable()).thenReturn(false);
        mController.init();
        // WHEN a fragment is attached
        mController.attachFragmentUi(mMockFragmentUi);
        // THEN the sync progress bar is shown
        verify(mMockFragmentUi).setReadyState(ReadyState.SYNCING);
    }

    /** Tests that user-initiated sync cancellation closes the activity. */
    @Test
    @UiThreadTest
    public void testSyncCancellation_closesActivityWhenUserInitiated() {
        // GIVEN an initialized controller and no location forest
        mController.init();
        // WHEN user initiates and completes a sync cancellation
        mFakeEventBus.post(new SyncCancelRequestedEvent());
        mFakeEventBus.post(new SyncCancelledEvent());
        // THEN the activity is closed
        verify(mMockUi).finish();
    }

    /**
     * Tests that user-initiated sync cancellation closes the activity even when the data model has
     * become available since cancellation was requested.
     */
    @Test
    @UiThreadTest
    public void testSyncCancellation_closesActivityWhenUserInitiatedAndDataModelAvailable() {
        // GIVEN an initialized controller
        mController.init();
        // WHEN user initiates a sync cancellation right before the data model is fetched
        mFakeEventBus.post(new SyncCancelRequestedEvent());
        when(mMockAppModel.getForest()).thenReturn(FakeForestFactory.build());
        mFakeEventBus.post(new SyncCancelledEvent());
        // THEN the activity is closed
        verify(mMockUi).finish();
    }

    /** Tests that sync cancellations requested by the Android OS do not close the activity. */
    @Test
    @UiThreadTest
    public void testSyncCancellation_doesNotCloseActivityIfNotUserInitiated() {
        // GIVEN an initialized controller and no location forest
        mController.init();
        // WHEN a sync is canceled, but not by the user
        mFakeEventBus.post(new SyncCancelledEvent());
        // THEN the activity is not closed
        verify(mMockUi, times(0)).finish();
    }

    /** Tests that 'sync failed' messages are ignored when the data model is already available. */
    @Test
    @UiThreadTest
    public void testSyncFailed_ignoredWhenDataModelAvailable() {
        // GIVEN an initialized controller with a location forest
        when(mMockAppModel.isFullModelAvailable()).thenReturn(true);
        when(mMockAppModel.getForest()).thenReturn(FakeForestFactory.build());
        mController.init();
        // WHEN a periodic sync fails
        mFakeEventBus.post(new SyncFailedEvent());
        // THEN the activity does not show the Sync Failed dialog
        verify(mMockUi, times(0)).showSyncFailedDialog(true);
    }

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        mFakeEventBus = new FakeEventBus();
        mFakeSyncManager = new FakeSyncManager();
        mController = new LocationListController(
            mMockAppModel,
            mMockSettings,
            mFakeEventBus,
            mMockUi,
            mFakeEventBus,
            mFakeSyncManager,
            mMockSearchController);
    }
}
