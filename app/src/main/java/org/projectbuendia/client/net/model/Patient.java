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

package org.projectbuendia.client.net.model;

import com.google.common.base.MoreObjects;

import org.joda.time.LocalDate;

import java.io.Serializable;

/**
 * A simple Java bean for representing a patient which can be used for JSON/Gson encoding/decoding.
 */
public class Patient implements Serializable {
    public static final int GENDER_UNKNOWN = 0;
    public static final int GENDER_MALE = 1;
    public static final int GENDER_FEMALE = 2;

    public String uuid;  // OpenMRS record UUID
    public String id;  // user-specified patient ID
    public String given_name;
    public String family_name;
    public String gender;  // must be "M" or "F"
    public LocalDate birthdate;
    public Location assigned_location;

    public Patient() {}

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("uuid", uuid)
                .add("id", id)
                .add("given_name", given_name)
                .add("family_name", family_name)
                .add("gender", gender)
                .add("birthdate", birthdate.toString())
                .add("assigned_location", assigned_location)
                .toString();
    }
}