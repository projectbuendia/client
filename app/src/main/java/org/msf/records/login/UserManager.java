package org.msf.records.login;

import org.msf.records.model.User;

import java.util.Set;

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
     * A lock object for the set of known users.
     */
    private final Object mKnownUsersLock = new Object();

    /**
     * A lock object for the current active user.
     */
    private final Object mActiveUserLock = new Object();

    private Set<User> mKnownUsers;
    private User mActiveUser;

    public UserManager() {}

    /**
     * Loads the set of all users known to the application from local cache.
     *
     * <p>This method will post a {@link KnownUsersLoadedEvent} if the known users were successfully
     * loaded and a {@link KnownUsersLoadFailedEvent} otherwise.
     */
    public void loadKnownUsers() {
        // TODO(dxchen): Load known users from local cache.
    }

    /**
     * Syncs the set of all users known to the application with the server.
     *
     * <p>Server synchronization will periodically happen automatically, but this method allows for
     * the sync to be forced.
     *
     * <p>This method will post a {@link KnownUsersSyncedEvent} if the sync succeeded and a
     * {@link KnownUsersSyncFailedEvent} otherwise.
     */
    public void syncKnownUsers() {
        // TODO(dxchen): Implement.
    }

    /**
     * Returns the current active user or {@code null} if no user is active.
     */
    public User getActiveUser() {
        return mActiveUser;
    }

    /**
     * Sets the current active user, returning whether the operation succeeded.
     *
     * <p>This method will fail if there is already an active user; the caller should first call
     * {@link #unsetActiveUser}. This method will also fail if the specified user is not known to
     * the application.
     *
     * <p>This method will post an {@link ActiveUserSetEvent} if the active user was successfully
     * set.
     */
    public boolean setActiveUser(User activeUser) {
        if (activeUser == null) {
            throw new NullPointerException("Active user cannot be null.");
        }

        synchronized(mKnownUsersLock) {
            synchronized (mActiveUserLock) {
                if (!mKnownUsers.contains(activeUser)) {
                    return false;
                }

                mActiveUser = activeUser;
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

    }

    /**
     * Deletes a user from the set of known users, both locally and on the server.
     *
     * <p>This method will post a {@link UserDeletedEvent} if the user was deleted successfully and
     * a {@link UserDeleteFailedEvent} otherwise.
     */
    public void deleteUser(User user) {

    }
}
