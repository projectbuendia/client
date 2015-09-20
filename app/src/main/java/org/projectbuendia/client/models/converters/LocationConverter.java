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

import org.projectbuendia.client.models.Location;
import org.projectbuendia.client.providers.Contracts;
import org.projectbuendia.client.utils.Utils;

/** An {@link Converter} that converts {@link Location}s. */
public class LocationConverter implements Converter<Location> {

    @Override public Location fromCursor(Cursor cursor) {
        return new Location(
            Utils.getString(cursor, Contracts.LocalizedLocations.UUID),
            Utils.getString(cursor, Contracts.LocalizedLocations.PARENT_UUID),
            Utils.getString(cursor, Contracts.LocalizedLocations.NAME),
            Utils.getLong(cursor, Contracts.LocalizedLocations.PATIENT_COUNT));
    }
}
