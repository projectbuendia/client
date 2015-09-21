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
import org.projectbuendia.client.providers.Contracts;
import org.projectbuendia.client.utils.Utils;

import javax.annotation.concurrent.Immutable;

@Immutable
public final class Patient extends Base<String> implements Comparable<Patient> {
    public static final int GENDER_UNKNOWN = 0;
    public static final int GENDER_MALE = 1;
    public static final int GENDER_FEMALE = 2;

    public final String uuid;
    public final String givenName;
    public final String familyName;
    public final int gender;
    // TODO: Make PatientDelta.birthdate and Patient.birthdate same type (LocalDate or DateTime).
    public final LocalDate birthdate;
    public final String locationUuid;

    /** Creates an instance of {@link Patient} from a network {@link JsonPatient} object. */
    public static Patient fromJson(JsonPatient patient) {
        return builder()
            .setId(patient.id)
            .setUuid(patient.uuid)
            .setGivenName(patient.given_name)
            .setFamilyName(patient.family_name)
            .setGender("F".equals(patient.gender) ? GENDER_FEMALE :
                "M".equals(patient.gender) ? GENDER_MALE : GENDER_UNKNOWN)
            .setBirthdate(patient.birthdate)
            .setLocationUuid(
                patient.assigned_location == null ? null : patient.assigned_location.uuid)
            .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Puts this object's fields in a {@link ContentValues} object for insertion into a database. */
    public ContentValues toContentValues() {
        ContentValues cv = new ContentValues();
        cv.put(Contracts.Patients.UUID, uuid);
        cv.put(Contracts.Patients.ID, id);
        cv.put(Contracts.Patients.GIVEN_NAME, givenName);
        cv.put(Contracts.Patients.FAMILY_NAME, familyName);
        cv.put(Contracts.Patients.GENDER,
            gender == JsonPatient.GENDER_MALE ? "M" :
                gender == JsonPatient.GENDER_FEMALE ? "F" : "U");
        cv.put(Contracts.Patients.BIRTHDATE, Utils.toString(birthdate));
        cv.put(Contracts.Patients.LOCATION_UUID,
            locationUuid == null ? Zones.DEFAULT_LOCATION_UUID : locationUuid);
        return cv;
    }

    @Override public int compareTo(Patient other) {
        return Utils.alphanumericComparator.compare(id, other.id);
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
        private int mGender;
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

        public Builder setGender(int gender) {
            this.mGender = gender;
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
        this.id = builder.mId;
        this.uuid = builder.mUuid;
        this.givenName = builder.mGivenName;
        this.familyName = builder.mFamilyName;
        this.gender = builder.mGender;
        this.birthdate = builder.mBirthdate;
        this.locationUuid = builder.mLocationUuid;
    }

    /** An {@link CursorLoader} that loads {@link Patient}s. */
    @Immutable
    public static class Loader implements CursorLoader<Patient> {
        @Override public Patient fromCursor(Cursor cursor) {
            return builder()
                .setUuid(Utils.getString(cursor, Contracts.Patients.UUID))
                .setId(Utils.getString(cursor, Contracts.Patients.ID))
                .setGivenName(Utils.getString(cursor, Contracts.Patients.GIVEN_NAME))
                .setFamilyName(Utils.getString(cursor, Contracts.Patients.FAMILY_NAME))
                .setBirthdate(Utils.getLocalDate(cursor, Contracts.Patients.BIRTHDATE))
                .setGender(getGenderFromString(Utils.getString(cursor, Contracts.Patients.GENDER)))
                .setLocationUuid(Utils.getString(cursor, Contracts.Patients.LOCATION_UUID))
                .build();
        }

        private static int getGenderFromString(String genderString) {
            switch (genderString) {
                case "M":
                    return GENDER_MALE;
                case "F":
                    return GENDER_FEMALE;
                default:
                    return GENDER_UNKNOWN;
            }
        }
    }
}
