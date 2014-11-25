package org.msf.records.model;

import android.os.Parcelable;

import java.util.Comparator;

import auto.parcel.AutoParcel;

/**
 * An object that represents a user.
 */
@AutoParcel
public abstract class User implements Parcelable, Comparable<User> {
    private static final String GUEST_ACCOUNT_NAME = "Guest";

    public static final Comparator<User> COMPARATOR_BY_ID = new Comparator<User>() {

        @Override
        public int compare(User a, User b) {
            return a.getId().compareTo(b.getId());
        }
    };

    public static final Comparator<User> COMPARATOR_BY_NAME = new Comparator<User>() {

        @Override
        public int compare(User a, User b) {
            // Special case: the guest account should always appear first if present.
            if (a.getFullName().equals(GUEST_ACCOUNT_NAME)) {
                if (b.getFullName().equals(GUEST_ACCOUNT_NAME)) {
                    return 0;
                }
                return -1;
            } else if (b.getFullName().equals(GUEST_ACCOUNT_NAME)) {
                return 1;
            }

            return a.getFullName().compareTo(b.getFullName());
        }
    };

    /**
     * The guest user.
     */
    public static User GUEST = User.create(Integer.toString(Integer.MAX_VALUE), "Guest");

    public abstract String getId();
    public abstract String getFullName();

    public static User create(String id, String fullName) {
        return new AutoParcel_User(id, fullName);
    }

    public String getInitials() {
        String[] parts = getFullName().split("\\s+");
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
