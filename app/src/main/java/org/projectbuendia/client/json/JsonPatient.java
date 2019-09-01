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

import com.google.common.base.MoreObjects;

import org.joda.time.LocalDate;
import org.projectbuendia.client.models.Sex;

import java.io.Serializable;

/** JSON representation of an OpenMRS Patient. */
public class JsonPatient implements Serializable {
    public String uuid;  // OpenMRS record UUID
    public boolean voided; // true if the patient has been voided.
    public String id;  // user-specified patient ID
    public String given_name;
    public String family_name;
    public Sex sex;
    public LocalDate birthdate;

    public JsonPatient() {
    }

    @Override public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("uuid", uuid)
            .add("voided", voided)
            .add("id", id)
            .add("given_name", given_name)
            .add("family_name", family_name)
            .add("sex", sex)
            .add("birthdate", birthdate.toString())
            .toString();
    }
}
