package org.msf.records.model;

import org.msf.records.net.model.PatientLocation;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * A patient's location.
 */
public final class Location2 implements Parcelable {

	public final String zone;
	public final String tent;
	public final String bed;

    public Location2(String zone, String tent, String bed) {
		this.zone = zone;
		this.tent = tent;
		this.bed = bed;
	}

	protected Location2(Parcel in) {
        zone = in.readString();
        tent = in.readString();
        bed = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(zone);
        dest.writeString(tent);
        dest.writeString(bed);
    }


    public static Location2 create(String zone, String tent, String bed) {
        return new Location2(zone, tent, bed);
    }

    public static Location2 create(PatientLocation patientLocation) {
        return new Location2(
                patientLocation.zone, patientLocation.tent, patientLocation.bed);
    }

    public PatientLocation toPatientLocation() {
        PatientLocation location = new PatientLocation();
        location.zone = zone;
        location.tent = tent;
        location.bed = bed;
        return location;
    }
    
    public static final Parcelable.Creator<Location2> CREATOR = new Parcelable.Creator<Location2>() {
        @Override
        public Location2 createFromParcel(Parcel in) {
            return new Location2(in);
        }

        @Override
        public Location2[] newArray(int size) {
            return new Location2[size];
        }
    };
}
