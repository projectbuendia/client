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
                mMockUi);
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
        verify(mMockUi).showUsers(ImmutableList.of(user));
    }

    /** Tests that an error is shown if users fail to load. */
    public void testKnownUsersLoadFailed_ShowsErrorUi() throws Exception {
        // GIVEN the controller is inited
        mController.init();
        // WHEN an error occurs loading the list of users
        mFakeEventBus.post(new KnownUsersLoadFailedEvent(KnownUsersLoadFailedEvent.REASON_UNKNOWN));
        // THEN an error is displayed
        verify(mMockUi).showErrorToast(anyInt());
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

}
