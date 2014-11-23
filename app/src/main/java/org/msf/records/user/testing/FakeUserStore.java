package org.msf.records.user.testing;

import com.google.common.collect.Sets;

import org.msf.records.model.User;
import org.msf.records.user.UserStore;

import java.util.Set;

/**
 * A fake user store for testing out user flows before we have server integration.
 */
public class FakeUserStore extends UserStore {

    private Set<User> mServerKnownUsers;
    private Set<User> mLocalKnownUsers;
    private int mNextId;

    public FakeUserStore() {
        User user1 = User.create("1", "Adam Kalachman");
        User user2 = User.create("2", "David Chen");
        User user3 = User.create("3", "Ganesh Shankar");
        User user4 = User.create("4", "Isabella Pighi");
        User user5 = User.create("5", "Jon Skeet");
        User user6 = User.create("6", "Ka-Ping Yee");
        User user7 = User.create("7", "Ken Krieger");
        User user8 = User.create("8", "Nick Fortescue");
        User user9 = User.create("9", "Pete Gillin");
        User user10 = User.create("10", "Steve Crossan");

        // Server doesn't have Nick.
        mServerKnownUsers =
                Sets.newHashSet(user2, user3, user4, user5, user6, user7, user8, user9, user10);

        // Local doesn't have Ping.
        mLocalKnownUsers =
                Sets.newHashSet(user1, user2, user3, user4, user5, user6, user7, user8, user9);

        mNextId = 11;
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
