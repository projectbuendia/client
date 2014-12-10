package org.msf.records.data;

import android.database.Cursor;
import android.database.DataSetObservable;
import android.database.DataSetObserver;

/**
 * A {@link Cursor}-like data structure that exposes a type-safe interface.
 *
 * <p>Subclasses of {@link TypedCursor} are most likely NOT thread-safe.
 *
 * @param <T> the type of the array elements
 */
public abstract class TypedCursor<T extends BaseModel> implements Iterable<T> {

    /**
     * Returns the number of items in this lazy array.
     */
    public abstract int getCount();

    /**
     * Returns the item at the specified position or {@code null} if the specified position is
     * invalid.
     */
    public abstract T get(int position);

    /**
     * Closes the {@link TypedCursor} and any backing types.
     */
    public abstract void close();

    public abstract void registerDataSetObserver(DataSetObserver observer);

    public abstract void unregisterDataSetObserver(DataSetObserver observer);
}
