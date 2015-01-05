package org.msf.records.events.data;

import org.msf.records.data.app.AppLocation;
import org.msf.records.data.app.AppPatient;
import org.msf.records.data.app.AppUser;
import org.msf.records.data.app.TypedCursor;

/**
 * A factory that creates instances of subclasses of {@link TypedCursorFetchedEvent}.
 */
public class TypedCursorFetchedEventFactory {

    /**
     * Creates a {@link TypedCursorFetchedEvent} for the specified data type and cursor.
     *
     * @throws IllegalArgumentException if {@code clazz} is unknown
     */
    @SuppressWarnings("unchecked") // Types checked by code.
    public static <T> TypedCursorFetchedEvent<?> createEvent(
            Class<T> clazz,
            TypedCursor<T> cursor) {
        if (clazz.equals(AppPatient.class)) {
            return new AppPatientsFetchedEvent((TypedCursor<AppPatient>) cursor);
        } else if (clazz.equals(AppUser.class)) {
            return new AppUsersFetchedEvent((TypedCursor<AppUser>) cursor);
        } else if (clazz.equals(AppLocation.class)) {
            return new AppLocationsFetchedEvent((TypedCursor<AppLocation>) cursor);
        } else {
            throw new IllegalArgumentException(
                    "Unable to create an event for unknown type " + clazz.getName());
        }
    }

    private TypedCursorFetchedEventFactory() {}
}
