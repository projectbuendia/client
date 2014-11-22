package org.msf.records.user;

import android.os.AsyncTask;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

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
import org.msf.records.model.User;
import org.msf.records.user.testing.FakeUserStore;

import java.util.HashSet;
import java.util.Set;

import de.greenrobot.event.EventBus;

/**
 * An object that manages the available logins and the currently logged-in user.
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
 * <p>This class is thread-safe.
 */
public class UserManager {

    /**
     * A lock object for the set of known users. If used with {@link #mActiveUserLock}, this lock
     * must be acquired first.
     */
    private final Object mKnownUsersLock = new Object();

    /**
     * A lock object for the current active user. If used with {@link #mKnownUsersLock}, this lock
     * must be acquired second.
     */
    private final Object mActiveUserLock = new Object();

    private final UserStore mUserStore;

    private Set<User> mKnownUsers;
    private User mActiveUser;

    public UserManager() {
        mUserStore = new FakeUserStore();
    }

    /**
     * Loads the set of all users known to the application from local cache.
     *
     * <p>This method will post a {@link KnownUsersLoadedEvent} if the known users were successfully
     * loaded and a {@link KnownUsersLoadFailedEvent} otherwise.
     *
     * <p>This method will only perform a local cache lookup once per application lifetime.
     */
    public void loadKnownUsers() {
        synchronized (mKnownUsersLock) {
            if (mKnownUsers == null) {
                new LoadKnownUsersTask().execute();
            } else {
                EventBus.getDefault().post(
                        new KnownUsersLoadedEvent(ImmutableSet.copyOf(mKnownUsers)));
            }
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
        new SyncKnownUsersTask().execute();
    }

    /**
     * Returns the current active user or {@code null} if no user is active.
     */
    public User getActiveUser() {
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
    public boolean setActiveUser(User activeUser) {
        synchronized(mKnownUsersLock) {
            synchronized (mActiveUserLock) {
                User previousActiveUser = mActiveUser;
                if (activeUser == null) {
                    mActiveUser = null;

                    EventBus.getDefault().post(new ActiveUserUnsetEvent(
                            previousActiveUser, ActiveUserUnsetEvent.REASON_UNSET_INVOKED));

                    return true;
                }

                if (!mKnownUsers.contains(activeUser)) {
                    // TODO(dxchen): Consider logging.

                    return false;
                }

                mActiveUser = activeUser;

                EventBus.getDefault().post(new ActiveUserSetEvent(previousActiveUser, activeUser));

                return true;
            }
        }
    }

    /**
     * Unsets the current active user, returning the previous active user or {@code null} if no
     * user was active.
     *
     * <p>This method will post an {@link ActiveUserUnsetEvent} if there was a previous active user.
     */
    public User unsetActiveUser(User activeUser) {
        synchronized(mActiveUserLock) {
            User previousActiveUser = mActiveUser;
            mActiveUser = null;

            if (previousActiveUser != null) {
                // TODO(dxchen): Post ActiveUserUnsetEvent.
            }

            return previousActiveUser;
        }
    }

    /**
     * Adds a user to the set of known users, both locally and on the server.
     *
     * <p>This method will post a {@link UserAddedEvent} if the user was added successfully and a
     * {@link UserAddFailedEvent} otherwise.
     */
    public void addUser(User user) {
        if (user == null) {
            throw new NullPointerException("User cannot be null.");
        }

        // TODO(dxchen): Validate user.

        new AddUserTask(user).execute();
    }

    /**
     * Deletes a user from the set of known users, both locally and on the server.
     *
     * <p>This method will post a {@link UserDeletedEvent} if the user was deleted successfully and
     * a {@link UserDeleteFailedEvent} otherwise.
     */
    public void deleteUser(User user) {
        if (user == null) {
            throw new NullPointerException("User cannot be null.");
        }

        // TODO(dxchen): Validate user.

        new DeleteUserTask(user).execute();
    }

    private class LoadKnownUsersTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            Set<User> newKnownUsers;
            try {
                newKnownUsers = mUserStore.loadKnownUsers();
            } catch (Exception e) {
                // TODO(dxchen): Log. Figure out type of exception to throw.
                EventBus.getDefault().post(
                        new KnownUsersLoadFailedEvent(KnownUsersLoadFailedEvent.REASON_UNKNOWN));

                return null;
            }

            synchronized (mKnownUsersLock) {
                mKnownUsers = new HashSet<User>(newKnownUsers);
                EventBus.getDefault()
                        .post(new KnownUsersLoadedEvent(ImmutableSet.copyOf(newKnownUsers)));
            }

            return null;
        }
    }

    private class SyncKnownUsersTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            Set<User> syncedKnownUsers;
            try {
                syncedKnownUsers = mUserStore.syncKnownUsers();
            } catch (Exception e) {
                // TODO(dxchen): Log. Figure out the type of exception to throw.
                EventBus.getDefault().post(
                        new KnownUsersSyncFailedEvent(KnownUsersSyncFailedEvent.REASON_UNKNOWN));

                return null;
            }

            synchronized (mKnownUsersLock) {
                ImmutableSet<User> addedUsers =
                        ImmutableSet.copyOf(Sets.difference(syncedKnownUsers, mKnownUsers));
                ImmutableSet<User> deletedUsers =
                        ImmutableSet.copyOf(Sets.difference(mKnownUsers, syncedKnownUsers));

                mKnownUsers = syncedKnownUsers;

                EventBus.getDefault()
                        .post(new KnownUsersSyncedEvent(addedUsers, deletedUsers));

                synchronized (mActiveUserLock) {
                    if (mActiveUser != null && deletedUsers.contains(mActiveUser)) {
                        EventBus.getDefault().post(new ActiveUserUnsetEvent(
                                mActiveUser, ActiveUserUnsetEvent.REASON_USER_DELETED));
                    }
                }
            }

            return null;
        }
    }

    private class AddUserTask extends AsyncTask<Void, Void, Void> {

        private final User mUser;

        public AddUserTask(User user) {
            mUser = user;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            User addedUser;
            try {
                addedUser = mUserStore.addUser(mUser);
            } catch (Exception e) {
                // TODO(dxchen): Log. Figure out the type of exception to throw.
                EventBus.getDefault()
                        .post(new UserAddFailedEvent(mUser, UserAddFailedEvent.REASON_UNKNOWN));

                return null;
            }

            synchronized (mKnownUsersLock) {
                mKnownUsers.add(addedUser);

                EventBus.getDefault().post(new UserAddedEvent(addedUser));
            }

            return null;
        }
    }

    private class DeleteUserTask extends AsyncTask<Void, Void, Void> {

        private final User mUser;

        public DeleteUserTask(User user) {
            mUser = user;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                mUserStore.deleteUser(mUser);
            } catch (Exception e) {
                // TODO(dxchen): Log. Figure out the type of exception to throw.
                EventBus.getDefault()
                        .post(new UserDeleteFailedEvent(
                                mUser, UserDeleteFailedEvent.REASON_UNKNOWN));

                return null;
            }

            synchronized (mKnownUsersLock) {
                mKnownUsers.remove(mUser);

                EventBus.getDefault().post(new UserDeletedEvent(mUser));
            }

            return null;
        }
    }
}
