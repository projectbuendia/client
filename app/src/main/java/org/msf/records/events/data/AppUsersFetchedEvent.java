package org.msf.records.events.data;

import org.msf.records.data.app.AppUser;
import org.msf.records.data.app.TypedCursor;

/**
 * An event bus event indicating that a list of users has been fetched from the data store.
 */
public class AppUsersFetchedEvent extends TypedCursorFetchedEvent<AppUser> {

    AppUsersFetchedEvent(TypedCursor<AppUser> cursor) {
        super(cursor);
    }
}
