package org.msf.records.data.app;

import android.database.ContentObserver;
import android.database.Cursor;

/**
 * A {@link Cursor}-like data structure that exposes a type-safe interface.
 *
 * <p>Subclasses of {@link TypedCursor} are most likely NOT thread-safe.
 *
 * @param <T> the type of the array elements
 */
public abstract class TypedCursor<T extends AppTypeBase> implements Iterable<T> {

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
     *
     * <p>Subsequent calls to {@code get} methods will return dummy values.
     */
    public abstract void close();

    public abstract void registerContentObserver(ContentObserver observer);

    public abstract void unregisterContentObserver(ContentObserver observer);
}
