package org.msf.records.user.testing;

import com.google.common.collect.Sets;

import org.msf.records.model.User;
import org.msf.records.user.UserStore;

import java.util.HashSet;
import java.util.Set;

/**
 * A fake user store for testing out user flows before we have server integration.
 */
public class FakeUserStore extends UserStore {

    private Set<User> mServerKnownUsers;
    private Set<User> mLocalKnownUsers;
    private int mNextId;

    public FakeUserStore() {
        User adam = User.create("0", "Adam Kalachman");
        User david = User.create("1", "David Chen");
        User kaping = User.create("2", "Ka-Ping Yee");
        User nick = User.create("3", "Nick Fortescue");

        // Server doesn't have Nick.
        mServerKnownUsers = Sets.newHashSet(adam, david, kaping);

        // Local doesn't have Ping.
        mLocalKnownUsers = Sets.newHashSet(adam, david, nick);

        mNextId = 4;
    }

    @Override
    public Set<User> loadKnownUsers() {
        return Sets.newHashSet(mLocalKnownUsers);
    }

    @Override
    public Set<User> syncKnownUsers() {
        mLocalKnownUsers = Sets.newHashSet(mServerKnownUsers);
        return Sets.newHashSet(mLocalKnownUsers);
    }

    // TODO(dxchen): Users presumably will have a server-side user ID. Should we have a data type
    // for a new user that excludes the user ID?
    @Override
    public User addUser(User user) {
        if (mLocalKnownUsers.contains(user)) {
            throw new RuntimeException("Local user already exists.");
        }

        if (mServerKnownUsers.contains(user)) {
            throw new RuntimeException("Server user already exists.");
        }

        String id = Integer.toString(mNextId++);
        User userWithId = User.create(id, user.getFullName());

        mServerKnownUsers.add(userWithId);
        mLocalKnownUsers.add(userWithId);

        return userWithId;
    }

    @Override
    public User deleteUser(User user) {
        if (!mLocalKnownUsers.contains(user)) {
            throw new RuntimeException("No local user with that ID.");
        }

        if (!mServerKnownUsers.contains(user)) {
            throw new RuntimeException("No server user with that ID.");
        }

        mServerKnownUsers.remove(user);
        mLocalKnownUsers.remove(user);

        return user;
    }
}
