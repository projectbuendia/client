package org.msf.records.data;

import android.database.Cursor;
import android.database.DataSetObserver;
import android.util.SparseArray;

import java.util.Iterator;

/**
 * A {@link TypedCursor} that's backed by a {@link ModelConverter} and a {@link Cursor}.
 *
 * <p>This data structure is NOT thread-safe. It should only be accessed from one thread, generally
 * the main thread. Furthermore, only one {@link Iterator} should be created on it at a time.
 *
 * <p>This data structure does NOT notify anyone when the data set changes (i.e., it does not
 * provide a mechanism to access {@link Cursor#registerDataSetObserver}). This is because the
 * associated {@link Cursor#requery} and {@link Cursor#deactivate} methods have been deprecated. It
 * does, however, pass along data set changes
 */
class TypedConvertedCursor<T extends BaseModel, U extends ModelConverter<T>>
        extends TypedCursor<T> {

    private final U mConverter;
    private final Cursor mCursor;

    private final SparseArray<T> mConvertedItems;

    private boolean mIsClosed;

    public TypedConvertedCursor(U converter, Cursor cursor) {
        mConverter = converter;
        mCursor = cursor;

        mConvertedItems = new SparseArray<>();
    }

    /**
     * {@inheritDoc}
     *
     * <p>If the backing {@link Cursor} is a database cursor, calling this method will be expensive.
     * Wherever possible, prefer to iterate.
     */
    public int getCount() {
        return mCursor.getCount();
    }

    public T get(int position) {
        T convertedItem = mConvertedItems.get(position);
        if (convertedItem == null) {
            int originalPosition = mCursor.getPosition();

            if (!mCursor.moveToPosition(position)) {
                return null;
            }

            convertedItem = mConverter.convert(mCursor);
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
    public void registerDataSetObserver(DataSetObserver observer) {
        mCursor.registerDataSetObserver(observer);
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        mCursor.unregisterDataSetObserver(observer);
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
                convertedItem = mConverter.convert(mCursor);
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
