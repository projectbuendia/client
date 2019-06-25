package org.projectbuendia.client.models;


import android.os.Parcel;
import android.os.Parcelable;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import javax.annotation.Nullable;

public class ObsRow implements Parcelable {

    public String uuid;
    public String day;
    public String time;
    public String conceptName;
    public String conceptUuid;
    public @Nullable String value;
    public @Nullable String valueName;

    public ObsRow(String Uuid,
               long Millis,
               String conceptName,
               String conceptUuid,
               @Nullable String Value,
               @Nullable String ValueName) {

        this.conceptName = conceptName;
        this.conceptUuid = conceptUuid;

        DateTimeFormatter fmtDay = DateTimeFormat.forPattern("dd MMM yyyy");
        day =  fmtDay.print(new DateTime(Millis));

        DateTimeFormatter fmtTime = DateTimeFormat.forPattern("HH:mm");
        time =  fmtTime.print(new DateTime(Millis));

        uuid = Uuid;
        value = Value;
        valueName = ValueName;
    }

    public ObsRow(Parcel in) {
        super();
        readFromParcel(in);
    }

    public static final Parcelable.Creator<ObsRow> CREATOR = new Parcelable.Creator<ObsRow>() {
        public ObsRow createFromParcel(Parcel in) {
            return new ObsRow(in);
        }
        public ObsRow[] newArray(int size) {return new ObsRow[size];}
    };

    public void readFromParcel(Parcel in) {
        uuid = in.readString();
        day = in.readString();
        time = in.readString();
        conceptName = in.readString();
        conceptUuid = in.readString();
        value = in.readString();
        valueName = in.readString();
    }
    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(uuid);
        dest.writeString(day);
        dest.writeString(time);
        dest.writeString(conceptName);
        dest.writeString(conceptUuid);
        dest.writeString(value);
        dest.writeString(valueName);
    }

}
