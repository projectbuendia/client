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

    public DateTime mEncounterTime;
    public String mLocationName;
    public String mClinicianName;
    public int mPregnant = UNSPECIFIED;
    public int mIvFitted = UNSPECIFIED;

    public PrepopulatableFields() {}

    public PrepopulatableFields(Parcel in) {
        mEncounterTime = Parcels.readNullableDateTime(in);
        mLocationName = Parcels.readNullableString(in);
        mClinicianName = Parcels.readNullableString(in);
        mPregnant = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        Parcels.writeNullableDateTime(dest, mEncounterTime);
        Parcels.writeNullableString(dest, mLocationName);
        Parcels.writeNullableString(dest, mClinicianName);
        dest.writeInt(mPregnant);
        dest.writeInt(mIvFitted);
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
