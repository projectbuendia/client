package org.odk.collect.android.model;

import android.os.Parcelable;

import auto.parcel.AutoParcel;

/**
 * An object that represents a patient.
 */
@AutoParcel
public abstract class Patient implements Parcelable {

    public abstract String getUuid();
    public abstract String getId();
    public abstract String getGivenName();
    public abstract String getFamilyName();

    public static Patient create(String uuid, String id, String givenName, String familyName) {
        return new AutoParcel_Patient(uuid, id, givenName, familyName);
    }
}
