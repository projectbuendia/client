package org.msf.records.events.user;

import com.google.common.collect.ImmutableSet;

import org.msf.records.net.model.User;

/**
 * An event bus event indicating that the set of known users has been loaded from local cache.
 */
public final class KnownUsersLoadedEvent {

    public final ImmutableSet<User> knownUsers;

    public KnownUsersLoadedEvent(ImmutableSet<User> knownUsers) {
        this.knownUsers = knownUsers;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof KnownUsersLoadedEvent)) {
            return false;
        }

        KnownUsersLoadedEvent other = (KnownUsersLoadedEvent) obj;
        return knownUsers.equals(other.knownUsers);
    }

    @Override
    public int hashCode() {
        return knownUsers.hashCode();
    }
}
