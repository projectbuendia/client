package org.msf.records.user;

import static com.google.common.base.Preconditions.checkNotNull;

import android.os.AsyncTask;
import android.util.Log;

import org.msf.records.events.user.ActiveUserSetEvent;
import org.msf.records.events.user.ActiveUserUnsetEvent;
import org.msf.records.events.user.KnownUsersLoadFailedEvent;
import org.msf.records.events.user.KnownUsersLoadedEvent;
import org.msf.records.events.user.KnownUsersSyncFailedEvent;
import org.msf.records.events.user.KnownUsersSyncedEvent;
import org.msf.records.events.user.UserAddFailedEvent;
import org.msf.records.events.user.UserAddedEvent;
import org.msf.records.events.user.UserDeleteFailedEvent;
import org.msf.records.events.user.UserDeletedEvent;
import org.msf.records.net.model.NewUser;
import org.msf.records.net.model.User;
import org.msf.records.utils.AsyncTaskRunner;
import org.msf.records.utils.EventBusInterface;

import com.android.volley.VolleyError;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * Manages the available logins and the currently logged-in user.
 *
 * <p>All classes that care about the current active user should be able to gracefully handle the
 * following event bus events:
 * <ul>
 *     <li>{@link ActiveUserSetEvent}</li>
 *     <li>{@link ActiveUserUnsetEvent}</li>
 * </ul>
 *
 * <p>All classes that care about all known users should additionally be able to gracefully handle
 * the following event bus events:
 * <ul>
 *     <li>{@link KnownUsersLoadedEvent}</li>
 *     <li>{@link KnownUsersLoadFailedEvent}</li>
 *     <li>{@link KnownUsersSyncedEvent}</li>
 *     <li>{@link KnownUsersSyncFailedEvent}</li>
 * </ul>
 *
 * <p>All classes that care about being able to add and delete users should additionally be able
 * gracefully handle the following event bus events:
 * <ul>
 *     <li>{@link UserAddedEvent}</li>
 *     <li>{@link UserAddFailedEvent}</li>
 *     <li>{@link UserDeletedEvent}</li>
 *     <li>{@link UserDeleteFailedEvent}</li>
 * </ul>
 *
 * <p>All methods should be called on the main thread.
 */
public class UserManager {

    private static final String TAG = UserManager.class.getName();

    private final UserStore mUserStore;
    private final EventBusInterface mEventBus;
    private final AsyncTaskRunner mAsyncTaskRunner;

    private final Set<User> mKnownUsers = new HashSet<User>();
    private boolean mSynced = false;
    @Nullable private User mActiveUser;

    public UserManager(
            UserStore userStore,
            EventBusInterface eventBus,
            AsyncTaskRunner asyncTaskRunner) {
        mAsyncTaskRunner = checkNotNull(asyncTaskRunner);
        mEventBus = checkNotNull(eventBus);
        mUserStore = checkNotNull(userStore);
    }

    /**
     * Loads the set of all users known to the application from local cache.
     *
     * <p>This method will post a {@link KnownUsersLoadedEvent} if the known users were
     * successfully loaded and a {@link KnownUsersLoadFailedEvent} otherwise.
     *
     * <p>This method will only perform a local cache lookup once per application lifetime.
     */
    public void loadKnownUsers() {
        if (!mSynced) {
            mAsyncTaskRunner.runTask(new LoadKnownUsersTask());
        } else {
            mEventBus.post(new KnownUsersLoadedEvent(ImmutableSet.copyOf(mKnownUsers)));
        }
    }

    /**
     * Syncs the set of all users known to the application with the server.
     *
     * <p>Server synchronization will periodically happen automatically, but this method allows for
     * the sync to be forced.
     *
     * <p>This method will post a {@link KnownUsersSyncedEvent} if the sync succeeded and a
     * {@link KnownUsersSyncFailedEvent} otherwise. If the sync succeeded and the current active
     * user was deleted on the server, this method will post a {@link ActiveUserUnsetEvent}.
     */
    public void syncKnownUsers() {
        mAsyncTaskRunner.runTask(new SyncKnownUsersTask());
    }

    /**
     * Returns the current active user or {@code null} if no user is active.
     */
    @Nullable public User getActiveUser() {
        return mActiveUser;
    }

    /**
     * Sets the current active user or unsets it if {@code activeUser} is {@code null}, returning
     * whether the operation succeeded.
     *
     * <p>This method will fail if the specified user is not known to the application.
     *
     * <p>This method will post an {@link ActiveUserSetEvent} if the active user was successfully
     * set and an {@link ActiveUserUnsetEvent} if the active user was unset successfully; these
     * events will be posted even if the active user did not change.
     */
    public boolean setActiveUser(@Nullable User activeUser) {
        @Nullable User previousActiveUser = mActiveUser;
        if (activeUser == null) {
            mActiveUser = null;
            mEventBus.post(new ActiveUserUnsetEvent(
                    previousActiveUser, ActiveUserUnsetEvent.REASON_UNSET_INVOKED));
            return true;
        }

        if (!mKnownUsers.contains(activeUser)) {
            Log.e(TAG, "Couldn't switch user -- new user is not known");
            return false;
        }

        mActiveUser = activeUser;
        mEventBus.post(new ActiveUserSetEvent(previousActiveUser, activeUser));
        return true;
    }

    /**
     * Adds a user to the set of known users, both locally and on the server.
     *
     * <p>This method will post a {@link UserAddedEvent} if the user was added successfully and a
     * {@link UserAddFailedEvent} otherwise.
     */
    public void addUser(NewUser user) {
        checkNotNull(user);
        // TODO(dxchen): Validate user.
        mAsyncTaskRunner.runTask(new AddUserTask(user));
    }

    /**
     * Deletes a user from the set of known users, both locally and on the server.
     *
     * <p>This method will post a {@link UserDeletedEvent} if the user was deleted successfully and
     * a {@link UserDeleteFailedEvent} otherwise.
     */
    public void deleteUser(User user) {
        checkNotNull(user);
        // TODO(dxchen): Validate user.
        mAsyncTaskRunner.runTask(new DeleteUserTask(user));
    }

    /**
     * Loads known users from the database into memory.
     *
     * <p>Forces a network sync if the database has not been downloaded yet.
     */
    private class LoadKnownUsersTask extends AsyncTask<Void, Void, Set<User>> {
        @Override
        protected Set<User> doInBackground(Void... voids) {
            try {
                return mUserStore.loadKnownUsers();
            } catch (Exception e) {
                // TODO(dxchen): Figure out type of exception to throw.
                Log.e(TAG, "Load users task failed", e);
                mEventBus.post(
                        new KnownUsersLoadFailedEvent(KnownUsersLoadFailedEvent.REASON_UNKNOWN));
                return null;
            }
        }

        @Override
        protected void onPostExecute(Set<User> knownUsers) {
            mKnownUsers.clear();
            mKnownUsers.addAll(knownUsers);

            if (mKnownUsers.isEmpty()) {
                Log.e(TAG, "No users returned from db");
                mEventBus.post(new KnownUsersLoadFailedEvent(
                                KnownUsersLoadFailedEvent.REASON_NO_USERS_RETURNED));
            } else {
                mSynced = true;
                mEventBus.post(new KnownUsersLoadedEvent(ImmutableSet.copyOf(mKnownUsers)));
            }
        }
    }

    /** Syncs the user list with the server. */
    private final class SyncKnownUsersTask extends AsyncTask<Void, Void, Set<User>> {
        @Override
        protected Set<User> doInBackground(Void... voids) {
            try {
               return mUserStore.syncKnownUsers();
            } catch (Exception e) {
                // TODO(dxchen): Figure out the type of exception to throw.
                Log.e(TAG, "User sync failed", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(Set<User> syncedUsers) {
            if (syncedUsers == null) {
                mEventBus.post(
                        new KnownUsersSyncFailedEvent(KnownUsersSyncFailedEvent.REASON_UNKNOWN));
                return;
            }

            ImmutableSet<User> addedUsers =
                    ImmutableSet.copyOf(Sets.difference(syncedUsers, mKnownUsers));
            ImmutableSet<User> deletedUsers =
                    ImmutableSet.copyOf(Sets.difference(mKnownUsers, syncedUsers));

            mKnownUsers.clear();
            mKnownUsers.addAll(syncedUsers);
            mEventBus.post(new KnownUsersSyncedEvent(addedUsers, deletedUsers));

            if (mActiveUser != null && deletedUsers.contains(mActiveUser)) {
                // TODO(rjlothian): Should we clear mActiveUser here?
                mEventBus.post(new ActiveUserUnsetEvent(
                        mActiveUser, ActiveUserUnsetEvent.REASON_USER_DELETED));
            }
        }
    }

    /**Adds a user to the database asynchronously. */
    private final class AddUserTask extends AsyncTask<Void, Void, User> {

        private final NewUser mUser;
        private boolean mAlreadyExists;

        public AddUserTask(NewUser user) {
            mUser = checkNotNull(user);
        }

        @Override
        protected User doInBackground(Void... voids) {
            try {
                User addedUser = mUserStore.addUser(mUser);
                return addedUser;
            } catch (VolleyError e) {
                if (e.getMessage() != null && e.getMessage().contains("already in use")) {
                    mAlreadyExists = true;
                }
                return null;
            }
        }

        @Override
        protected void onPostExecute(User addedUser) {
            if (addedUser != null) {
                mKnownUsers.add(addedUser);
                mEventBus.post(new UserAddedEvent(addedUser));
            } else if (mAlreadyExists) {
                mEventBus.post(new UserAddFailedEvent(
                        mUser, UserAddFailedEvent.REASON_USER_EXISTS_ON_SERVER));
            } else {
                mEventBus.post(new UserAddFailedEvent(mUser, UserAddFailedEvent.REASON_UNKNOWN));
            }
        }
    }

    /** Deletes a user from the database asynchronously. */
    private final class DeleteUserTask extends AsyncTask<Void, Void, Boolean> {
        private final User mUser;

        public DeleteUserTask(User user) {
            mUser = checkNotNull(user);
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                mUserStore.deleteUser(mUser);
            } catch (Exception e) {
                // TODO(dxchen): Figure out the type of exception to throw.
                Log.e(TAG, "Failed to delete user", e);
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                mKnownUsers.remove(mUser);
                mEventBus.post(new UserDeletedEvent(mUser));
            } else {
                mEventBus.post(new UserDeleteFailedEvent(
                    mUser, UserDeleteFailedEvent.REASON_UNKNOWN));
            }
        }
    }
}
