package org.msf.records.ui.matchers;

import org.mockito.ArgumentMatcher;
import org.msf.records.filter.FilterGroup;
import org.msf.records.filter.LocationUuidFilter;
import org.msf.records.filter.SimpleSelectionFilter;

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
     * Matches any {@link SimpleSelectionFilter} that is or wraps (via a {@link FilterGroup})
     * a {@link LocationUuidFilter} filtering by the specified location UUID.
     */
    public static class IsFilterGroupWithLocationFilter
            extends ArgumentMatcher<SimpleSelectionFilter> {
        private String mLocationUuid;

        public IsFilterGroupWithLocationFilter(String locationUuid) {
            mLocationUuid = locationUuid;
        }

        /**
         * Matches any {@link SimpleSelectionFilter} that is or wraps (via a {@link FilterGroup})
         * a {@link LocationUuidFilter} filtering by the specified location UUID.
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
            if (!(filter instanceof FilterGroup)) {
                return false;
            }

            List<SimpleSelectionFilter> filterList = ((FilterGroup) filter).getFilters();
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
