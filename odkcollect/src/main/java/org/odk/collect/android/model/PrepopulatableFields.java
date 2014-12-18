package org.odk.collect.android.model;

import android.os.Parcel;
import android.os.Parcelable;

import org.joda.time.DateTime;
import org.odk.collect.android.utilities.Parcels;

/**
 * An object that contains the prepopulatable fields
 */
public class PrepopulatableFields implements Parcelable {

    public static final int UNSPECIFIED = -1;
    public static final int UNKNOWN = 1;
    public static final int YES = 2;
    public static final int NO = 3;

    public DateTime encounterTime;
    public String locationName;
    public String clinicianName;
    public int pregnant = UNSPECIFIED;
    public int ivFitted = UNSPECIFIED;
    public String targetGroup;

    public PrepopulatableFields() {}

    public PrepopulatableFields(Parcel in) {
        encounterTime = Parcels.readNullableDateTime(in);
        locationName = Parcels.readNullableString(in);
        clinicianName = Parcels.readNullableString(in);
        pregnant = in.readInt();
        ivFitted = in.readInt();
        targetGroup = Parcels.readNullableString(in);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        Parcels.writeNullableDateTime(dest, encounterTime);
        Parcels.writeNullableString(dest, locationName);
        Parcels.writeNullableString(dest, clinicianName);
        dest.writeInt(pregnant);
        dest.writeInt(ivFitted);
        Parcels.writeNullableString(dest, targetGroup);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<PrepopulatableFields> CREATOR =
            new Parcelable.Creator<PrepopulatableFields>() {

        @Override
        public PrepopulatableFields createFromParcel(Parcel in) {
            return new PrepopulatableFields(in);
        }

        @Override
        public PrepopulatableFields[] newArray(int size) {
            return new PrepopulatableFields[size];
        }
    };
}
