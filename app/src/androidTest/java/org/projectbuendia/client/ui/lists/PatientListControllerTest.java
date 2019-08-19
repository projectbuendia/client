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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.projectbuendia.client.events.sync.SyncEvent;
import org.projectbuendia.client.events.sync.SyncFailedEvent;
import org.projectbuendia.client.events.sync.SyncSucceededEvent;
import org.projectbuendia.client.sync.SyncManager;
import org.projectbuendia.client.ui.FakeEventBus;

import androidx.test.filters.SmallTest;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/** Tests for {@link PatientListController}. */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class PatientListControllerTest {
    private PatientListController mController;
    private FakeEventBus mFakeEventBus;
    @Mock private SyncManager mMockSyncManager;
    @Mock private PatientListController.Ui mMockUi;

    /** Tests whether refreshing results in a sync. */
    @Test
    @UiThreadTest
    public void testRefresh_RequestsSync() {
        // GIVEN initialized PatientListController
        mController.init();
        // WHEN PatientListController is refreshed
        mController.onRefreshRequested();
        // THEN SyncManager performs sync
        verify(mMockSyncManager).syncAll();
    }

    /** Tests that refreshing multiple times in quick succession results in only one sync. */
    @Test
    @UiThreadTest
    public void testRefresh_PreventsMultipleSimultaneousSyncs() {
        // GIVEN initialized PatientListController
        mController.init();
        // WHEN PatientListController is refreshed multiple times in quick succession
        mController.onRefreshRequested();
        mController.onRefreshRequested();
        // THEN SyncManager performs one, and only one, sync
        verify(mMockSyncManager, times(1)).syncAll();
    }

    /** Tests that refreshing again after a first successful sync results in a new sync. */
    @Test
    @UiThreadTest
    public void testRefresh_AllowsMultipleSequentialSyncsAfterSuccess() {
        // GIVEN initialized PatientListController
        mController.init();
        // WHEN PatientListController is refreshed successfully, then is later refreshed again
        mController.onRefreshRequested();
        mFakeEventBus.post(new SyncSucceededEvent());
        mController.onRefreshRequested();
        // THEN SyncManager performs sync each time
        verify(mMockSyncManager, times(2)).syncAll();
    }

    /** Tests that refreshing again after a first failed sync results in a new sync. */
    @Test
    @UiThreadTest
    public void testRefresh_AllowsMultipleSequentialSyncsAfterFailure() {
        // GIVEN initialized PatientListController
        mController.init();
        // WHEN PatientListController fails to refresh, then is later refreshed again
        mController.onRefreshRequested();
        mFakeEventBus.post(new SyncFailedEvent());
        mController.onRefreshRequested();
        // THEN SyncManager performs sync each time
        verify(mMockSyncManager, times(2)).syncAll();
    }

    /** Tests that the PatientListController listens for events when initialized. */
    @Test
    @UiThreadTest
    public void testInit_EnablesEventBusListener() {
        // GIVEN PatientListController
        // WHEN initialized
        mController.init();
        // THEN nothing happens
        assertEquals(1, mFakeEventBus.countRegisteredReceivers());
    }

    /** Tests that the PatientListController stops listening for events when suspended. */
    @Test
    @UiThreadTest
    public void testSuspend_DisablesEventBusListener() {
        // GIVEN suspended PatientListController
        mController.init();
        mController.suspend();
        // WHEN a SyncSucceededEvent occurs
        SyncEvent event = new SyncSucceededEvent();
        mFakeEventBus.post(event);
        // THEN nothing happens
        assertEquals(0, mFakeEventBus.countRegisteredReceivers());
    }

    /** Tests that the failure of a requested sync results in an error being displayed. */
    @Test
    @UiThreadTest
    public void testForcedSyncFailure_DisplaysSyncError() {
        // GIVEN initialized PatientListController with a forced sync
        mController.init();
        mController.onRefreshRequested();
        // WHEN a forced sync fails
        SyncEvent event = new SyncFailedEvent();
        mFakeEventBus.post(event);
        // THEN an error is shown
        verify(mMockUi).showRefreshError();
    }

    /** Tests that a background sync does not result in a sync error being displayed. */
    @Test
    @UiThreadTest
    public void testBackgroundSyncFailure_DoesNotDisplaySyncError() {
        // GIVEN initialized PatientListController
        mController.init();
        // WHEN a background sync fails
        SyncEvent event = new SyncFailedEvent();
        mFakeEventBus.post(event);
        // THEN no error is shown
        verify(mMockUi, times(0)).showRefreshError();
    }

    /** Tests that a successful sync hides the refresh indicator. */
    @Test
    @UiThreadTest
    public void testSyncSuccess_StopsRefresh() {
        // GIVEN initialized PatientListController
        mController.init();
        // WHEN a sync succeeds
        SyncEvent event = new SyncSucceededEvent();
        mFakeEventBus.post(event);
        // THEN the refresh indicator disappears
        verify(mMockUi).stopRefreshAnimation();
    }

    /** Tests that a failed sync hides the refresh indicator. */
    @Test
    @UiThreadTest
    public void testSyncFailure_StopsRefresh() {
        // GIVEN initialized PatientListController
        mController.init();
        // WHEN a sync fails
        SyncEvent event = new SyncFailedEvent();
        mFakeEventBus.post(event);
        // THEN the refresh indicator disappears
        verify(mMockUi).stopRefreshAnimation();
    }

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        mFakeEventBus = new FakeEventBus();
        mController = new PatientListController(mMockUi, mMockSyncManager, mFakeEventBus);
    }
}
