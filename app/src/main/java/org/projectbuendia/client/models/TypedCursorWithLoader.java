// Copyright 2015 The Project Buendia Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at: http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distrib-
// uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
// specific language governing permissions and limitations under the License.

package org.projectbuendia.client.models;

import android.database.ContentObserver;
import android.database.Cursor;
import android.util.SparseArray;

import java.util.Iterator;

/**
 * A {@link TypedCursor} that uses a {@link CursorLoader} to load items from a {@link Cursor}.
 * <p/>
 * <p>This data structure is NOT thread-safe. It should only be accessed from one thread at a time,
 * generally the main thread. Furthermore, only one {@link Iterator} should be created on it at a
 * time.
 * <p/>
 * <p>This data structure does NOT notify anyone when the data set changes (i.e., it does not
 * provide a mechanism to access {@link Cursor#registerDataSetObserver}). This is because the
 * associated {@link Cursor#requery} and {@link Cursor#deactivate} methods have been deprecated. It
 * does, however, pass along {@link ContentObserver} callbacks.
 */
public class TypedCursorWithLoader<T> implements TypedCursor<T> {
    private final CursorLoader<T> mLoader;
    private final Cursor mCursor;

    private final SparseArray<T> mLoadedItems;

    public TypedCursorWithLoader(Cursor cursor, CursorLoader<T> loader) {
        mLoader = loader;
        mCursor = cursor;
        mLoadedItems = new SparseArray<>();
    }

    /**
     * {@inheritDoc}
     * <p/>
     * <p>If the backing {@link Cursor} is a database cursor, calling this method may be expensive.
     * Wherever possible, prefer to iterate.
     */
    @Override public int getCount() {
        if (mCursor.isClosed()) {
            return 0;
        }

        return mCursor.getCount();
    }

    @Override public T get(int position) {
        if (mCursor.isClosed()) {
            return null;
        }

        T convertedItem = mLoadedItems.get(position);
        if (convertedItem == null) {
            int originalPosition = mCursor.getPosition();

            if (!mCursor.moveToPosition(position)) {
                return null;
            }

            convertedItem = mLoader.fromCursor(mCursor);
            mCursor.moveToPosition(originalPosition);

            mLoadedItems.put(position, convertedItem);
        }

        return convertedItem;
    }

    @Override public Iterator<T> iterator() {
        return new LazyLoaderIterator();
    }

    @Override public void close() {
        mCursor.close();
    }

    @Override public void registerContentObserver(ContentObserver observer) {
        mCursor.registerContentObserver(observer);
    }

    @Override public void unregisterContentObserver(ContentObserver observer) {
        mCursor.unregisterContentObserver(observer);
    }

    private class LazyLoaderIterator implements Iterator<T> {

        @Override public boolean hasNext() {
            return !mCursor.isLast() && !mCursor.isAfterLast();
        }

        @Override public T next() {
            if (!mCursor.moveToNext()) {
                throw new IllegalStateException("Cannot move cursor past its last entry.");
            }

            int position = mCursor.getPosition();

            T loadedItem = mLoadedItems.get(position);
            if (loadedItem == null) {
                loadedItem = mLoader.fromCursor(mCursor);
                mLoadedItems.put(position, loadedItem);
            }

            return loadedItem;
        }

        @Override public void remove() {
            throw new UnsupportedOperationException(
                "Elements cannot be removed from an iterator backed by a cursor.");
        }
    }
}
