// Copyright 2015 The Project Buendia Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at: http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distrib-
// uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
// specific language governing permissions and limitations under the License.

package org.msf.records.user;

import com.google.common.collect.Sets;

import org.msf.records.net.model.NewUser;
import org.msf.records.net.model.User;

import java.util.Set;

/** A fake {@link UserStore} for testing out user flows before we have server integration. */
public class FakeUserStore extends UserStore {

    private final Set<User> mServerKnownUsers;
    private Set<User> mLocalKnownUsers;

    /**
     * Instantiates a {@link FakeUserStore} with demo data in which the faked server contains a
     * different set of users from the client, to facilitate test cases.
     */
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
