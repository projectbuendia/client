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

package org.projectbuendia.client.json;

import com.google.common.base.Preconditions;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;

import static org.projectbuendia.client.utils.Utils.eq;

/** JSON reprsentation of a user (an OpenMRS Provider). */
public class JsonUser implements Serializable, Comparable<JsonUser> {
    public String uuid;
    public String fullName;

    // GUEST_ACCOUNT_NAME must match the name defined in UserResource on the server side.
    // The only special handling for this user is that (a) the server automatically
    // creates this user and (b) the client always sorts it first when showing a list.

    // TODO/i18n: This will be tricky to internationalize as it's stored on the server.
    // Perhaps create the guest account with a special name like "*" on the server, and replace
    // "*" with the localized string for "Guest User" on the client when displaying the user?
    private static final String GUEST_ACCOUNT_NAME = "Guest User";

    public static final Comparator<JsonUser> COMPARATOR_BY_UUID = (a, b) -> a.uuid.compareTo(b.uuid);

    public static final Comparator<JsonUser> COMPARATOR_BY_NAME = (a, b) -> {
        // Special case: the guest account should always appear first if present.
        int aSection = a.isGuestUser() ? 1 : 2;
        int bSection = b.isGuestUser() ? 1 : 2;
        if (aSection != bSection) {
            return aSection - bSection;
        }
        return a.fullName.compareTo(b.fullName);
    };

    /** Default constructor for serialization. */
    public JsonUser() {
        // Intentionally blank.
    }

    /** Creates a user with the given unique id and full name. */
    public JsonUser(String uuid, String fullName) {
        Preconditions.checkNotNull(uuid);
        Preconditions.checkNotNull(fullName);
        this.uuid = uuid;
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
        return COMPARATOR_BY_UUID.compare(this, other);
    }

    @Override public int hashCode() {
        return Objects.hash(uuid);
    }

    @Override public boolean equals(Object other) {
        return other instanceof JsonUser && eq(uuid, ((JsonUser) other).uuid);
    }

    public final boolean isGuestUser() {
        return GUEST_ACCOUNT_NAME.equals(fullName);
    }
}
