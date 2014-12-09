package org.msf.records.ui;

import android.test.AndroidTestCase;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.msf.records.events.user.KnownUsersLoadedEvent;
import org.msf.records.net.model.User;
import org.msf.records.user.UserManager;

import de.greenrobot.event.EventBus;

import static org.mockito.Mockito.verify;

/**
 * Tests for {@link UserLoginController}.
 */
public class UserLoginControllerTest extends AndroidTestCase {
	
	private UserLoginController mController;
	@Mock private UserManager mMockUserManager; 
	@Mock private UserLoginController.Ui mMockUi;
	private EventBus mEventBus;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		MockitoAnnotations.initMocks(this);
		
		// TODO: Create a fake event bus so we can check whether the controller
		// unregistered its event handler.
		mEventBus = new EventBus();
		mController = new UserLoginController(
				mMockUserManager,
				mEventBus,
				mMockUi);
	}
	
	public void testInit_SetsKnownUserLoadGoing() {
		// WHEN the controller is inited
		mController.init();
		// THEN it requests that the user manager loads the list of users
		mMockUserManager.loadKnownUsers();
    }
	
	public void testKnownUsersLoadedEvent_UpdatesUi() {
		// GIVEN the controller is inited
		mController.init();
		// WHEN a KnownUsersLoadedEvent is sent over the event bus
		User user = User.create("idA", "nameA");
		mEventBus.post(new KnownUsersLoadedEvent(ImmutableSet.of(user)));
		// THEN the UI is updated
		verify(mMockUi).showUsers(ImmutableList.of(user));
    }
}
