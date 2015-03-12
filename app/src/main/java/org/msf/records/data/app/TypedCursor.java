package org.msf.records.data.app;

import android.database.Cursor;
import android.net.Uri;

/**
 * A {@link Cursor}-like data structure that exposes a type-safe interface.
 *
 * <p>Implementations are most likely NOT thread-safe.
 *
 * @param <T> the type of the array elements
 */
public interface TypedCursor<T> extends Iterable<T>, AppModelObservable {

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
     * Returns the URI for which notifications are received.
     */
    Uri getNotificationUri();
}
