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

package org.projectbuendia.client.json;

import android.content.ContentValues;

import org.joda.time.LocalDate;
import org.projectbuendia.client.models.Sex;
import org.projectbuendia.client.providers.Contracts.Patients;

import java.io.Serializable;

/** JSON representation of an OpenMRS Patient. */
public class JsonPatient implements Serializable {
    public String uuid;  // OpenMRS record UUID
    public String id;  // user-specified patient ID
    public String given_name;
    public String family_name;
    public Sex sex;
    public LocalDate birthdate;
    public boolean voided;

    public JsonPatient() { }

    /** Populates a ContentValues record with the fields of this patient. */
    public ContentValues toContentValues() {
        ContentValues cv = new ContentValues();
        cv.put(Patients.ID, id);
        cv.put(Patients.GIVEN_NAME, given_name);
        cv.put(Patients.FAMILY_NAME, family_name);
        cv.put(Patients.SEX, Sex.nullableNameOf(sex));
        cv.put(Patients.BIRTHDATE, birthdate != null ? birthdate.toString() : null);
        return cv;
    }
}
