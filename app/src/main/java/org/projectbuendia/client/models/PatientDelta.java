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

import com.google.common.base.Optional;

import org.joda.time.LocalDate;
import org.json.JSONException;
import org.json.JSONObject;
import org.projectbuendia.client.providers.Contracts;
import org.projectbuendia.client.utils.Logger;
import org.projectbuendia.client.utils.Utils;

/** Represents the data to write to a new patient or the data to update on a patient. */
public class PatientDelta {
    public Optional<String> id = Optional.absent();
    public Optional<String> givenName = Optional.absent();
    public Optional<String> familyName = Optional.absent();
    public Optional<Sex> sex = Optional.absent();
    public Optional<LocalDate> birthdate = Optional.absent();
    private static final Logger LOG = Logger.create();

    /** Returns the {@link ContentValues} corresponding to the delta. */
    public ContentValues toContentValues() {
        ContentValues cv = new ContentValues();

        if (id.isPresent()) {
            cv.put(Contracts.Patients.ID, id.get());
        }
        if (givenName.isPresent()) {
            cv.put(Contracts.Patients.GIVEN_NAME, givenName.get());
        }
        if (familyName.isPresent()) {
            cv.put(Contracts.Patients.FAMILY_NAME, familyName.get());
        }
        if (sex.isPresent()) {
            cv.put(Contracts.Patients.SEX, Sex.nullableNameOf(sex.get()));
        }
        if (birthdate.isPresent()) {
            cv.put(Contracts.Patients.BIRTHDATE, birthdate.get().toString());
        }
        return cv;
    }

    @Override public String toString() {
        JSONObject jsonObject = new JSONObject();
        if (toJson(jsonObject)) {
            return jsonObject.toString();
        }
        return super.toString();
    }

    /**
     * Serializes the fields changed in the delta to a {@link JSONObject}.
     * @return whether serialization succeeded
     */
    public boolean toJson(JSONObject json) {
        // TODO: Use a JsonPatient instead of all these field name constants.
        try {
            if (id.isPresent()) {
                json.put("id", id.get());
            }
            if (givenName.isPresent()) {
                json.put("given_name", givenName.get());
            }
            if (familyName.isPresent()) {
                json.put("family_name", familyName.get());
            }
            if (sex.isPresent()) {
                json.put("sex", Sex.serialize(sex.get()));
            }
            if (birthdate.isPresent()) {
                json.put("birthdate", Utils.format(birthdate.get()));
            }
            return true;
        } catch (JSONException e) {
            LOG.w(e, "Unable to serialize a patient delta to JSON.");
            return false;
        }
    }
}
