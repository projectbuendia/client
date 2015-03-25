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

package org.msf.records.ui.matchers;

import org.mockito.ArgumentMatcher;
import org.msf.records.filter.db.SimpleSelectionFilterGroup;
import org.msf.records.filter.db.patient.LocationUuidFilter;
import org.msf.records.filter.db.SimpleSelectionFilter;

import java.util.List;

/**
 * Matchers for {@link SimpleSelectionFilter} objects and arrays.
 */
public class SimpleSelectionFilterMatchers {
    private SimpleSelectionFilterMatchers() {}

    /**
     * Matches any array of filters containing a filter with the specified name.
     */
    public static class ContainsFilterWithName extends ArgumentMatcher<SimpleSelectionFilter[]> {
        private String mFilterName;

        public ContainsFilterWithName(String filterName) {
            mFilterName = filterName;
        }

        /**
         * Matches any array of filters containing a filter with the specified name.
         *
         * @param filters an array of {@link SimpleSelectionFilter}'s
         * @return true if the array contains a filter with the specified name
         */
        public boolean matches(Object filters) {
            SimpleSelectionFilter[] filterArr = (SimpleSelectionFilter[])filters;
            for (int i = 0; i < filterArr.length; i++) {
                if (filterArr[i].toString().equals(mFilterName)) {
                    return true;
                }
            }

            return false;
        }
    }

    /**
     * Matches any {@link SimpleSelectionFilter} that is or wraps (via a
     * {@link SimpleSelectionFilterGroup}) a {@link LocationUuidFilter} filtering by the specified
     * location UUID.
     */
    public static class IsFilterGroupWithLocationFilter
            extends ArgumentMatcher<SimpleSelectionFilter> {
        private String mLocationUuid;

        public IsFilterGroupWithLocationFilter(String locationUuid) {
            mLocationUuid = locationUuid;
        }

        /**
         * Matches any {@link SimpleSelectionFilter} that is or wraps (via a
         * {@link SimpleSelectionFilterGroup}) a {@link LocationUuidFilter} filtering by the
         * specified location UUID.
         */
        public boolean matches(Object filter) {
            if (isMatchingLocationFilter((SimpleSelectionFilter)filter)) {
                return true;
            }

            if (isMatchingFilterGroup((SimpleSelectionFilter)filter)) {
                return true;
            }

            return false;
        }

        private boolean isMatchingFilterGroup(SimpleSelectionFilter filter) {
            if (!(filter instanceof SimpleSelectionFilterGroup)) {
                return false;
            }

            List<SimpleSelectionFilter> filterList =
                    ((SimpleSelectionFilterGroup) filter).getFilters();
            for (SimpleSelectionFilter internalFilter : filterList) {
                if (matches(internalFilter)) {
                    return true;
                }
            }

            return false;
        }

        private boolean isMatchingLocationFilter(SimpleSelectionFilter filter) {
            if (!(filter instanceof LocationUuidFilter)) {
                return false;
            }

            return ((LocationUuidFilter) filter).getFilterRootUuid().equals(mLocationUuid);
        }
    }
}
