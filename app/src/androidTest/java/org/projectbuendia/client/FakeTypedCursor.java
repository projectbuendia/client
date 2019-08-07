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

package org.projectbuendia.client;

import com.google.common.collect.Iterators;

import org.projectbuendia.client.models.TypedCursor;

import java.util.Iterator;

/** A {@link TypedCursor} that takes its contents as input for ease of testing. */
public class FakeTypedCursor<T> implements TypedCursor<T> {
    private final T[] mObjects;
    private boolean mIsClosed = false;

    /**
     * Creates a {@link FakeTypedCursor} that contains the specified objects.
     * @param objects the contents of the cursor
     */
    public FakeTypedCursor(T... objects) {
        mObjects = objects;
    }

    @Override public int getCount() {
        return mObjects.length;
    }

    @Override public T get(int position) {
        return mObjects[position];
    }

    @Override public void close() {
        mIsClosed = true;
    }

    @Override public Iterator<T> iterator() {
        return Iterators.forArray(mObjects);
    }

    public boolean isClosed() {
        return mIsClosed;
    }
}
