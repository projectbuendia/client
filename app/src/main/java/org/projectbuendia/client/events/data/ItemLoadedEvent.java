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

package org.projectbuendia.client.events.data;

import org.projectbuendia.client.events.DefaultCrudEventBus;
import org.projectbuendia.client.models.TypedCursor;

/**
 * Indicates that a single item has been fetched from the data store.
 * Should only be posted on a {@link DefaultCrudEventBus}.
 * <p/>
 * <p>Unlike {@link TypedCursorLoadedEvent}, this provides the fetched item
 * (rather than providing a {@link TypedCursor} to lazy-load fetched items).
 */
public class ItemLoadedEvent<T> {
    public final T item;

    public ItemLoadedEvent(T item) {
        this.item = item;
    }
}
