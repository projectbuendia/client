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

package org.msf.records.net.model;

/**
 * A simple Java bean representing a JSON object used to encode information about encounters
 * (between a patient and clinician) and the observations made there.
 *
 * <p>Must call {@link CustomSerialization#registerTo(com.google.gson.GsonBuilder)} before use.
 */
public class PatientChart {

    public String uuid;
    public Encounter[] encounters;
}
