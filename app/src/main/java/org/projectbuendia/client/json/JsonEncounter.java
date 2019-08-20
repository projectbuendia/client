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

import org.joda.time.DateTime;

import java.util.Map;

/** JSON representation of an OpenMRS Encounter; call Serializers.registerTo before use. */
public class JsonEncounter {
    public String patient_uuid;
    public String uuid;
    public DateTime timestamp;
    public String enterer_id;
    /** A {conceptUuid: value} map, where value can be a number, string, or answer UUID. */
    public Map<String, Object> observations;
    public String[] order_uuids;  // orders executed during this encounter
}
