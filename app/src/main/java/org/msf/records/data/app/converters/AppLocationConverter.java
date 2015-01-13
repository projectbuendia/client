package org.msf.records.data.app.converters;

import android.database.Cursor;

import org.msf.records.data.app.AppLocation;
import org.msf.records.sync.LocationProjection;

/**
 * A {@link AppTypeConverter} that converts {@link AppLocation}s.
 */
public class AppLocationConverter implements AppTypeConverter<AppLocation> {

    @Override
    public AppLocation fromCursor(Cursor cursor) {
        // TODO(dxchen): Refactor LocationProjection into Contracts to avoid magic value pollution.
        return new AppLocation(
                cursor.getString(LocationProjection.LOCATION_LOCATION_UUID_COLUMN),
                cursor.getString(LocationProjection.LOCATION_PARENT_UUID_COLUMN),
                cursor.getString(2)); // The localized location name.
    }
}
