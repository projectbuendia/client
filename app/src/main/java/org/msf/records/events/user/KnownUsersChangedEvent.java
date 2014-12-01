package org.msf.records.events.user;

import com.google.common.collect.ImmutableSet;

import org.msf.records.net.model.User;

/**
 * An event bus event indicating that the set of known users has changed.
 */
public class KnownUsersChangedEvent {

    private final ImmutableSet<User> mAddedUsers;
    private final ImmutableSet<User> mDeletedUsers;

    public KnownUsersChangedEvent(ImmutableSet<User> addedUsers, ImmutableSet<User> deletedUsers) {
        mAddedUsers = addedUsers;
        mDeletedUsers = deletedUsers;
    }
}
