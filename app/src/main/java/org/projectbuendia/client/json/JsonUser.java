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

import org.projectbuendia.client.App;
import org.projectbuendia.client.R;
import org.projectbuendia.client.utils.Utils;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;

import javax.annotation.Nonnull;

import static org.projectbuendia.client.utils.Utils.eq;

/** JSON reprsentation of a user (an OpenMRS Provider). */
public class JsonUser implements Serializable {
    private String uuid;
    private String name;

    /** This must match the same constant on the server. */
    public static final String PROVIDER_GUEST_UUID = "buendia_provider_guest";

    public static final Comparator<JsonUser> COMPARATOR_BY_NAME = (a, b) -> {
        // The guest account always sorts first.
        int aSection = a.isGuestUser() ? 1 : 2;
        int bSection = b.isGuestUser() ? 1 : 2;
        if (aSection != bSection) {
            return aSection - bSection;
        }
        return a.name.compareTo(b.name);
    };

    /** Default constructor for serialization. */
    public JsonUser() { }

    /** Creates a user with the given unique id and full name. */
    public JsonUser(@Nonnull String uuid, @Nonnull String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public static JsonUser fromNewUser(JsonNewUser newUser) {
        String fullName = newUser.givenName + " " + newUser.familyName;
        return new JsonUser(newUser.username, fullName);
    }

    public String toString() {
        return Utils.format("<User %s [%s]>", Utils.repr(name), uuid);
    }

    @Override public int hashCode() {
        return Objects.hash(uuid);
    }

    @Override public boolean equals(Object other) {
        return other instanceof JsonUser && eq(uuid, ((JsonUser) other).uuid);
    }

    public final boolean isGuestUser() {
        return eq(getUuid(), PROVIDER_GUEST_UUID);
    }

    public String getName() {
        return name;
    }

    public String getLocalizedName() {
        return isGuestUser() ? App.str(R.string.guest_user_name) : name;
    }

    /** Returns the user's initials, using the first letter of each word of the user's full name. */
    public String getLocalizedInitials() {
        if (isGuestUser()) {
            return App.str(R.string.guest_user_initials);
        }
        String[] parts = name.split("\\s+");
        switch (parts.length) {
            case 0:
                return "?";
            case 1:
                return parts[0].substring(0, 1);
            default:
                return parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1);
        }
    }

    public String getUuid() {
        return uuid;
    }
}
