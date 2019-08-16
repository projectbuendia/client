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

/**
 * A simple Java bean for JSON/Gson encoding/decoding, defining how the patient chart should be
 * displayed, giving the grouping and ordering of fields.
 */
public class JsonChart {
    public String uuid;  // UUID of the OpenMRS Form
    public String version;  // dot-separated decimal integers, e.g. "0.6", "0.7.1"
    public JsonChartSection[] sections;  // sections in display order
}
