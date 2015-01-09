package org.msf.records.data.app.converters;

import android.database.Cursor;

import org.joda.time.DateTime;
import org.msf.records.data.app.AppLocation;
import org.msf.records.data.app.AppPatient;
import org.msf.records.sync.LocationProjection;
import org.msf.records.sync.PatientProjection;
import org.msf.records.utils.Utils;

/**
 * A {@link AppTypeConverter} that converts {@link AppLocation}s.
 */
public class AppLocationConverter implements AppTypeConverter<AppLocation> {

    private static final String TAG = AppLocationConverter.class.getSimpleName();

    @Override
    public AppLocation fromCursor(Cursor cursor) {
        return new AppLocation(
                cursor.getString(LocationProjection.LOCATION_LOCATION_UUID_COLUMN),
                cursor.getString(LocationProjection.LOCATION_PARENT_UUID_COLUMN),
                cursor.getString(2));
    }
}
