package org.msf.records.events.data;

/**
 * An event bus event indicating that a single item has been created on the server, directly
 * providing the item that was created.
 */
public class SingleItemCreatedEvent<T> {

    public final T item;

    public SingleItemCreatedEvent(T item) {
        this.item = item;
    }
}