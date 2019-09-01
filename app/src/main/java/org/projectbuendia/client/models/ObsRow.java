package org.projectbuendia.client.models;

import android.os.Parcel;
import android.os.Parcelable;

import org.joda.time.DateTime;

import javax.annotation.Nullable;

/** An observation as formatted for presentation, together with its concept name. */
public class ObsRow implements Parcelable {
    public final String uuid;
    public final DateTime time;
    public final String conceptName;
    public final String conceptUuid;
    public final @Nullable String value;
    public final @Nullable String valueName;

    // TODO(ping): Rearrange/rename to uuid, millis, questionUuid, questionName, answer, answerName.
    public ObsRow(
        String uuid, long millis, String conceptName, String conceptUuid,
        @Nullable String value, @Nullable String valueName
    ) {
        this.uuid = uuid;
        this.time = new DateTime(millis);
        this.conceptName = conceptName;
        this.conceptUuid = conceptUuid;
        this.value = value;
        this.valueName = valueName;
    }

    public ObsRow(Parcel in) {
        super();
        uuid = in.readString();
        time = new DateTime(in.readLong());
        conceptName = in.readString();
        conceptUuid = in.readString();
        value = in.readString();
        valueName = in.readString();
    }

    public static final Parcelable.Creator<ObsRow> CREATOR = new Parcelable.Creator<ObsRow>() {
        public ObsRow createFromParcel(Parcel in) {
            return new ObsRow(in);
        }

        public ObsRow[] newArray(int size) {
            return new ObsRow[size];
        }
    };

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(uuid);
        dest.writeLong(time.getMillis());
        dest.writeString(conceptName);
        dest.writeString(conceptUuid);
        dest.writeString(value);
        dest.writeString(valueName);
    }
}
