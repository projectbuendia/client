package org.msf.records.events.data;

/**
 * An event bus event indicating that a single item has been updated on the server, directly
 * providing both the original and new items for ease of comparison.
 */
public class SingleItemUpdatedEvent<T> {

    public final T originalItem;
    public final T newItem;

    public SingleItemUpdatedEvent(T originalItem, T newItem) {
        this.originalItem = originalItem;
        this.newItem = newItem;
    }
}
