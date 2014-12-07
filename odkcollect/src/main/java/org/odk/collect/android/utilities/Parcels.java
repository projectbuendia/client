package org.odk.collect.android.utilities;

import android.os.Parcel;

import org.joda.time.DateTime;

/**
 * Utilities for {@link Parcel}s.
 */
public class Parcels {

    /**
     * Reads a possibly-{@code null} string from a {@link Parcel}
     */
    public static String readNullableString(Parcel source) {
        return (source.readByte() == 0) ? null : source.readString();
    }

    /**
     * Writes a possibly-{@code null} string to a {@link Parcel}.
     */
    public static void writeNullableString(Parcel dest, String string) {
        if (string == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeString(string);
        }
    }

    /**
     * Reads a possibly-{@code null} {@link DateTime} from a {@link Parcel}
     */
    public static DateTime readNullableDateTime(Parcel source) {
        return (source.readByte() == 0) ? null : new DateTime(source.readLong());
    }

    /**
     * Writes a possibly-{@code null} {@link DateTime} to a {@link Parcel}.
     */
    public static void writeNullableDateTime(Parcel dest, DateTime dateTime) {
        if (dateTime == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeLong(dateTime.getMillis());
        }
    }

    private Parcels() {}
}
