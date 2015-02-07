package org.msf.records.ui.userlogin;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.verify;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.msf.records.events.user.KnownUsersLoadFailedEvent;
import org.msf.records.events.user.KnownUsersLoadedEvent;
import org.msf.records.net.model.User;
import org.msf.records.ui.FakeEventBus;
import org.msf.records.ui.userlogin.UserLoginController;
import org.msf.records.user.UserManager;

import android.test.AndroidTestCase;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 * Tests for {@link UserLoginController}.
 */
public class UserLoginControllerTest extends AndroidTestCase {

    private UserLoginController mController;
    @Mock private UserManager mMockUserManager;
    @Mock private UserLoginController.Ui mMockUi;
    @Mock private UserLoginController.FragmentUi mMockFragmentUi;
    private FakeEventBus mFakeEventBus;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        // TODO: Create a fake event bus so we can check whether the controller
        // unregistered its event handler.
        mFakeEventBus = new FakeEventBus();
        mController = new UserLoginController(
                mMockUserManager,
                mFakeEventBus,
                mMockUi,
                mMockFragmentUi);
    }

    /** Tests that init() attempts to load known users. */
    public void testInit_SetsKnownUserLoadGoing() {
        // WHEN the controller is inited
        mController.init();
        // THEN it requests that the user manager loads the list of users
        mMockUserManager.loadKnownUsers();
    }

    /** Tests that suspend() unregisters subscribers from the event bus. */
    public void testSuspend_UnregistersFromEventBus() {
        // GIVEN an initialized controller
        mController.init();
        // WHEN the controller is suspended
        mController.suspend();
        // THEN the controller unregisters from the event bus
        assertEquals(0, mFakeEventBus.countRegisteredReceivers());
    }

    /** Tests that the UI updates when users are loaded. */
    public void testKnownUsersLoadedEvent_UpdatesUi() throws Exception {
        // GIVEN the controller is inited
        mController.init();
        // WHEN a KnownUsersLoadedEvent is sent over the event bus
        User user = new User("idA", "nameA");
        mFakeEventBus.post(new KnownUsersLoadedEvent(ImmutableSet.of(user)));
        // THEN the UI is updated
        verify(mMockFragmentUi).showUsers(ImmutableList.of(user));
    }

    /** Tests that settings are shown when requested. */
    public void testSettingsPress_ShowsSettings() {
        // GIVEN an inited controller
        mController.init();
        // WHEN the settings button is pressed
        mController.onSettingsPressed();
        // THEN the settings screen is opened
        verify(mMockUi).showSettings();
    }

    /** Tests that selecting a user causes a transition to the tent selection screen. */
    public void testSelectingUser_SetsUserAndOpensTentSelection() throws Exception {
        // GIVEN an controller inited controller with users loaded
        mController.init();
        User user = new User("idA", "nameA");
        mFakeEventBus.post(new KnownUsersLoadedEvent(ImmutableSet.of(user)));
        // WHEN one of the users is selected
        mController.onUserSelected(user);
        // THEN that user is set as the active user
        verify(mMockUserManager).setActiveUser(user);
        // THEN the tent selection screen is shown
        verify(mMockUi).showTentSelectionScreen();
    }

    /** Tests that spinner is shown when the controller is first initialized. */
    public void testInit_showsSpinner() {
        // WHEN controller is inited
        mController.init();
        // THEN spinner is shown
        verify(mMockFragmentUi).showSpinner(true);
    }

    /** Tests that successful user load hides the spinner. */
    public void testUsersLoaded_hidesSpinner() {
        // GIVEN initialized controller
        mController.init();
        // WHEN users are loaded
        User user = new User("idA", "nameA");
        mFakeEventBus.post(new KnownUsersLoadedEvent(ImmutableSet.of(user)));
        // THEN the spinner is hidden
        verify(mMockFragmentUi).showSpinner(false);
    }

    /** Tests that the sync failed dialog appears when loading users fails. */
    public void testUserLoadFails_showsSyncFailedDialog() {
        // GIVEN initialized controller
        mController.init();
        // WHEN users fail to load
        mFakeEventBus.post(new KnownUsersLoadFailedEvent(KnownUsersLoadFailedEvent.REASON_UNKNOWN));
        // THEN the sync fail dialog is shown
        verify(mMockUi).showSyncFailedDialog(true);
    }

    /** Tests that the sync failed dialog is hidden when users are successfully loaded. */
    public void testUserLoaded_hidesSyncFailedDialog() {
        // GIVEN initialized controller
        mController.init();
        // WHEN users are loaded
        User user = new User("idA", "nameA");
        mFakeEventBus.post(new KnownUsersLoadedEvent(ImmutableSet.of(user)));
        // THEN the sync fail dialog is hidden
        verify(mMockUi).showSyncFailedDialog(false);
    }

    /** Tests that users are requested when a retry is requested. */
    public void testOnSyncRetry_requestsUsers() {
        // GIVEN initialized controller
        mController.init();
        verify(mMockUserManager).loadKnownUsers();
        // WHEN onSyncRetry is called
        mController.onSyncRetry();
        // THEN users are requested
        verify(mMockUserManager).loadKnownUsers();
    }

    /** Tests that the spinner is shown when a retry is requested. */
    public void testOnSyncRetry_showsSpinner() {
        // GIVEN initialized controller
        mController.init();
        verify(mMockFragmentUi).showSpinner(true);
        // WHEN onSyncRetry is called
        mController.onSyncRetry();
        // THEN spinner is shown
        verify(mMockFragmentUi).showSpinner(true);
    }
}
