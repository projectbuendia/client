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

package org.projectbuendia.client.models;

import java.util.List;

/** A chart definition. */
public class Chart {
    public final String uuid;  // UUID of the OpenMRS Form containing this chart definition
    public final List<ChartSection> tileGroups;
    public final List<ChartSection> rowGroups;

    public Chart(String uuid, List<ChartSection> tileGroups, List<ChartSection> rowGroups) {
        this.uuid = uuid;
        this.tileGroups = tileGroups;
        this.rowGroups = rowGroups;
    }
}
