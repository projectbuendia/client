package org.msf.records.events.data;

import org.msf.records.data.app.AppPatient;
import org.msf.records.data.app.TypedCursor;

/**
 * An event bus event indicating that a list of patients has been fetched from the data store.
 */
public class AppPatientsFetchedEvent extends TypedCursorFetchedEvent<AppPatient> {

    AppPatientsFetchedEvent(TypedCursor<AppPatient> cursor) {
        super(cursor);
    }
}

