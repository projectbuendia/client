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

package org.projectbuendia.client.net.model;

import com.google.common.base.Preconditions;

import java.io.Serializable;
import java.util.Comparator;

/** A simple Java bean representing a user, which can be used for JSON/Gson encoding/decoding. */
public class User implements Serializable, Comparable<User> {
    private static final String GUEST_ACCOUNT_NAME = "Guest User";

    public static final Comparator<User> COMPARATOR_BY_ID = new Comparator<User>() {

        @Override
        public int compare(User a, User b) {
            return a.id.compareTo(b.id);
        }
    };

    public static final Comparator<User> COMPARATOR_BY_NAME = new Comparator<User>() {

        @Override
        public int compare(User a, User b) {
            // Special case: the guest account should always appear first if present.
            int aSection = a.isGuestUser() ? 1 : 2;
            int bSection = b.isGuestUser() ? 1 : 2;
            if (aSection != bSection) {
                return aSection - bSection;
            }
            return a.fullName.compareTo(b.fullName);
        }
    };

    public String id;
    public String fullName;

    /** Default constructor for serialization. */
    public User() {
        // Intentionally blank.
    }

    /** Creates a user with the given unique id and full name. */
    public User(String id, String fullName) {
        Preconditions.checkNotNull(id);
        Preconditions.checkNotNull(fullName);
        this.id = id;
        this.fullName = fullName;
    }

    public static User fromNewUser(NewUser newUser) {
        String fullName = newUser.givenName + " " + newUser.familyName;
        return new User(newUser.username, fullName);
    }

    /** Returns the user's initials, using the first letter of each word of the user's full name. */
    public String getInitials() {
        String[] parts = fullName.split("\\s+");
        switch (parts.length) {
            case 0:
                return "?";
            case 1:
                return parts[0].substring(0, 1);
            default:
                return parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1);
        }
    }

    @Override
    public int compareTo(User other) {
        return COMPARATOR_BY_ID.compare(this, other);
    }

    public final boolean isGuestUser() {
        return GUEST_ACCOUNT_NAME.equals(fullName);
    }
}
