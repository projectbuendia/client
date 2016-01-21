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

import org.joda.time.LocalDate;

import java.io.Serializable;

/** JSON representation of an OpenMRS Patient. */
public class JsonPatient implements Serializable {
    public static final int GENDER_UNKNOWN = 0;
    public static final int GENDER_MALE = 1;
    public static final int GENDER_FEMALE = 2;

    public String uuid;  // OpenMRS record UUID
    public boolean voided; // true if the patient has been voided.
    public String id;  // user-specified patient ID
    public String given_name;
    public String family_name;
    public String sex; // must be "M" or "F"
    public LocalDate birthdate;
    public JsonLocation assigned_location; // TODO: make this a plain uuid; API change

    public JsonPatient() {
    }
}
