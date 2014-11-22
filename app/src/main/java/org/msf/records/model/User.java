package org.msf.records.model;

import android.os.Parcelable;

import auto.parcel.AutoParcel;

/**
 * An object that represents a user.
 */
@AutoParcel
public abstract class User implements Parcelable {

    public abstract String getId();
    public abstract String getFullName();

    public static User create(String id, String fullName) {
        return new AutoParcel_User(id, fullName);
    }
}
