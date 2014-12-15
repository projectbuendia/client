package org.msf.records.data.app;

import android.database.ContentObserver;
import android.database.Cursor;

/**
 * A {@link Cursor}-like data structure that exposes a type-safe interface.
 *
 * <p>Implementations are most likely NOT thread-safe.
 */
public interface TypedCursor<T> extends Iterable<T> {

    /**
     * Returns the number of items in this lazy array.
     */
    int getCount();

    /**
     * Returns the item at the specified position or {@code null} if the specified position is
     * invalid.
     */
    T get(int position);

    /**
     * Closes the {@link TypedCursor} and any backing types.
     *
     * <p>Subsequent calls to {@code get} methods may throw an exception.
     */
    void close();

    void registerContentObserver(ContentObserver observer);

    void unregisterContentObserver(ContentObserver observer);
}
