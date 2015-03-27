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

package org.msf.records.data.app.converters;

import android.database.Cursor;

import org.msf.records.data.app.AppLocation;
import org.msf.records.sync.providers.Contracts;

/** An {@link AppTypeConverter} that converts {@link AppLocation}s. */
public class AppLocationConverter implements AppTypeConverter<AppLocation> {

    @Override
    public AppLocation fromCursor(Cursor cursor) {
        return new AppLocation(
                cursor.getString(cursor.getColumnIndex(Contracts.LocalizedLocations.LOCATION_UUID)),
                cursor.getString(cursor.getColumnIndex(Contracts.LocalizedLocations.PARENT_UUID)),
                cursor.getString(
                        cursor.getColumnIndex(Contracts.LocalizedLocations.LOCALIZED_NAME)),
                cursor.getInt(cursor.getColumnIndex(Contracts.LocalizedLocations.PATIENT_COUNT)));
    }
}
