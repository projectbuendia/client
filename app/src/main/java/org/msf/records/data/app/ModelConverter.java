package org.msf.records.data.app;

import android.database.Cursor;

/**
 * An interface for a converter that converts between model data types and Android database
 * abstractions.
 */
public interface ModelConverter<T extends ModelTypeBase> {

    /**
     * Converts the current position in a {@link Cursor} to a model data type.
     */
    T convert(Cursor cursor);
}
