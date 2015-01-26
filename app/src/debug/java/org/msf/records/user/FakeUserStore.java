package org.msf.records.user;

import com.google.common.collect.Sets;

import org.msf.records.net.model.NewUser;
import org.msf.records.net.model.User;
import org.msf.records.user.UserStore;

import java.util.Set;

/**
 * A fake {@link UserStore} for testing out user flows before we have server integration.
 */
public class FakeUserStore extends UserStore {

    private final Set<User> mServerKnownUsers;
    private Set<User> mLocalKnownUsers;

    public FakeUserStore() {
        User akalachman = new User("akalachman", "Adam Kalachman");
        User cpritchard = new User("cpritchard", "Corinne Pritchard");
        User dan = new User("dan", "Dan Cunningham");
        User danielsjulio = new User("danielsjulio", "Daniel Julio");
        User dxchen = new User("dxchen", "David Chen");
        User gilsjulio = new User("gilsjulio", "Gil Julio");
        User gansha = new User("gansha", "Ganesh Shankar");
        User isabella = new User("isabella", "Isabella Pighi");
        User ivangayton = new User("ivangayton", "Ivan Gayton");
        User jonskeet = new User("jonskeet", "Jon Skeet");
        User kenk = new User("kenk", "Ken Krieger");
        User koen = new User("koen", "Koen Vendrik");
        User kpy = new User("kpy", "Ka-Ping Yee");
        User madhul = new User("madhul", "Madhuwati Lagu");
        User nfortescue = new User("nfortescue", "Nick Fortescue");
        User peteg = new User("peteg", "Pete Gillin");
        User pim = new User("pim", "Pim de Witte");
        User sanderlatour = new User("sanderlatour", "Sander Latour");
        User scrossan = new User("scrossan", "Steve Crossan");

        Set<User> allKnownUsers =
            Sets.newHashSet(akalachman, cpritchard, dan, danielsjulio, dxchen, gilsjulio,
                gansha, isabella, ivangayton, jonskeet, kenk, koen, kpy, madhul,
                nfortescue, peteg, pim, sanderlatour, scrossan);

        // Server doesn't have Nick.
        mServerKnownUsers = Sets.newHashSet(allKnownUsers);
        mServerKnownUsers.remove(nfortescue);

        // Local doesn't have Ping.
        mLocalKnownUsers = Sets.newHashSet(allKnownUsers);
        mLocalKnownUsers.remove(kpy);

        //noinspection UnusedAssignment
        int nextId = 1;
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

    @Override
    public User addUser(NewUser user) {
        User asUser = User.fromNewUser(user);
        if (mLocalKnownUsers.contains(asUser)) {
            throw new RuntimeException("Local user already exists.");
        }

        if (mServerKnownUsers.contains(asUser)) {
            throw new RuntimeException("Server user already exists.");
        }

        mServerKnownUsers.add(asUser);
        mLocalKnownUsers.add(asUser);

        return asUser;
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
