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
