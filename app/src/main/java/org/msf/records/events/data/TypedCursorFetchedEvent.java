package org.msf.records.events.data;

import org.msf.records.data.app.TypedCursor;
import org.msf.records.events.DefaultCrudEventBus;

/**
 * An event bus event indicating that a {@link TypedCursor} pointing to a list of items has been
 * fetched from the data store.
 *
 * <p>This event should only ever be posted on a {@link DefaultCrudEventBus}.
 */
// TODO(rjlothian): Java type erasure will cause problems for using parameterized
// event types with event bus. To work around this, let's create named (sub)classes for
// each type T that we use.
public class TypedCursorFetchedEvent<T> {

    public final TypedCursor<T> cursor;

    public TypedCursorFetchedEvent(TypedCursor<T> cursor) {
        this.cursor = cursor;
    }
}
