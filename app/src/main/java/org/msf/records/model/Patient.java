package org.msf.records.model;

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

    public static Patient create(org.msf.records.net.model.Patient netPatient) {
        return new AutoParcel_Patient(
                netPatient.uuid, netPatient.id, netPatient.given_name, netPatient.family_name);
    }
}
