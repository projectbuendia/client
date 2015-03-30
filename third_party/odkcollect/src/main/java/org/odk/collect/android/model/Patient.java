package org.odk.collect.android.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Patient details.
 */
public final class Patient implements Parcelable {

    // TODO: Document this field.
    public final String uuid;
    // TODO: Document this field.
    public final String id;
    public final String givenName;
    public final String familyName;

    public Patient(String uuid, String id, String givenName, String familyName) {
        this.uuid = uuid;
        this.id = id;
        this.givenName = givenName;
        this.familyName = familyName;
    }

    protected Patient(Parcel in) {
        uuid = in.readString();
        id = in.readString();
        givenName = in.readString();
        familyName = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(uuid);
        dest.writeString(id);
        dest.writeString(givenName);
        dest.writeString(familyName);
    }

    public static final Parcelable.Creator<Patient> CREATOR = new Parcelable.Creator<Patient>() {
        @Override
        public Patient createFromParcel(Parcel in) {
            return new Patient(in);
        }

        @Override
        public Patient[] newArray(int size) {
            return new Patient[size];
        }
    };
}
