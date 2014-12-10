package org.msf.records.events.data;


import org.msf.records.data.app.AppTypeBase;
import org.msf.records.data.app.TypedCursor;
import org.msf.records.events.CrudEventBus;

/**
 * An event bus event indicating that a {@link TypedCursor} pointing to a list of items has been
 * fetched from the data store.
 *
 * <p>This event should only ever be posted on a {@link CrudEventBus}.
 */
public class TypedCursorFetchedEvent<T extends AppTypeBase> {

    public final TypedCursor<T> mCursor;

    public TypedCursorFetchedEvent(TypedCursor<T> cursor) {
        mCursor = cursor;
    }
}
