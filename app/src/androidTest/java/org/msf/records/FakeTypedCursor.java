package org.msf.records;

import android.database.ContentObserver;
import android.net.Uri;

import com.google.common.collect.Iterators;

import org.msf.records.data.app.TypedCursor;

import java.util.Iterator;

/**
 * A {@link TypedCursor} that takes its contents as input for ease of testing.
 */
public class FakeTypedCursor<T> implements TypedCursor<T> {
    private final T[] mObjects;
    private boolean mIsClosed = false;

    /**
     * Creates a {@link FakeTypedCursor} that contains the specified objects.
     *
     * @param objects the contents of the cursor
     */
    public FakeTypedCursor(T... objects) {
        mObjects = objects;
    }

    @Override
    public int getCount() {
        return mObjects.length;
    }

    @Override
    public T get(int position) {
        return mObjects[position];
    }

    /**
     * Returns {@code null}.
     */
    @Override
    public Uri getNotificationUri() {
        return null;
    }

    /**
     * No-op.
     */
    @Override
    public void registerContentObserver(ContentObserver observer) {}

    /**
     * No-op.
     */
    @Override
    public void unregisterContentObserver(ContentObserver observer) {}

    @Override
    public void close() {
        mIsClosed = true;
    }

    @Override
    public Iterator<T> iterator() {
        return Iterators.forArray(mObjects);
    }

    public boolean isClosed() {
        return mIsClosed;
    }
}
