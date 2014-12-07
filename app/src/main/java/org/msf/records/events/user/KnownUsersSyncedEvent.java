package org.msf.records.events.user;

import com.google.common.collect.ImmutableSet;

import org.msf.records.net.model.User;

/**
 * An event bus event indicating that the set of known users was synced from the server.
 */
public class KnownUsersSyncedEvent extends KnownUsersChangedEvent {

    public KnownUsersSyncedEvent(ImmutableSet<User> addedUsers, ImmutableSet<User> deletedUsers) {
        super(addedUsers, deletedUsers);
    }
}
