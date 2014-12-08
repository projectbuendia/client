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

    private Set<User> mServerKnownUsers;
    private Set<User> mLocalKnownUsers;
    private int mNextId;

    public FakeUserStore() {
        User akalachman = User.create("akalachman", "Adam Kalachman");
        User cpritchard = User.create("cpritchard", "Corinne Pritchard");
        User dan = User.create("dan", "Dan Cunningham");
        User danielsjulio = User.create("danielsjulio", "Daniel Julio");
        User dxchen = User.create("dxchen", "David Chen");
        User gilsjulio = User.create("gilsjulio", "Gil Julio");
        User gansha = User.create("gansha", "Ganesh Shankar");
        User isabella = User.create("isabella", "Isabella Pighi");
        User ivangayton = User.create("ivangayton", "Ivan Gayton");
        User jonskeet = User.create("jonskeet", "Jon Skeet");
        User kenk = User.create("kenk", "Ken Krieger");
        User koen = User.create("koen", "Koen Vendrik");
        User kpy = User.create("kpy", "Ka-Ping Yee");
        User madhul = User.create("madhul", "Madhuwati Lagu");
        User nfortescue = User.create("nfortescue", "Nick Fortescue");
        User peteg = User.create("peteg", "Pete Gillin");
        User pim = User.create("pim", "Pim de Witte");
        User sanderlatour = User.create("sanderlatour", "Sander Latour");
        User scrossan = User.create("scrossan", "Steve Crossan");

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

        mNextId = 1;
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
