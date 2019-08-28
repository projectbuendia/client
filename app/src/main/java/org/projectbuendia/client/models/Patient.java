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

package org.projectbuendia.client.models;

import android.content.ContentValues;
import android.database.Cursor;

import org.joda.time.LocalDate;
import org.projectbuendia.client.json.JsonPatient;
import org.projectbuendia.client.providers.Contracts.Patients;
import org.projectbuendia.client.utils.Utils;

import javax.annotation.concurrent.Immutable;

public final @Immutable class Patient extends Model {
    public final String id;
    public final String givenName;
    public final String familyName;
    public final Sex sex;
    public final LocalDate birthdate;

    // The fields below are denormalized from observations, not received
    // as part of the patient model.
    public final boolean pregnancy;
    public final String locationUuid;
    public final String bedNumber;

    /** Creates an instance of {@link Patient} from a network {@link JsonPatient} object. */
    public static Patient fromJson(JsonPatient patient) {
        return new Patient(
            patient.uuid, patient.id, patient.given_name, patient.family_name,
            patient.sex, patient.birthdate, false /* pregnancy */,
            "" /* locationUuid */, "" /* bedNumber */);
    }

    /** Puts this object's fields in a {@link ContentValues} object for insertion into a database. */
    public ContentValues toContentValues() {
        ContentValues cv = new ContentValues();
        cv.put(Patients.UUID, uuid);
        cv.put(Patients.ID, id);
        cv.put(Patients.GIVEN_NAME, givenName);
        cv.put(Patients.FAMILY_NAME, familyName);
        cv.put(Patients.SEX, Sex.nullableNameOf(sex));
        cv.put(Patients.BIRTHDATE, Utils.formatDate(birthdate));
        // PREGNANCY is a denormalized column and is never written directly.
        // LOCATION_UUID is a denormalized column and is never written directly.
        // BED_NUMBER is a denormalized column and is never written directly.
        return cv;
    }

    public org.odk.collect.android.model.Patient toOdkPatient() {
        return new org.odk.collect.android.model.Patient(
            uuid, id, givenName, familyName);
    }

    public Patient(String uuid, String id, String givenName, String familyName,
                   Sex sex, LocalDate birthdate, boolean pregnancy,
                   String locationUuid, String bedNumber) {
        super(uuid);
        this.id = Utils.toNonnull(id);
        this.givenName = Utils.toNonnull(givenName);
        this.familyName = Utils.toNonnull(familyName);
        this.sex = sex;
        this.birthdate = birthdate;
        this.pregnancy = pregnancy;
        this.locationUuid = Utils.toNonnull(locationUuid);
        this.bedNumber = Utils.toNonnull(bedNumber);
    }

    public static Patient load(Cursor cursor) {
        return new Patient(
            Utils.getString(cursor, Patients.UUID),
            Utils.getString(cursor, Patients.ID),
            Utils.getString(cursor, Patients.GIVEN_NAME),
            Utils.getString(cursor, Patients.FAMILY_NAME),
            Sex.nullableValueOf(Utils.getString(cursor, Patients.SEX)),
            Utils.getLocalDate(cursor, Patients.BIRTHDATE),
            Utils.getBoolean(cursor, Patients.PREGNANCY, false),
            Utils.getString(cursor, Patients.LOCATION_UUID),
            Utils.getString(cursor, Patients.BED_NUMBER)
        );
    }
}
