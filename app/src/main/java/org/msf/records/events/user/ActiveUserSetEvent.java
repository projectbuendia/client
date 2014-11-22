package org.msf.records.events.user;

import org.msf.records.model.User;

/**
 * An event bus event that indicates that the active user has been set.
 */
public class ActiveUserSetEvent {

    /**
     * The current active user.
     */
    public final User mActiveUser;

    /**
     * Creates a new {@link ActiveUserSetEvent}.
     */
    public ActiveUserSetEvent(User activeUser) {
        mActiveUser = activeUser;
    }
}
