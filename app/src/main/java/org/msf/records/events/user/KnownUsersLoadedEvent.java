package org.msf.records.events.user;

import com.google.common.collect.ImmutableSet;

import org.msf.records.net.model.User;

/**
 * An event bus event indicating that the set of known users has been loaded from local cache.
 */
public final class KnownUsersLoadedEvent {

    public final ImmutableSet<User> mKnownUsers;

    public KnownUsersLoadedEvent(ImmutableSet<User> knownUsers) {
        mKnownUsers = knownUsers;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof KnownUsersLoadedEvent)) return false;
        KnownUsersLoadedEvent other = (KnownUsersLoadedEvent) o;
        return mKnownUsers.equals(other.mKnownUsers);
    }

    @Override
    public int hashCode() {
        return mKnownUsers.hashCode();
    }
}
