package org.msf.records.events.data;

import org.msf.records.data.app.AppLocation;
import org.msf.records.data.app.AppUser;
import org.msf.records.data.app.TypedCursor;

/**
 * An event bus event indicating that a list of locations has been fetched from the data store.
 */
public class AppLocationsFetchedEvent extends TypedCursorFetchedEvent<AppLocation> {

    AppLocationsFetchedEvent(TypedCursor<AppLocation> cursor) {
        super(cursor);
    }
}

