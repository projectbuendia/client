package org.msf.records.events.data;

/**
 * An event bus event indicating that the fetch of a single item failed.
 */
public class SingleItemFetchFailedEvent {

    public final String error;

    public SingleItemFetchFailedEvent(String errorMessage) {
        error = errorMessage;
    }
}
