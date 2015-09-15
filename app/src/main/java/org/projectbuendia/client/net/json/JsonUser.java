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

package org.projectbuendia.client.net.json;

import com.google.common.base.Preconditions;

import java.io.Serializable;
import java.util.Comparator;

/** JSON reprsentation of a user (an OpenMRS Provider). */
public class JsonUser implements Serializable, Comparable<JsonUser> {
    public String id;
    public static final Comparator<JsonUser> COMPARATOR_BY_ID = new Comparator<JsonUser>() {

        @Override public int compare(JsonUser a, JsonUser b) {
            return a.id.compareTo(b.id);
        }
    };
    public String fullName;
    private static final String GUEST_ACCOUNT_NAME = "Guest User";
    public static final Comparator<JsonUser> COMPARATOR_BY_NAME = new Comparator<JsonUser>() {

        @Override public int compare(JsonUser a, JsonUser b) {
            // Special case: the guest account should always appear first if present.
            int aSection = a.isGuestUser() ? 1 : 2;
            int bSection = b.isGuestUser() ? 1 : 2;
            if (aSection != bSection) {
                return aSection - bSection;
            }
            return a.fullName.compareTo(b.fullName);
        }
    };

    /** Default constructor for serialization. */
    public JsonUser() {
        // Intentionally blank.
    }

    /** Creates a user with the given unique id and full name. */
    public JsonUser(String id, String fullName) {
        Preconditions.checkNotNull(id);
        Preconditions.checkNotNull(fullName);
        this.id = id;
        this.fullName = fullName;
    }

    public static JsonUser fromNewUser(JsonNewUser newUser) {
        String fullName = newUser.givenName + " " + newUser.familyName;
        return new JsonUser(newUser.username, fullName);
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

    @Override public int compareTo(JsonUser other) {
        return COMPARATOR_BY_ID.compare(this, other);
    }

    public final boolean isGuestUser() {
        return GUEST_ACCOUNT_NAME.equals(fullName);
    }
}
