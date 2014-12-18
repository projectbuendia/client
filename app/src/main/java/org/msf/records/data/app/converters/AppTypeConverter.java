package org.msf.records.data.app.converters;

import android.database.Cursor;

/**
 * An interface for a converter that converts between model data types and Android database
 * abstractions.
 */
public interface AppTypeConverter<T> {

    /**
     * Converts the current position in a {@link Cursor} to a model data type.
     */
    T fromCursor(Cursor cursor);
}
