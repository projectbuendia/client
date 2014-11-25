package org.msf.records.events.user;

import org.msf.records.model.User;

/**
 * An event bus event indicating that a user was successfully deleted, both locally and on the
 * server.
 */
public class UserDeletedEvent {

    public final User mUser;

    public UserDeletedEvent(User user) {
        mUser = user;
    }
}
