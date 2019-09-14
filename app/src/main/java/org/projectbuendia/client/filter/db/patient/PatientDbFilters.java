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

package org.projectbuendia.client.filter.db.patient;

import com.google.common.collect.ImmutableList;

import org.projectbuendia.client.filter.db.AllFilter;
import org.projectbuendia.client.filter.db.SimpleSelectionFilter;
import org.projectbuendia.client.models.Location;
import org.projectbuendia.client.models.LocationForest;

import java.util.ArrayList;
import java.util.List;

/** All available patient filters available to the user, categorized by filter type. */
public final class PatientDbFilters {
    public static SimpleSelectionFilter getDefaultFilter() {
        return new AllFilter();
    }

    /** Returns a list of all {@link SimpleSelectionFilter}s that should be displayed to the user. */
    public static List<SimpleSelectionFilter<?>> getFiltersForDisplay(LocationForest forest) {
        List<SimpleSelectionFilter<?>> allFilters = new ArrayList<>();
        allFilters.addAll(getLocationFilters(forest));
        allFilters.add(null); // Section break
        allFilters.addAll(getOtherFilters());
        return allFilters;
    }

    /** Returns a list of filters, each representing a location. */
    public static List<SimpleSelectionFilter<?>> getLocationFilters(LocationForest forest) {
        List<SimpleSelectionFilter<?>> filters = new ArrayList<>();
        for (Location location : forest.allNodes()) {
            filters.add(new LocationUuidFilter(forest, location));
        }
        return filters;
    }

    /** Returns a list of all the filters unrelated to locations. */
    public static List<SimpleSelectionFilter<?>> getOtherFilters() {
        return ImmutableList.of(
            new PregnancyFilter(),
            new AgeFilter(5),
            new AgeFilter(2)
        );
    }
}
