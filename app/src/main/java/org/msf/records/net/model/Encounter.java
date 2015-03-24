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

import org.joda.time.DateTime;

import java.util.Map;

/**
 * A single encounter between a patient and a clinician in a patient chart, which can be used for
 * GSON/JSON encoding/decoding.
 *
 * <p>Must call {@link CustomSerialization#registerTo(com.google.gson.GsonBuilder)} before use.
 */
public class Encounter {
    /**
     * The uuid of the encounter.
     */
    public String uuid;
    public DateTime timestamp;
    public String enterer_id;
    /**
     * Keys are uuid strings for the concept representing the concept observed. Values are the
     * value observed. To find out what the type is the type in the concept dictionary must be
     * inspected. Common values are doubles and String representing concept uuids of coded concepts.
     */
    public Map<Object, Object> observations;

}
