package org.msf.records.model;

import android.support.annotation.Nullable;

import auto.parcel.AutoParcel;

/**
 * An object that represents a request to create a new user, which does not necessarily match the
 * visible fields for that user during retrieval (for example, the user's password).
 */
@AutoParcel
public abstract class NewUser {
    private static final String DEFAULT_PASSWORD = "Password123";

    public abstract String getUsername();
    public abstract String getGivenName();
    public abstract String getFamilyName();
    public abstract String getPassword();

    public static NewUser create(
            String username, String givenName, String familyName) {
        return new AutoParcel_NewUser(
                username, givenName, familyName, DEFAULT_PASSWORD);
    }
}
