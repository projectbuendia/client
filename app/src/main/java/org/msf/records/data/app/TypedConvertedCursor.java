package org.msf.records.data.app;

import android.database.ContentObserver;
import android.database.Cursor;
import android.util.SparseArray;

import org.msf.records.data.app.converters.AppTypeConverter;

import java.util.Iterator;

/**
 * A {@link TypedCursor} that's backed by a {@link AppTypeConverter} and a {@link Cursor}.
 *
 * <p>This data structure is NOT thread-safe. It should only be accessed from one thread at a time,
 * generally the main thread. Furthermore, only one {@link Iterator} should be created on it at a
 * time.
 *
 * <p>This data structure does NOT notify anyone when the data set changes (i.e., it does not
 * provide a mechanism to access {@link Cursor#registerDataSetObserver}). This is because the
 * associated {@link Cursor#requery} and {@link Cursor#deactivate} methods have been deprecated. It
 * does, however, pass along {@link ContentObserver} callbacks.
 */
class TypedConvertedCursor<T, U extends AppTypeConverter<T>> implements TypedCursor<T> {

    private final U mConverter;
    private final Cursor mCursor;

    private final SparseArray<T> mConvertedItems;

    private boolean mIsClosed;

    public TypedConvertedCursor(U converter, Cursor cursor) {
        mConverter = converter;
        mCursor = cursor;
        mIsClosed = mCursor.isClosed();

        mConvertedItems = new SparseArray<>();
    }

    /**
     * {@inheritDoc}
     *
     * <p>If the backing {@link Cursor} is a database cursor, calling this method may be expensive.
     * Wherever possible, prefer to iterate.
     */
    @Override
    public int getCount() {
        if (mIsClosed) {
            return 0;
        }

        return mCursor.getCount();
    }

    @Override
    public T get(int position) {
        if (mIsClosed) {
            return null;
        }

        T convertedItem = mConvertedItems.get(position);
        if (convertedItem == null) {
            int originalPosition = mCursor.getPosition();

            if (!mCursor.moveToPosition(position)) {
                return null;
            }

            convertedItem = mConverter.fromCursor(mCursor);
            mCursor.moveToPosition(originalPosition);

            mConvertedItems.put(position, convertedItem);
        }

        return convertedItem;
    }

    @Override
    public Iterator<T> iterator() {
        return new LazyConverterIterator();
    }

    @Override
    public void close() {
        mCursor.close();

        mIsClosed = true;
    }

    @Override
    public void registerContentObserver(ContentObserver observer) {
        mCursor.registerContentObserver(observer);
    }

    @Override
    public void unregisterContentObserver(ContentObserver observer) {
        mCursor.unregisterContentObserver(observer);
    }

    private class LazyConverterIterator implements Iterator<T> {

        @Override
        public boolean hasNext() {
            return !mCursor.isLast() && !mCursor.isAfterLast();
        }

        @Override
        public T next() {
            if (!mCursor.moveToNext()) {
                throw new IllegalStateException("Cannot move cursor past its last entry.");
            }
            int position = mCursor.getPosition();
            T convertedItem = mConvertedItems.get(position);
            if (convertedItem == null) {
                convertedItem = mConverter.fromCursor(mCursor);
            }

            return convertedItem;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException(
                    "Elements cannot be removed from an iterator backed by a cursor.");
        }
    }
}
