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

import java.util.Map;

/**
 * A single medical concept, usually a question or answer in an observation for a patient, which can
 * be used for GSON/JSON encoding/decoding. Stores localization and type information.
 */
public class Concept {

    public String uuid;
    /**
     * The server side id. Prefer the UUID for sending to the server, but this is needed for some
     * xforms tasks.
     */
    public Integer xform_id;
    public ConceptType type;

    /**
     * A map from locales to the name in that locale. Eg en->heart, fr->couer, ...
     */
    public Map<String, String> names;
}
