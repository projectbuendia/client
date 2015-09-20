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

import java.util.Map;

/** JSON representation of an OpenMRS Concept */
public class JsonConcept {

    public String uuid;

    /** Server-side ID, needed for some XForms tasks. */
    public Integer xform_id;

    public ConceptType type;

    /** A map from locales to localized names, e.g. {'en': 'heart', 'fr': 'coeur'} */
    public Map<String, String> names;
}
