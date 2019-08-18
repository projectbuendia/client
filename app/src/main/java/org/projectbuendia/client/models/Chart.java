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

import java.util.ArrayList;
import java.util.List;

/** A chart definition. */
public class Chart {
    // TODO(ping): Support multiple charts stored in multiple forms.  Right now,
    // there is no UUID field because there is only ever a single chart form.
    public final List<ChartSection> tileGroups;
    public final List<ChartSection> rowGroups;
    public final String name;

    public Chart(String name) {
        this.name = name;
        this.tileGroups = new ArrayList<>();
        this.rowGroups = new ArrayList<>();
    }
}
