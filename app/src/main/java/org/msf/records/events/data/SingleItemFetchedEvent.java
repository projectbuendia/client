package org.msf.records.events.data;

import org.msf.records.data.app.TypedCursor;
import org.msf.records.events.CrudEventBus;

/**
 * An event bus event indicating that a single item has been fetched from the data store.
 *
 * <p>Unlike {@link TypedCursorFetchedEvent}, this class directly provides the fetched item (rather
 * than providing a {@link TypedCursor} to lazy-load fetched items).
 *
 * <p>This event should only ever be posted on a {@link CrudEventBus}.
 */
public class SingleItemFetchedEvent<T> {

    public final T mItem;

    public SingleItemFetchedEvent(T item) {
        mItem = item;
    }
}
