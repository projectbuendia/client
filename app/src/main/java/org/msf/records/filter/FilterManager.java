package org.msf.records.filter;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import org.msf.records.filter.FilterGroup.FilterType;

/**
 * FilterManager is a container for all available patient filters that will be displayed to the
 * user, categorized by filter type.
 */
public class FilterManager {
    // Id and name filters should always be applied.
    private static final FilterGroup baseFilters = new FilterGroup(
            FilterType.OR, new IdFilter(), new NameFilter());

    // TODO(akalachman): Get from server.
    private static final SimpleSelectionFilter[] ZONE_FILTERS = new SimpleSelectionFilter[] {
        new FilterGroup(baseFilters, new ZoneFilter(null)).setName("All Patients"),
        new FilterGroup(baseFilters, new ZoneFilter("Triage")).setName("Triage"),
        new FilterGroup(baseFilters, new ZoneFilter("Suspect")).setName("Suspect"),
        new FilterGroup(baseFilters, new ZoneFilter("Probable")).setName("Probable"),
        new FilterGroup(baseFilters, new ZoneFilter("Confirmed")).setName("Confirmed"),
    };

    private static final SimpleSelectionFilter[] OTHER_FILTERS = new SimpleSelectionFilter[] {
        new FilterGroup(baseFilters, new PregnantFilter()).setName("Pregnant"),
        new FilterGroup(baseFilters, new AgeFilter(5)).setName("Children Under 5"),
        new FilterGroup(baseFilters, new AgeFilter(2)).setName("Children Under 2")
    };

    public static SimpleSelectionFilter[] getZoneFilters() {
        return ZONE_FILTERS;
    }

    public static SimpleSelectionFilter[] getOtherFilters() {
        return OTHER_FILTERS;
    }

    public static SimpleSelectionFilter[] getFiltersForDisplay() {
        List<SimpleSelectionFilter> allFilters = new ArrayList<SimpleSelectionFilter>();
        for (SimpleSelectionFilter filter : getZoneFilters()) {
            allFilters.add(filter);
        }
        allFilters.add(null); // Section break
        for (SimpleSelectionFilter filter : getOtherFilters()) {
            allFilters.add(filter);
        }

        SimpleSelectionFilter[] filterArray = new SimpleSelectionFilter[allFilters.size()];
        allFilters.toArray(filterArray);
        return filterArray;
    }
}
