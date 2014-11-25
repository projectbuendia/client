package org.msf.records.events.user;

import org.msf.records.model.User;

/**
 * An event bus event that indicates that the active user has been set.
 */
public class ActiveUserSetEvent {

    /**
     * The previous active user.
     */
    public final User mPreviousActiveUser;

    /**
     * The current active user.
     */
    public final User mActiveUser;

    /**
     * Creates a new {@link ActiveUserSetEvent}.
     */
    public ActiveUserSetEvent(User previousActiveUser, User activeUser) {
        mPreviousActiveUser = previousActiveUser;
        mActiveUser = activeUser;
    }
}
