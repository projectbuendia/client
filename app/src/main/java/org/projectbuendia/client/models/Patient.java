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

import org.joda.time.LocalDate;
import org.projectbuendia.client.json.JsonPatient;
import org.projectbuendia.client.providers.Contracts;
import org.projectbuendia.client.utils.Utils;

import javax.annotation.concurrent.Immutable;

public final @Immutable class Patient extends Model implements Comparable<Patient> {
    public static final CursorLoader<Patient> LOADER = cursor -> new Patient(
        Utils.getString(cursor, Contracts.Patients.UUID),
        Utils.getString(cursor, Contracts.Patients.ID),
        Utils.getString(cursor, Contracts.Patients.GIVEN_NAME),
        Utils.getString(cursor, Contracts.Patients.FAMILY_NAME),
        Sex.forCode(Utils.getString(cursor, Contracts.Patients.SEX)),
        Utils.getLocalDate(cursor, Contracts.Patients.BIRTHDATE),
        Utils.getString(cursor, Contracts.Patients.LOCATION_UUID)
    );

    public final String id;
    public final String givenName;
    public final String familyName;
    public final Sex sex;
    // TODO: Make PatientDelta.birthdate and Patient.birthdate same type (LocalDate or DateTime).
    public final LocalDate birthdate;
    public final String locationUuid;

    /** Creates an instance of {@link Patient} from a network {@link JsonPatient} object. */
    public static Patient fromJson(JsonPatient patient) {
        return new Patient(
            patient.uuid, patient.id, patient.given_name, patient.family_name,
            Sex.forCode(patient.sex), patient.birthdate,
            patient.assigned_location != null ? patient.assigned_location.uuid : null
        );
    }

    @Override public int compareTo(Patient other) {
        return Utils.ALPHANUMERIC_COMPARATOR.compare(id, other.id);
    }

    /** Puts this object's fields in a {@link ContentValues} object for insertion into a database. */
    public ContentValues toContentValues() {
        ContentValues cv = new ContentValues();
        cv.put(Contracts.Patients.UUID, uuid);
        cv.put(Contracts.Patients.ID, id);
        cv.put(Contracts.Patients.GIVEN_NAME, givenName);
        cv.put(Contracts.Patients.FAMILY_NAME, familyName);
        cv.put(Contracts.Patients.SEX, sex.code);
        cv.put(Contracts.Patients.BIRTHDATE, Utils.formatDate(birthdate));
        cv.put(Contracts.Patients.LOCATION_UUID, locationUuid);
        return cv;
    }

    public org.odk.collect.android.model.Patient toOdkPatient() {
        return new org.odk.collect.android.model.Patient(
            uuid, id, givenName, familyName);
    }

    public Patient(String uuid, String id, String givenName, String familyName,
                   Sex sex, LocalDate birthdate, String locationUuid) {
        super(uuid);
        this.id = id;
        this.givenName = givenName;
        this.familyName = familyName;
        this.sex = sex;
        this.birthdate = birthdate;
        this.locationUuid = locationUuid;
    }
}
