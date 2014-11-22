package org.msf.records.events.user;

import com.google.common.collect.ImmutableSet;

import org.msf.records.model.User;

import java.util.Set;

/**
 * An event bus event indicating that the set of known users has been loaded from local cache.
 */
public class KnownUsersLoadedEvent {

    private final ImmutableSet<User> mKnownUsers;

    public KnownUsersLoadedEvent(ImmutableSet<User> knownUsers) {
        mKnownUsers = knownUsers;
    }
}
