package org.msf.records.events.data;

import org.msf.records.data.app.AppLocationTree;

/**
 * An event bus event indicating that a location tree has been fetched from the data store.
 */
public class AppLocationTreeFetchedEvent {

    public final AppLocationTree tree;

    public AppLocationTreeFetchedEvent(AppLocationTree tree) {
        this.tree = tree;
    }
}
