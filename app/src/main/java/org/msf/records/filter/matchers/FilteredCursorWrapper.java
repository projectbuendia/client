package org.msf.records.filter.matchers;

import android.database.ContentObserver;
import android.net.Uri;

import org.msf.records.data.app.TypedCursor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Provides a wrapper over a {@link TypedCursor}, providing a filtered view of the cursor using
 * a specified {@link MatchingFilter}.
 */
public class FilteredCursorWrapper<T> implements TypedCursor<T> {
    private final TypedCursor<T> mCursor;
    private final List<Integer> mIndices;

    /**
     * Applies the given {@link MatchingFilter} to the given {@link TypedCursor}, given a particular
     * search term.
     */
    public FilteredCursorWrapper(
            TypedCursor<T> cursor, MatchingFilter<T> filter, CharSequence constraint) {
        mCursor = cursor;
        mIndices = new ArrayList<Integer>();

        int count = cursor.getCount();
        for (int i = 0; i < count; i++) {
            T obj = cursor.get(i);
            if (filter.matches(obj, constraint)) {
                mIndices.add(i);
            }
        }
    }

    @Override
    public int getCount() {
        return mIndices.size();
    }

    @Override
    public T get(int position) {
        return mCursor.get(mIndices.get(position));
    }

    @Override
    public Uri getNotificationUri() {
        return mCursor.getNotificationUri();
    }

    @Override
    public void registerContentObserver(ContentObserver observer) {
        mCursor.registerContentObserver(observer);
    }

    @Override
    public void unregisterContentObserver(ContentObserver observer) {
        mCursor.unregisterContentObserver(observer);
    }

    @Override
    public void close() {
        mCursor.close();
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private int mPosition = -1;

            @Override
            public boolean hasNext() {
                return mPosition + 1 < getCount();
            }

            @Override
            public T next() {
                mPosition++;
                return get(mPosition);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
