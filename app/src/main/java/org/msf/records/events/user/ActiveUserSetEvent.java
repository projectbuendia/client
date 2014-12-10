package org.msf.records.events.user;

import org.msf.records.net.model.User;

/**
 * An event bus event that indicates that the active user has been set.
 */
public class ActiveUserSetEvent {

    /**
     * The previous active user.
     */
    public final User previousActiveUser;

    /**
     * The current active user.
     */
    public final User activeUser;

    /**
     * Creates a new {@link ActiveUserSetEvent}.
     */
    public ActiveUserSetEvent(User previousActiveUser, User activeUser) {
        this.previousActiveUser = previousActiveUser;
        this.activeUser = activeUser;
    }
}
