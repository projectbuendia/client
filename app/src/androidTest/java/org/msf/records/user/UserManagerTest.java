package org.msf.records.user;

import static org.mockito.Mockito.when;

import android.test.InstrumentationTestCase;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.msf.records.FakeAsyncTaskRunner;
import org.msf.records.events.user.KnownUsersLoadFailedEvent;
import org.msf.records.events.user.KnownUsersLoadedEvent;
import org.msf.records.net.model.User;
import org.msf.records.ui.FakeEventBus;

import com.google.common.collect.ImmutableSet;

/**
 * Tests for {@link UserManager}.
 */
public final class UserManagerTest extends InstrumentationTestCase {

    private static final User USER = new User("id", "name");

    private UserManager mUserManager;
    private FakeEventBus mFakeEventBus;
    private FakeAsyncTaskRunner mFakeAsyncTaskRunner;
    @Mock private UserStore mMockUserStore;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        mFakeEventBus = new FakeEventBus();
        mFakeAsyncTaskRunner = new FakeAsyncTaskRunner(getInstrumentation());
        mUserManager = new UserManager(
                mMockUserStore,
                mFakeEventBus,
                mFakeAsyncTaskRunner);
    }

    public void testGetActiveUser_ReturnsNullInitially() {
        // GIVEN a new UserManager instance (user never set)
        // WHEN getActiveUser is called
        // THEN it returns null
        assertNull(mUserManager.getActiveUser());
    }

    public void testLoadKnownUsers_GeneratesEventOnSucess() {
        // GIVEN the user store returns a set of users
        when(mMockUserStore.loadKnownUsers()).thenReturn(ImmutableSet.of(USER));
        // WHEN loadKnownUsers is called and the async task is run
        mUserManager.loadKnownUsers();
        mFakeAsyncTaskRunner.runUntilEmpty();
        // THEN the user manager fires off a KnownUsersLoadedEvent
        assertTrue(mFakeEventBus.getEventLog().contains(
                new KnownUsersLoadedEvent(ImmutableSet.of(USER))));
    }

    public void testLoadKnownUsers_GeneratesEventOnFailure() {
        // GIVEN the user store returns an empty set of users
        when(mMockUserStore.loadKnownUsers()).thenReturn(ImmutableSet.<User>of());
        // WHEN loadKnownUsers is called and the async task is run
        mUserManager.loadKnownUsers();
        mFakeAsyncTaskRunner.runUntilEmpty();
        // THEN the user manager fires off a KnownUsersLoadFailedEvent
        mFakeEventBus.assertEventLogContains(
                new KnownUsersLoadFailedEvent(KnownUsersLoadFailedEvent.REASON_NO_USERS_RETURNED));
    }
}