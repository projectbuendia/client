package org.msf.records.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.msf.records.R;

import java.util.Arrays;
import java.util.HashMap;

/**
 * An object that represents patient status.
 */
public class Status implements Parcelable {

    private static final HashMap<String, Status> STATUSES;

    public final String key;
    public final int nameId;
    public final int squareIconId;
    public final int roundIconId;
    public final int colorId;

    public Status(String key, int nameId, int squareIconId, int roundIconId, int colorId) {
        this.key = key;
        this.nameId = nameId;
        this.squareIconId = squareIconId;
        this.roundIconId = roundIconId;
        this.colorId = colorId;
    }

    public Status(Parcel in) {
        key = in.readString();
        nameId = in.readInt();
        squareIconId = in.readInt();
        roundIconId = in.readInt();
        colorId = in.readInt();
    }

    static {
        STATUSES = new HashMap<>();
        for (Status status : new Status[]{
                new Status("SUSPECTED_CASE", R.string.status_suspect, R.drawable.square_suspect, R.drawable.round_suspect, R.color.status_suspect_old),
                new Status("PROBABLE_CASE", R.string.status_probable, R.drawable.square_probable, R.drawable.round_probable, R.color.status_probable_old),
                new Status("CONFIRMED_CASE", R.string.status_confirmed, R.drawable.square_confirmed, R.drawable.round_confirmed, R.color.status_confirmed_old),
                new Status("NON_CASE", R.string.status_non_case, R.drawable.square_non_case, R.drawable.round_non_case, R.color.status_non_case_old),
                new Status("CONVALESCENT", R.string.status_convalescence, R.drawable.square_convalescence, R.drawable.round_convalescent, R.color.status_convalescent_old),
                new Status("READY_FOR_DISCHARGE", R.string.status_can_be_discharged, R.drawable.square_can_be_discharged, R.drawable.round_can_be_discharged, R.color.status_can_be_discharged_old),
                new Status("DISCHARGED", R.string.status_discharged_old, R.drawable.square_discharged, R.drawable.round_discharged, R.color.status_discharged_old),
                new Status("SUSPECTED_DEATH", R.string.status_unconfirmed_death, R.drawable.square_unconfirmed_death, R.drawable.round_deceased_unconfirmed, R.color.status_unconfirmed_death_old),
                new Status("CONFIRMED_DEATH", R.string.status_confirmed_death, R.drawable.square_confirmed_death, R.drawable.round_deceased_confirmed, R.color.status_confirmed_death_old)
        }) {
            STATUSES.put(status.key, status);
        }
    }

    public static Status[] getStatus() {
        Object[] objects = STATUSES.values().toArray();
        return Arrays.copyOf(objects, objects.length, Status[].class);
    }

    public static Status getStatus(String statusKey){
        Status status = STATUSES.get(statusKey);
        if (status == null) {
            Log.e("Status", "Tried to get status for unknown: '" + statusKey + '\'');
            return STATUSES.get("SUSPECTED_CASE");
        }
        return status;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(key);
        out.writeInt(nameId);
        out.writeInt(squareIconId);
        out.writeInt(roundIconId);
        out.writeInt(colorId);
    }

    public static final Parcelable.Creator<Status> CREATOR = new Parcelable.Creator<Status>() {
        public Status createFromParcel(Parcel in) {
            return new Status(in);
        }

        @Override
        public Status[] newArray(int size) {
            return new Status[size];
        }
    };
}