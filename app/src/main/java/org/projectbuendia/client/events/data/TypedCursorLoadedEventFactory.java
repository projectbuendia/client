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

import org.projectbuendia.models.Patient;
import org.projectbuendia.models.TypedCursor;

/** A factory that creates instances of subclasses of {@link TypedCursorLoadedEvent}. */
public class TypedCursorLoadedEventFactory {

    /**
     * Creates a {@link TypedCursorLoadedEvent} for the specified data type and cursor.
     * @throws IllegalArgumentException if {@code clazz} is unknown
     */
    @SuppressWarnings("unchecked") // Types checked by code.
    public static <T> TypedCursorLoadedEvent<?> createEvent(
        Class<T> clazz,
        TypedCursor<T> cursor) {
        if (clazz.equals(Patient.class)) {
            return new AppPatientsLoadedEvent((TypedCursor<Patient>) cursor);
        } else {
            throw new IllegalArgumentException(
                "Unable to create an event for unknown type " + clazz.getName());
        }
    }

    private TypedCursorLoadedEventFactory() { }
}
