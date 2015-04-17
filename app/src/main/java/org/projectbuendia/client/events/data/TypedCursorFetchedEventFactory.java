// Copyright 2015 The Project Buendia Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at: http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distrib-
// uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
// specific language governing permissions and limitations under the License.

package org.projectbuendia.client.events.data;

import org.projectbuendia.client.data.app.AppLocation;
import org.projectbuendia.client.data.app.AppPatient;
import org.projectbuendia.client.data.app.AppUser;
import org.projectbuendia.client.data.app.TypedCursor;

/** A factory that creates instances of subclasses of {@link TypedCursorFetchedEvent}. */
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
