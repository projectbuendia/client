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

package org.projectbuendia.client.data.app.converters;

import android.database.Cursor;
import android.provider.BaseColumns;

import org.projectbuendia.client.data.app.AppPatient;
import org.projectbuendia.client.sync.providers.Contracts.Patients;
import org.projectbuendia.client.utils.Utils;

import javax.annotation.concurrent.Immutable;

/** An {@link AppTypeConverter} that converts {@link AppPatient}s. */
@Immutable
public class AppPatientConverter implements AppTypeConverter<AppPatient> {

    @Override
    public AppPatient fromCursor(Cursor cursor) {
        return AppPatient.builder()
                .setId(Utils.getString(cursor, Patients._ID))
                .setUuid(Utils.getString(cursor, Patients.UUID))
                .setGivenName(Utils.getString(cursor, Patients.GIVEN_NAME))
                .setFamilyName(Utils.getString(cursor, Patients.FAMILY_NAME))
                .setBirthdate(Utils.getLocalDate(cursor, Patients.BIRTHDATE))
                .setGender(getGenderFromString(Utils.getString(cursor, Patients.GENDER)))
                .setLocationUuid(Utils.getString(cursor, Patients.LOCATION_UUID))
                .build();
    }

    private static int getGenderFromString(String genderString) {
        switch (genderString) {
            case "M":
                return AppPatient.GENDER_MALE;
            case "F":
                return AppPatient.GENDER_FEMALE;
            default:
                return AppPatient.GENDER_UNKNOWN;
        }
    }
}
