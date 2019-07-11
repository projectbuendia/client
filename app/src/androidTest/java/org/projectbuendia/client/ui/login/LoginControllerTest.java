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

package org.projectbuendia.client.ui.login;

import android.support.test.annotation.UiThreadTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.test.filters.SmallTest;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.projectbuendia.client.AppSettings;
import org.projectbuendia.client.diagnostics.Troubleshooter;
import org.projectbuendia.client.diagnostics.TroubleshootingAction;
import org.projectbuendia.client.events.diagnostics.TroubleshootingActionsChangedEvent;
import org.projectbuendia.client.events.user.KnownUsersLoadFailedEvent;
import org.projectbuendia.client.events.user.KnownUsersLoadedEvent;
import org.projectbuendia.client.events.user.UserAddFailedEvent;
import org.projectbuendia.client.events.user.UserAddedEvent;
import org.projectbuendia.client.json.JsonNewUser;
import org.projectbuendia.client.json.JsonUser;
import org.projectbuendia.client.ui.FakeEventBus;
import org.projectbuendia.client.user.UserManager;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Tests for {@link LoginController}. */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class LoginControllerTest {

    private LoginController mController;
    @Mock private UserManager mMockUserManager;
    @Mock private LoginController.Ui mMockUi;
    @Mock private LoginController.FragmentUi mMockFragmentUi;
    @Mock private Troubleshooter mTroubleshooter;
    @Mock private AppSettings mAppSettings;
    private FakeEventBus mFakeEventBus;

    /** Tests that init() attempts to load known users. */
    @Test
    @UiThreadTest
    public void testInit_SetsKnownUserLoadGoing() {
        // WHEN the controller is inited
        mController.init();
        // THEN it requests that the user manager loads the list of users
        mMockUserManager.loadKnownUsers();
    }

    /** Tests that suspend() unregisters subscribers from the event bus. */
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

    /** Tests that the UI updates when users are loaded. */
    @Test
    @UiThreadTest
    public void testKnownUsersLoadedEvent_UpdatesUi() throws Exception {
        // GIVEN the controller is inited
        mController.init();
        // WHEN a KnownUsersLoadedEvent is sent over the event bus
        JsonUser user = new JsonUser("idA", "nameA");
        mFakeEventBus.post(new KnownUsersLoadedEvent(ImmutableSet.of(user)));
        // THEN the UI is updated
        verify(mMockFragmentUi).showUsers(ImmutableList.of(user));
    }

    /** Tests that settings are shown when requested. */
    @Test
    @UiThreadTest
    public void testSettingsPress_ShowsSettings() {
        // GIVEN an inited controller
        mController.init();
        // WHEN the settings button is pressed
        mController.onSettingsPressed();
        // THEN the settings screen is opened
        verify(mMockUi).showSettings();
    }

    /** Tests that selecting a user causes a transition to the tent selection screen. */
    @Test
    @UiThreadTest
    public void testSelectingUser_SetsUserAndOpensTentSelection() throws Exception {
        // GIVEN an controller inited controller with users loaded
        mController.init();
        JsonUser user = new JsonUser("idA", "nameA");
        mFakeEventBus.post(new KnownUsersLoadedEvent(ImmutableSet.of(user)));
        // WHEN one of the users is selected
        mController.onUserSelected(user);
        // THEN that user is set as the active user
        verify(mMockUserManager).setActiveUser(user);
        // THEN the tent selection screen is shown
        verify(mMockUi).showTentSelectionScreen();
    }

    /** Tests that spinner is shown when the controller is first initialized. */
    @Test
    @UiThreadTest
    public void testInit_showsSpinner() {
        // WHEN controller is inited
        mController.init();
        // THEN spinner is shown
        verify(mMockFragmentUi).showSpinner(true);
    }

    /** Tests that successful user load hides the spinner. */
    @Test
    @UiThreadTest
    public void testUsersLoaded_hidesSpinner() {
        // GIVEN initialized controller
        mController.init();
        // WHEN users are loaded
        JsonUser user = new JsonUser("idA", "nameA");
        mFakeEventBus.post(new KnownUsersLoadedEvent(ImmutableSet.of(user)));
        // THEN the spinner is hidden
        verify(mMockFragmentUi).showSpinner(false);
    }

    /** Tests that the sync failed dialog appears when loading users fails. */
    @Test
    @UiThreadTest
    public void testUserLoadFails_showsSyncFailedDialog() {
        // GIVEN initialized controller
        mController.init();
        // WHEN users fail to load
        mFakeEventBus.post(new KnownUsersLoadFailedEvent(KnownUsersLoadFailedEvent.REASON_UNKNOWN));
        // THEN the sync fail dialog is shown
        verify(mMockUi).showSyncFailedDialog(true);
    }

    /** Tests that the sync failed dialog is hidden when users are successfully loaded. */
    @Test
    @UiThreadTest
    public void testUserLoaded_hidesSyncFailedDialog() {
        // GIVEN initialized controller
        mController.init();
        // WHEN users are loaded
        JsonUser user = new JsonUser("idA", "nameA");
        mFakeEventBus.post(new KnownUsersLoadedEvent(ImmutableSet.of(user)));
        // THEN the sync fail dialog is hidden
        verify(mMockUi).showSyncFailedDialog(false);
    }

    /** Tests that users are requested when a retry is requested. */
    @Test
    @UiThreadTest
    public void testOnSyncRetry_requestsUsers() {
        // GIVEN initialized controller
        mController.init();
        // WHEN onSyncRetry is called
        mController.onSyncRetry();
        // THEN users are requested
        // Note: already called once in init().
        verify(mMockUserManager, times(2)).loadKnownUsers();
    }

    /** Tests that the spinner is shown when a retry is requested. */
    @Test
    @UiThreadTest
    public void testOnSyncRetry_showsSpinner() {
        // GIVEN initialized controller
        mController.init();
        // WHEN onSyncRetry is called
        mController.onSyncRetry();
        // THEN spinner is shown
        // Note: already shown once in init().
        verify(mMockFragmentUi, times(2)).showSpinner(true);
    }

    /** Tests that the spinner is hidden whenever a user is added. */
    @Test
    @UiThreadTest
    public void testOnUserAdded_showsSpinner() {
        // GIVEN initialized controller
        mController.init();
        // WHEN a user is added
        JsonUser user = new JsonUser("idA", "nameA");
        mFakeEventBus.post(new UserAddedEvent(user));
        // THEN spinner is hidden
        verify(mMockFragmentUi).showSpinner(false);
    }

    /** Tests that the spinner is hidden whenever a user add operation fails. */
    @Test
    @UiThreadTest
    public void testOnUserAddFailed_showsSpinner() {
        // GIVEN initialized controller
        mController.init();
        // WHEN a user fails to be added
        JsonUser user = new JsonUser("idA", "nameA");
        mFakeEventBus.post(new UserAddFailedEvent(new JsonNewUser(), 0));
        // THEN spinner is hidden
        verify(mMockFragmentUi).showSpinner(false);
    }

    /** Tests that users are reloaded if the server becomes healthy and users are unavailable. */
    @Test
    @UiThreadTest
    public void testOnServerHealthy_reloadsUsersIfNotAvailable() {
        // GIVEN initialized controller, no users loaded, server unhealthy
        when(mTroubleshooter.isServerHealthy()).thenReturn(false);
        mController.init();
        // WHEN server becomes healthy
        when(mTroubleshooter.isServerHealthy()).thenReturn(true);
        mFakeEventBus.post(new TroubleshootingActionsChangedEvent(
            ImmutableSet.of(TroubleshootingAction.CHECK_PACKAGE_SERVER_CONFIGURATION), null));
        // THEN users are reloaded
        // Note: already called once in init()
        verify(mMockUserManager, times(2)).loadKnownUsers();
    }

    /** Tests that users are not reloaded if the server becomes healthy and users are available. */
    @Test
    @UiThreadTest
    public void testOnServerHealthy_doesNothingIfUsersAvailable() {
        // GIVEN initialized controller, users loaded, server unhealthy
        when(mTroubleshooter.isServerHealthy()).thenReturn(false);
        mController.init();
        JsonUser user = new JsonUser("idA", "nameA");
        mFakeEventBus.post(new KnownUsersLoadedEvent(ImmutableSet.of(user)));
        // WHEN server becomes healthy
        when(mTroubleshooter.isServerHealthy()).thenReturn(true);
        mFakeEventBus.post(new TroubleshootingActionsChangedEvent(
            ImmutableSet.of(TroubleshootingAction.CHECK_PACKAGE_SERVER_CONFIGURATION), null));
        // THEN users are not reloaded
        verify(mMockUserManager, times(1)).loadKnownUsers();
    }

    /**
     * Tests that TroubleshootingActionsChangedEvents do not trigger user reload if server is still
     * unhealthy.
     */
    @Test
    @UiThreadTest
    public void testOnTroubleshootingActionsChanged_checksServerHealthy() {
        // GIVEN initialized controller, no users loaded, server unhealthy
        when(mTroubleshooter.isServerHealthy()).thenReturn(false);
        mController.init();
        // WHEN TroubleshootingActions change but server is still unhealthy
        mFakeEventBus.post(new TroubleshootingActionsChangedEvent(
            ImmutableSet.of(TroubleshootingAction.CHECK_PACKAGE_SERVER_CONFIGURATION), null));
        // THEN users are not reloaded
        // Note: this function is called once during init(), so expect it to be called once, but
        //       only once.
        verify(mMockUserManager, times(1)).loadKnownUsers();
    }

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        mFakeEventBus = new FakeEventBus();
        mController = new LoginController(
            mMockUserManager,
            mFakeEventBus,
            mTroubleshooter,
            mMockUi,
            mMockFragmentUi,
            mAppSettings);

    }
}