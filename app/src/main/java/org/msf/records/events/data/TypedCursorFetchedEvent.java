package org.msf.records.events.data;

import org.msf.records.data.app.TypedCursor;
import org.msf.records.events.DefaultCrudEventBus;

/**
 * An abstract event bus event indicating that a {@link TypedCursor} pointing to a list of items has
 * been fetched from the data store.
 *
 * <p>Subclasses of this event should only ever be posted on a {@link DefaultCrudEventBus}.
 */
public abstract class TypedCursorFetchedEvent<T> {

    public final TypedCursor<T> cursor;

    TypedCursorFetchedEvent(TypedCursor<T> cursor) {
        this.cursor = cursor;
    }
}
