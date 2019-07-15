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

package org.projectbuendia.client.events;

import org.projectbuendia.client.events.data.AppLocationTreeFetchedEvent;
import org.projectbuendia.client.events.data.TypedCursorFetchedEvent;

import de.greenrobot.event.EventBus;
import de.greenrobot.event.NoSubscriberEvent;

/** An {@link EventBus} wrapper to be used by asynchronous CRUD operations on the data model. */
public final class DefaultCrudEventBus implements CrudEventBus {
    private final EventBus mBus;

    /** Creates a new {@link DefaultCrudEventBus} that wraps the specified {@link EventBus}. */
    DefaultCrudEventBus(EventBus bus) {
        mBus = bus;
        bus.register(this);
    }

    @Override public void register(Object subscriber) {
        mBus.register(subscriber);
    }

    @Override public void unregister(Object subscriber) {
        mBus.unregister(subscriber);
    }

    @Override public void post(Object event) {
        mBus.post(event);
    }

    @SuppressWarnings("unused") // Called by reflection from event bus.
    public void onEvent(NoSubscriberEvent event) {
        if (event.originalEvent instanceof TypedCursorFetchedEvent<?>) {
            // If no subscribers were registered for a DataFetchedEvent, then the TypedCursor in
            // the event won't be managed by anyone else; therefore, we close it ourselves.
            ((TypedCursorFetchedEvent<?>) event.originalEvent).cursor.close();
        } else if (event.originalEvent instanceof AppLocationTreeFetchedEvent) {
            ((AppLocationTreeFetchedEvent) event.originalEvent).tree.close();
        }
    }
}
