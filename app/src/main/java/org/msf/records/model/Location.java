package org.msf.records.model;

import android.os.Parcel;
import android.os.Parcelable;

import org.msf.records.R;

/**
 * Created by danieljulio on 09/10/2014.
 */
public class Location implements Parcelable, ListItem{

    public int location;
    public int subLocation;

    public Location(int location, int subLocation) {
        this.location = location;
        this.subLocation = subLocation;
    }

    public Location(Parcel in) {
        location = in.readInt();
        subLocation = in.readInt();
    }

    public static Location[] getLocation() {
        Location[] locations = new Location[7];
        locations[0] = new Location(R.string.location_all_zones, 0);
        locations[1] = new Location(R.string.location_triage, 0);
        locations[2] = new Location(R.string.location_suspected_zone, 1);
        locations[3] = new Location(R.string.location_probable_zone, 1);
        locations[4] = new Location(R.string.location_confirmed_zone, 1);
        locations[5] = new Location(R.string.location_mortuary, 0);
        locations[6] = new Location(R.string.location_outside, 0);
        return locations;
    }


    public static Location[] getLocationWithoutAll() {
        Location[] locations = new Location[6];
        locations[0] = new Location(R.string.location_triage, 0);
        locations[1] = new Location(R.string.location_suspected_zone, 1);
        locations[2] = new Location(R.string.location_probable_zone, 1);
        locations[3] = new Location(R.string.location_confirmed_zone, 1);
        locations[4] = new Location(R.string.location_mortuary, 0);
        locations[5] = new Location(R.string.location_outside, 0);
        return locations;
    }

    @Override
    public int getObjectId() {
        return 1;
    }

    @Override
    public int getTitleId() {
        return location;
    }

    @Override
    public int getIconId() {
        return 0;
    }

    @Override
    public int getFurtherDialogId() {
        return subLocation;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(location);
        out.writeInt(subLocation);

    }

    public static final Parcelable.Creator<Location> CREATOR = new Parcelable.Creator<Location>() {
        public Location createFromParcel(Parcel in) {
            return new Location(in);
        }

        @Override
        public Location[] newArray(int size) {
            return new Location[size];
        }
    };
}