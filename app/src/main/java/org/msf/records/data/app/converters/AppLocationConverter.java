package org.msf.records.data.app.converters;

import android.database.Cursor;

import org.msf.records.data.app.AppLocation;
import org.msf.records.sync.providers.Contracts;

/**
 * A {@link AppTypeConverter} that converts {@link AppLocation}s.
 */
public class AppLocationConverter implements AppTypeConverter<AppLocation> {

    @Override
    public AppLocation fromCursor(Cursor cursor) {
        return new AppLocation(
                cursor.getString(cursor.getColumnIndex(Contracts.LocalizedLocations.LOCATION_UUID)),
                cursor.getString(cursor.getColumnIndex(Contracts.LocalizedLocations.PARENT_UUID)),
                cursor.getString(
                        cursor.getColumnIndex(Contracts.LocalizedLocations.LOCALIZED_NAME)),
                cursor.getInt(cursor.getColumnIndex(Contracts.LocalizedLocations.PATIENT_COUNT)));
    }
}
