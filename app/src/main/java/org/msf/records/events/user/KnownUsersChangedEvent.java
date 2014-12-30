package org.msf.records.events.user;

import com.google.common.collect.ImmutableSet;

import org.msf.records.net.model.User;

/**
 * An event bus event indicating that the set of known users has changed.
 */
public class KnownUsersChangedEvent {

    public final ImmutableSet<User> addedUsers;
    public final ImmutableSet<User> deletedUsers;

    public KnownUsersChangedEvent(ImmutableSet<User> addedUsers, ImmutableSet<User> deletedUsers) {
        this.addedUsers = addedUsers;
        this.deletedUsers = deletedUsers;
    }
}
