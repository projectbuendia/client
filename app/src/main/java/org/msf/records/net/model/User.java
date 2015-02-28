package org.msf.records.net.model;

import android.os.Parcelable;

import com.google.common.base.Preconditions;

import java.io.Serializable;
import java.util.Comparator;

import auto.parcel.AutoParcel;

/**
 * An object that represents a user.
 */
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
            if (a.fullName.equals(GUEST_ACCOUNT_NAME)) {
                if (b.fullName.equals(GUEST_ACCOUNT_NAME)) {
                    return 0;
                }
                return -1;
            } else if (b.fullName.equals(GUEST_ACCOUNT_NAME)) {
                return 1;
            }

            return a.fullName.compareTo(b.fullName);
        }
    };

    public String id;
    public String fullName;

    /**
     * Default constructor for serialization.
     */
    public User() {
        // Intentionally blank.
    }

    /**
     * Creates a user with the given unique id and full name.
     */
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
}
