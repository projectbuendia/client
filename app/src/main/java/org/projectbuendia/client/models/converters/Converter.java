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

package org.projectbuendia.client.models.converters;

import android.database.Cursor;

/**
 * An interface for a converter that converts a model data type and a corresponding Android database
 * abstraction.
 */
// TODO: This doesn't really convert anything; it actually constructs a model object using
// content values from a cursor.  Rename this to a less confusing name like CursorLoader and
// make it an inner class of each corresponding model type.
public interface Converter<T> {

    /** Converts the current position in a {@link Cursor} to a model data type. */
    T fromCursor(Cursor cursor);
}
