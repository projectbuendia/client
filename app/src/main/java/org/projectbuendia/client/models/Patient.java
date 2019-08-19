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

public final @Immutable class Patient extends Base<String> implements Comparable<Patient> {
    public static final CursorLoader<Patient> LOADER = cursor -> builder()
        .setUuid(Utils.getString(cursor, Contracts.Patients.UUID))
        .setId(Utils.getString(cursor, Contracts.Patients.ID))
        .setGivenName(Utils.getString(cursor, Contracts.Patients.GIVEN_NAME))
        .setFamilyName(Utils.getString(cursor, Contracts.Patients.FAMILY_NAME))
        .setBirthdate(Utils.getLocalDate(cursor, Contracts.Patients.BIRTHDATE))
        .setSex(Sex.forCode(Utils.getString(cursor, Contracts.Patients.SEX)))
        .setLocationUuid(Utils.getString(cursor, Contracts.Patients.LOCATION_UUID))
        .build();

    public final String uuid;
    public final String givenName;
    public final String familyName;
    public final Sex sex;
    // TODO: Make PatientDelta.birthdate and Patient.birthdate same type (LocalDate or DateTime).
    public final LocalDate birthdate;
    public final String locationUuid;

    public static Builder builder() {
        return new Builder();
    }

    /** Creates an instance of {@link Patient} from a network {@link JsonPatient} object. */
    public static Patient fromJson(JsonPatient patient) {
        return builder()
            .setId(patient.id)
            .setUuid(patient.uuid)
            .setGivenName(patient.given_name)
            .setFamilyName(patient.family_name)
            .setSex(Sex.forCode(patient.sex))
            .setBirthdate(patient.birthdate)
            .setLocationUuid(
                patient.assigned_location != null ? patient.assigned_location.uuid : null)
            .build();
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

    public static final class Builder {
        private String mId;
        private String mUuid;
        private String mGivenName;
        private String mFamilyName;
        private Sex mSex;
        private LocalDate mBirthdate;
        private String mLocationUuid;

        public Builder setId(String id) {
            this.mId = id;
            return this;
        }

        public Builder setUuid(String uuid) {
            this.mUuid = uuid;
            return this;
        }

        public Builder setGivenName(String givenName) {
            this.mGivenName = givenName;
            return this;
        }

        public Builder setFamilyName(String familyName) {
            this.mFamilyName = familyName;
            return this;
        }

        public Builder setSex(Sex sex) {
            this.mSex = sex;
            return this;
        }

        public Builder setBirthdate(LocalDate birthdate) {
            this.mBirthdate = birthdate;
            return this;
        }

        public Builder setLocationUuid(String locationUuid) {
            this.mLocationUuid = locationUuid;
            return this;
        }

        public Patient build() {
            return new Patient(this);
        }

        private Builder() {
        }
    }

    private Patient(Builder builder) {
        super(builder.mId);
        this.uuid = builder.mUuid;
        this.givenName = builder.mGivenName;
        this.familyName = builder.mFamilyName;
        this.sex = builder.mSex;
        this.birthdate = builder.mBirthdate;
        this.locationUuid = builder.mLocationUuid;
    }
}
