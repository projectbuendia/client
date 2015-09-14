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

package org.projectbuendia.client.filter.matchers;

import android.database.ContentObserver;
import android.net.Uri;

import org.projectbuendia.client.models.TypedCursor;

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

    @Override public Uri getNotificationUri() {
        return mCursor.getNotificationUri();
    }

    @Override public void registerContentObserver(ContentObserver observer) {
        mCursor.registerContentObserver(observer);
    }

    @Override public void unregisterContentObserver(ContentObserver observer) {
        mCursor.unregisterContentObserver(observer);
    }

    @Override public void close() {
        mCursor.close();
    }

    @Override public Iterator<T> iterator() {
        return new Iterator<T>() {
            private int mPosition = -1;

            @Override public boolean hasNext() {
                return mPosition + 1 < getCount();
            }

            @Override public T next() {
                mPosition++;
                return get(mPosition);
            }

            @Override public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override public int getCount() {
        return mIndices.size();
    }

    @Override public T get(int position) {
        return mCursor.get(mIndices.get(position));
    }
}
