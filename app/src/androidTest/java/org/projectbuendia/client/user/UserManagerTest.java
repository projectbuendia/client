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

package org.projectbuendia.client.user;

import android.test.InstrumentationTestCase;

import com.google.common.collect.ImmutableSet;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.projectbuendia.client.FakeAsyncTaskRunner;
import org.projectbuendia.client.events.user.KnownUsersLoadFailedEvent;
import org.projectbuendia.client.events.user.KnownUsersLoadedEvent;
import org.projectbuendia.client.json.JsonUser;
import org.projectbuendia.client.ui.FakeEventBus;

import static org.mockito.Mockito.when;

/** Tests for {@link UserManager}. */
public final class UserManagerTest extends InstrumentationTestCase {

    private static final JsonUser USER = new JsonUser("id", "name");

    private UserManager mUserManager;
    private FakeEventBus mFakeEventBus;
    private FakeAsyncTaskRunner mFakeAsyncTaskRunner;
    @Mock private UserStore mMockUserStore;

    /** Tests that getActiveUser() returns null if the user is never set. */
    public void testGetActiveUser_ReturnsNullInitially() {
        // GIVEN a new UserManager instance (user never set)
        // WHEN getActiveUser is called
        // THEN it returns null
        assertNull(mUserManager.getActiveUser());
    }

    /** Tests that an event is posted when users are loaded successfully. */
    public void testLoadKnownUsers_GeneratesEventOnSucess() throws Exception {
        // GIVEN the user store returns a set of users
        when(mMockUserStore.loadKnownUsers()).thenReturn(ImmutableSet.of(USER));
        // WHEN loadKnownUsers is called and the async task is run
        mUserManager.loadKnownUsers();
        mFakeAsyncTaskRunner.runUntilEmpty();
        // THEN the user manager fires off a KnownUsersLoadedEvent
        assertTrue(mFakeEventBus.getEventLog().contains(
            new KnownUsersLoadedEvent(ImmutableSet.of(USER))));
    }

    /** Tests that an event is posted when users fail to load. */
    public void testLoadKnownUsers_GeneratesEventOnFailure() throws Exception {
        // GIVEN the user store returns an empty set of users
        when(mMockUserStore.loadKnownUsers()).thenReturn(ImmutableSet.<JsonUser> of());
        // WHEN loadKnownUsers is called and the async task is run
        mUserManager.loadKnownUsers();
        mFakeAsyncTaskRunner.runUntilEmpty();
        // THEN the user manager fires off a KnownUsersLoadFailedEvent
        mFakeEventBus.assertEventLogContains(
            new KnownUsersLoadFailedEvent(KnownUsersLoadFailedEvent.REASON_NO_USERS_RETURNED));
    }

    @Override protected void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        mFakeEventBus = new FakeEventBus();
        mFakeAsyncTaskRunner = new FakeAsyncTaskRunner(getInstrumentation());
        mUserManager = new UserManager(
            mMockUserStore,
            mFakeEventBus,
            mFakeAsyncTaskRunner);
    }
}
