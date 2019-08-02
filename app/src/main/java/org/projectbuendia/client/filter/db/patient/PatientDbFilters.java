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

import org.projectbuendia.client.App;
import org.projectbuendia.client.R;
import org.projectbuendia.client.filter.db.AllFilter;
import org.projectbuendia.client.filter.db.SimpleSelectionFilter;
import org.projectbuendia.client.models.ConceptUuids;
import org.projectbuendia.client.models.LocationTree;
import org.projectbuendia.client.models.NewLocation;
import org.projectbuendia.client.models.NewLocationTree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** All available patient filters available to the user, categorized by filter type. */
public final class PatientDbFilters {
    private static final SimpleSelectionFilter[] OTHER_FILTERS = new SimpleSelectionFilter[] {
        new ConceptFilter(
            App.getInstance().getString(R.string.pregnant),
            ConceptUuids.PREGNANCY_UUID,
            ConceptUuids.YES_UUID),
        new AgeFilter(5),
        new AgeFilter(2)
    };

    private static final SimpleSelectionFilter DEFAULT_FILTER = new AllFilter();

    public static SimpleSelectionFilter getDefaultFilter() {
        return DEFAULT_FILTER;
    }

    /** Returns an array of all {@link SimpleSelectionFilter}s that should be displayed to the user. */
    public static SimpleSelectionFilter[] getFiltersForDisplay(LocationTree locationTree) {
        List<SimpleSelectionFilter> allFilters = new ArrayList<>();
        allFilters.add(new PresentFilter());
        Collections.addAll(allFilters, getLocationFilters(locationTree));
        allFilters.add(null); // Section break
        Collections.addAll(allFilters, getOtherFilters());

        SimpleSelectionFilter[] filterArray = new SimpleSelectionFilter[allFilters.size()];
        allFilters.toArray(filterArray);
        return filterArray;
    }

    /** Returns an array of {@link SimpleSelectionFilter}s, each representing a zone. */
    public static SimpleSelectionFilter[] getLocationFilters(LocationTree locationTree) {
        List<SimpleSelectionFilter> filters = new ArrayList<>();
        NewLocationTree tree = null;

        for (NewLocation location : tree.allNodes()) {
            filters.add(new LocationUuidFilter(tree, location));
        }
        SimpleSelectionFilter[] filterArray = new SimpleSelectionFilter[filters.size()];
        filters.toArray(filterArray);
        return filterArray;
    }

    /**
     * Returns an array of all {@link SimpleSelectionFilter}s that are unrelated to user location
     * (for example, based on pregnancy or age).
     */
    public static SimpleSelectionFilter[] getOtherFilters() {
        return OTHER_FILTERS;
    }
}
