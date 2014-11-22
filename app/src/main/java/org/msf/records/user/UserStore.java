package org.msf.records.user;

import org.msf.records.model.User;

import java.util.Set;

/**
 * A store for users.
 */
public class UserStore {

    public Set<User> loadKnownUsers() {
        throw new UnsupportedOperationException();
    }

    public Set<User> syncKnownUsers() {
        throw new UnsupportedOperationException();
    }

    // TODO(dxchen): Users presumably will have a server-side user ID. Should we have a data type
    // for a new user that excludes the user ID?
    public User addUser(User user) {
        throw new UnsupportedOperationException();
    }

    public User deleteUser(User user) {
        throw new UnsupportedOperationException();
    }
}
