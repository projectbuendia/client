package org.msf.records.events.user;

import com.google.common.collect.ImmutableSet;

import org.msf.records.net.model.User;

/**
 * An event bus event indicating that a user was successfully added, both locally and on the server.
 */
public class UserAddedEvent extends KnownUsersChangedEvent {

    public final User addedUser;

    public UserAddedEvent(User addedUser) {
        super(ImmutableSet.of(addedUser), ImmutableSet.<User>of());
        this.addedUser = addedUser;
    }
}
