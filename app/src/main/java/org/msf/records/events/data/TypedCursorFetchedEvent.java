package org.msf.records.events.data;


import org.msf.records.data.app.TypedCursor;
import org.msf.records.events.DefaultCrudEventBus;

/**
 * An event bus event indicating that a {@link TypedCursor} pointing to a list of items has been
 * fetched from the data store.
 *
 * <p>This event should only ever be posted on a {@link DefaultCrudEventBus}.
 */
public class TypedCursorFetchedEvent<T> {

	// TODO(rjlothian): Type erasure may cause problems for handling these events.
	// Consider creating a separate class for each T.

    public final TypedCursor<T> mCursor;

    public TypedCursorFetchedEvent(TypedCursor<T> cursor) {
        mCursor = cursor;
    }
}
