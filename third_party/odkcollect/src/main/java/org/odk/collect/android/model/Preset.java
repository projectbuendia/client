package org.odk.collect.android.model;

import android.os.Parcel;
import android.os.Parcelable;

import org.joda.time.DateTime;
import org.odk.collect.android.utilities.Parcels;

/** A Parcelable data object that specifies preset values for certain form fields. */
public class Preset implements Parcelable {

    public static final int UNSPECIFIED = -1;
    public static final int UNKNOWN = 1;
    public static final int YES = 2;
    public static final int NO = 3;

    public DateTime encounterDatetime;  // the preset value for encounter.encounter_datetime
    public String locationUuid;  // UUID of the preset value for encounter.location_id
    public String providerUuid;  // UUID of the preset value for encounter.provider_id
    public int pregnant = UNSPECIFIED;
    public int ivFitted = UNSPECIFIED;
    public String targetGroup;  // text of a section heading in the form to scroll to

    public Preset() {}

    public Preset(Parcel in) {
        encounterDatetime = Parcels.readNullableDateTime(in);
        locationUuid = Parcels.readNullableString(in);
        providerUuid = Parcels.readNullableString(in);
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
        Parcels.writeNullableDateTime(dest, encounterDatetime);
        Parcels.writeNullableString(dest, locationUuid);
        Parcels.writeNullableString(dest, providerUuid);
        dest.writeInt(pregnant);
        dest.writeInt(ivFitted);
        Parcels.writeNullableString(dest, targetGroup);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<Preset> CREATOR =
            new Parcelable.Creator<Preset>() {

        @Override
        public Preset createFromParcel(Parcel in) {
            return new Preset(in);
        }

        @Override
        public Preset[] newArray(int size) {
            return new Preset[size];
        }
    };
}
