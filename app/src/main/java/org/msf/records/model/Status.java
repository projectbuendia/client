package org.msf.records.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.msf.records.R;

import java.util.Arrays;
import java.util.HashMap;

/**
 * Created by danieljulio on 08/10/2014.
 */
public class Status implements Parcelable, ListItem {

    private static HashMap<String, Status> STATUSES;

    public String key;
    public int nameId;
    public int squareIconId;
    public int roundIconId;
    public int colorId;

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
        STATUSES = new HashMap<String, Status>();
        STATUSES.put("suspected", new Status("suspected", R.string.status_suspect, R.drawable.square_suspect, R.drawable.round_suspect, R.color.status_suspect));
        STATUSES.put("probable", new Status("probable", R.string.status_probable, R.drawable.square_probable, R.drawable.round_probable, R.color.status_probable));
        STATUSES.put("confirmed", new Status("confirmed", R.string.status_confirmed, R.drawable.square_confirmed, R.drawable.round_confirmed, R.color.status_confirmed));
        STATUSES.put("non-case", new Status("non-case", R.string.status_non_case, R.drawable.square_non_case, R.drawable.round_non_case, R.color.status_non_case));
        STATUSES.put("convalescent", new Status("convalescent", R.string.status_convalescence, R.drawable.square_convalescence, R.drawable.round_convalescent, R.color.status_convalescent));
        STATUSES.put("can be discharged", new Status("can be discharged", R.string.status_can_be_discharged, R.drawable.square_can_be_discharged, R.drawable.round_can_be_discharged, R.color.status_can_be_discharged));
        STATUSES.put("discharged", new Status("discharged", R.string.status_discharged, R.drawable.square_discharged, R.drawable.round_discharged, R.color.status_discharged));
        STATUSES.put("unconfirmed-death", new Status("unconfirmed-death", R.string.status_unconfirmed_death, R.drawable.square_unconfirmed_death, R.drawable.round_deceased_unconfirmed, R.color.status_unconfirmed_death));
        STATUSES.put("confirmed-death", new Status("confirmed-death", R.string.status_confirmed_death, R.drawable.square_confirmed_death, R.drawable.round_deceased_confirmed, R.color.status_confirmed_death));
    }


    public static Status[] getStatus() {
        Object[] objects = STATUSES.values().toArray();
        return Arrays.copyOf(objects, objects.length, Status[].class);
    }

    public static Status getStatus(String statusKey){
        Status status = STATUSES.get(statusKey);
        if (status == null) {
            Log.e("Status", "Tried to get status for unknown: " + statusKey);
            return STATUSES.get("suspected");
        }
        return status;
    }

    @Override
    public int getObjectId() {
        return 0;
    }

    @Override
    public int getTitleId() {
        return nameId;
    }

    @Override
    public int getIconId() {
        return squareIconId;
    }

    @Override
    public int getFurtherDialogId() {
        return 0;
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
