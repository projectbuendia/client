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

/** A group of tiles (shown in one row) or a group of grid rows (with a heading) in a chart. */
public class ChartSection {
    public final String label;
    public final List<ChartItem> items;

    public ChartSection(String label) {
        this(label, new ArrayList<ChartItem>());
    }

    public ChartSection(String label, List<ChartItem> items) {
        this.label = label;
        this.items = items;
    }
}
