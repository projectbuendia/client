package org.msf.records.events.user;

import org.msf.records.net.model.User;

/**
 * An event bus event indicating that a user was successfully deleted, both locally and on the
 * server.
 */
public class UserDeletedEvent {

    public final User user;

    public UserDeletedEvent(User user) {
        this.user = user;
    }
}
