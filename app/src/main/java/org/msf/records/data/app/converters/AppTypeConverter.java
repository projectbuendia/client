package org.msf.records.data.app.converters;

import android.database.Cursor;

import org.msf.records.data.app.AppTypeBase;

/**
 * An interface for a converter that converts between model data types and Android database
 * abstractions.
 */
public interface AppTypeConverter<T extends AppTypeBase> {

    /**
     * Converts the current position in a {@link Cursor} to a model data type.
     */
    T fromCursor(Cursor cursor);
}
