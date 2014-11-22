package org.msf.records.model;

import android.os.Parcelable;

import auto.parcel.AutoParcel;

/**
 * An object that represents a patient's location.
 */
@AutoParcel
public abstract class Location2 implements Parcelable {

    public abstract String getZone();
    public abstract String getTent();
    public abstract String getBed();

    public static Location2 create(String zone, String tent, String bed) {
        return new AutoParcel_Location2(zone, tent, bed);
    }

    public static Location2 create(PatientLocation patientLocation) {
        return new AutoParcel_Location2(
                patientLocation.zone, patientLocation.tent, patientLocation.bed);
    }

    public PatientLocation toPatientLocation() {
        PatientLocation location = new PatientLocation();
        location.zone = getZone();
        location.tent = getTent();
        location.bed = getBed();
        return location;
    }
}
