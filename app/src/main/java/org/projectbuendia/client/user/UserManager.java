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

import android.content.OperationApplicationException;
import android.os.AsyncTask;
import android.os.RemoteException;

import com.android.volley.VolleyError;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import org.projectbuendia.client.events.user.ActiveUserSetEvent;
import org.projectbuendia.client.events.user.ActiveUserUnsetEvent;
import org.projectbuendia.client.events.user.KnownUsersLoadFailedEvent;
import org.projectbuendia.client.events.user.KnownUsersLoadedEvent;
import org.projectbuendia.client.events.user.KnownUsersSyncFailedEvent;
import org.projectbuendia.client.events.user.KnownUsersSyncedEvent;
import org.projectbuendia.client.events.user.UserAddFailedEvent;
import org.projectbuendia.client.events.user.UserAddedEvent;
import org.projectbuendia.client.json.JsonNewUser;
import org.projectbuendia.client.json.JsonUser;
import org.projectbuendia.client.utils.AsyncTaskRunner;
import org.projectbuendia.client.utils.EventBusInterface;
import org.projectbuendia.client.utils.Logger;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Manages the available logins and the currently logged-in user.
 * <p/>
 * <p>All classes that care about the current active user should be able to gracefully handle the
 * following event bus events:
 * <ul>
 * <li>{@link ActiveUserSetEvent}
 * <li>{@link ActiveUserUnsetEvent}
 * </ul>
 * <p/>
 * <p>All classes that care about all known users should additionally be able to gracefully handle
 * the following event bus events:
 * <ul>
 * <li>{@link KnownUsersLoadedEvent}
 * <li>{@link KnownUsersLoadFailedEvent}
 * <li>{@link KnownUsersSyncedEvent}
 * <li>{@link KnownUsersSyncFailedEvent}
 * </ul>
 * <p/>
 * <p>All classes that care about being able to add and delete users should additionally be able
 * gracefully handle the following event bus events:
 * <ul>
 * <li>{@link UserAddedEvent}
 * <li>{@link UserAddFailedEvent}
 * </ul>
 * <p/>
 * <p>All methods should be called on the main thread.
 */
public class UserManager {

    private static final Logger LOG = Logger.create();

    private final UserStore mUserStore;
    private final EventBusInterface mEventBus;
    private final AsyncTaskRunner mAsyncTaskRunner;

    private final Set<JsonUser> mKnownUsers = new HashSet<>();
    private boolean mSynced = false;
    private boolean mAutoCancelEnabled = false;
    private boolean mIsDirty = false;
    @Nullable private AsyncTask mLastTask;
    @Nullable private JsonUser mActiveUser;

    /**
     * Utility function for automatically canceling user load tasks to simulate network connectivity
     * issues.
     * TODO: Move to a fake or mock out when daggered.
     */
    public void setAutoCancelEnabled(boolean autoCancelEnabled) {
        mAutoCancelEnabled = autoCancelEnabled;
    }

    /** Resets the UserManager to its initial empty state. */
    public void reset() {
        mKnownUsers.clear();
        mSynced = false;
    }

    /**
     * If true, users have been recently updated and any data relying on a specific view of users
     * may be out of sync.
     */
    public boolean isDirty() {
        return mIsDirty;
    }

    /** Sets whether or not users have been recently updated. */
    public void setDirty(boolean shouldInvalidateFormCache) {
        mIsDirty = shouldInvalidateFormCache;
    }

    /**
     * Loads the set of all users known to the application from local cache.
     * <p/>
     * <p>This method will post a {@link KnownUsersLoadedEvent} if the known users were
     * successfully loaded and a {@link KnownUsersLoadFailedEvent} otherwise.
     * <p/>
     * <p>This method will only perform a local cache lookup once per application lifetime.
     */
    public void loadKnownUsers() {
        if (!mSynced) {
            mLastTask = new LoadKnownUsersTask();
            mAsyncTaskRunner.runTask(mLastTask);
        } else {
            mEventBus.post(new KnownUsersLoadedEvent(ImmutableSet.copyOf(mKnownUsers)));
        }
    }

    /** Sync users synchronously. Blocks until the list of users is synced, or interrupted. */
    public void syncKnownUsersSynchronously()
        throws InterruptedException, ExecutionException, RemoteException,
        OperationApplicationException, UserSyncException {
        onUsersSynced(mUserStore.syncKnownUsers());
    }

    /**
     * Called when users are retrieved from the server, in order to send events and update user
     * state as necessary.
     */
    private void onUsersSynced(Set<JsonUser> syncedUsers) throws UserSyncException {
        if (syncedUsers == null || syncedUsers.isEmpty()) {
            throw new UserSyncException("Set of users retrieved from server is null or empty.");
        }

        ImmutableSet<JsonUser> addedUsers =
            ImmutableSet.copyOf(Sets.difference(syncedUsers, mKnownUsers));
        ImmutableSet<JsonUser> deletedUsers =
            ImmutableSet.copyOf(Sets.difference(mKnownUsers, syncedUsers));

        mKnownUsers.clear();
        mKnownUsers.addAll(syncedUsers);
        mEventBus.post(new KnownUsersSyncedEvent(addedUsers, deletedUsers));

        if (mActiveUser != null && deletedUsers.contains(mActiveUser)) {
            // TODO: Potentially clear mActiveUser here.
            mEventBus.post(new ActiveUserUnsetEvent(
                mActiveUser, ActiveUserUnsetEvent.REASON_USER_DELETED));
        }

        // If at least one user was added or deleted, the set of known users has changed.
        if (!addedUsers.isEmpty() || !deletedUsers.isEmpty()) {
            setDirty(true);
        }
    }

    /** Returns the current active user or {@code null} if no user is active. */
    @Nullable public JsonUser getActiveUser() {
        return mActiveUser;
    }

    /**
     * Sets the current active user or unsets it if {@code activeUser} is {@code null}, returning
     * whether the operation succeeded.
     * <p/>
     * <p>This method will fail if the specified user is not known to the application.
     * <p/>
     * <p>This method will post an {@link ActiveUserSetEvent} if the active user was successfully
     * set and an {@link ActiveUserUnsetEvent} if the active user was unset successfully; these
     * events will be posted even if the active user did not change.
     */
    public boolean setActiveUser(@Nullable JsonUser activeUser) {
        @Nullable JsonUser previousActiveUser = mActiveUser;
        if (activeUser == null) {
            mActiveUser = null;
            mEventBus.post(new ActiveUserUnsetEvent(
                previousActiveUser, ActiveUserUnsetEvent.REASON_UNSET_INVOKED));
            return true;
        }

        if (!mKnownUsers.contains(activeUser)) {
            LOG.e("Couldn't switch user -- new user is not known");
            return false;
        }

        mActiveUser = activeUser;
        mEventBus.post(new ActiveUserSetEvent(previousActiveUser, activeUser));
        return true;
    }

    /**
     * Adds a user to the set of known users, both locally and on the server.
     * <p/>
     * <p>This method will post a {@link UserAddedEvent} if the user was added successfully and a
     * {@link UserAddFailedEvent} otherwise.
     */
    public void addUser(JsonNewUser user) {
        checkNotNull(user);
        // TODO: Validate user.
        mAsyncTaskRunner.runTask(new AddUserTask(user));
    }

    /** Thrown when an error occurs syncing users from server. */
    public static class UserSyncException extends Throwable {
        public UserSyncException(String s) {
            super(s);
        }
    }

    UserManager(
        UserStore userStore,
        EventBusInterface eventBus,
        AsyncTaskRunner asyncTaskRunner) {
        mAsyncTaskRunner = checkNotNull(asyncTaskRunner);
        mEventBus = checkNotNull(eventBus);
        mUserStore = checkNotNull(userStore);
    }

    /**
     * Loads known users from the database into memory.
     * <p/>
     * <p>Forces a network sync if the database has not been downloaded yet.
     */
    private class LoadKnownUsersTask extends AsyncTask<Object, Void, Set<JsonUser>> {
        @Override protected Set<JsonUser> doInBackground(Object... unusedObjects) {
            if (mAutoCancelEnabled) {
                cancel(true);
                return null;
            }

            try {
                return mUserStore.loadKnownUsers();
            } catch (Exception e) {
                // TODO: Figure out type of exception to throw.
                LOG.e(e, "Load users task failed");
                mEventBus.post(
                    new KnownUsersLoadFailedEvent(KnownUsersLoadFailedEvent.REASON_UNKNOWN));
                return null;
            }
        }

        @Override protected void onCancelled() {
            LOG.w("Load users task cancelled");
            mEventBus.post(
                new KnownUsersLoadFailedEvent(KnownUsersLoadFailedEvent.REASON_CANCELLED));
        }

        @Override protected void onPostExecute(Set<JsonUser> knownUsers) {
            mKnownUsers.clear();
            if (knownUsers != null) {
                mKnownUsers.addAll(knownUsers);
            }
            mSynced = true;
            mEventBus.post(new KnownUsersLoadedEvent(ImmutableSet.copyOf(mKnownUsers)));
        }
    }

    /** Adds a user to the database asynchronously. */
    private final class AddUserTask extends AsyncTask<Void, Void, JsonUser> {

        private final JsonNewUser mUser;
        private boolean mAlreadyExists;
        private boolean mFailedToConnect;

        public AddUserTask(JsonNewUser user) {
            mUser = checkNotNull(user);
        }

        @Override protected JsonUser doInBackground(Void... voids) {
            try {
                return mUserStore.addUser(mUser);
            } catch (VolleyError e) {
                if (e.getMessage() != null) {
                    if (e.getMessage().contains("already in use")) {
                        mAlreadyExists = true;
                    } else if (e.getMessage().contains("failed to connect")) {
                        mFailedToConnect = true;
                    }
                }
                return null;
            }
        }

        @Override protected void onPostExecute(JsonUser addedUser) {
            if (addedUser != null) {
                mKnownUsers.add(addedUser);
                mEventBus.post(new UserAddedEvent(addedUser));

                // Set of known users has changed.
                setDirty(true);
            } else if (mAlreadyExists) {
                mEventBus.post(new UserAddFailedEvent(
                    mUser, UserAddFailedEvent.REASON_USER_EXISTS_ON_SERVER));
            } else if (mFailedToConnect) {
                mEventBus.post(new UserAddFailedEvent(
                    mUser, UserAddFailedEvent.REASON_CONNECTION_ERROR));
            } else {
                mEventBus.post(new UserAddFailedEvent(mUser, UserAddFailedEvent.REASON_UNKNOWN));
            }
        }
    }
}
