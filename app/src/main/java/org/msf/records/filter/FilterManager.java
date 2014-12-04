package org.msf.records.filter;

import android.content.Context;

import org.msf.records.filter.FilterGroup.FilterType;
import org.msf.records.model.LocationTree;
import org.msf.records.model.LocationTreeFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * FilterManager is a container for all available patient filters that will be displayed to the
 * user, categorized by filter type.
 */
public class FilterManager {
    private static final String TAG = "FilterManager";

    // Id and name filters should always be applied.
    private static final FilterGroup baseFilters = new FilterGroup(
            FilterType.OR, new IdFilter(), new NameFilter()).setName("All Patients");
    private static final SimpleSelectionFilter[] OTHER_FILTERS = new SimpleSelectionFilter[] {
        new FilterGroup(baseFilters, new PregnantFilter()).setName("Pregnant"),
        new FilterGroup(baseFilters, new AgeFilter(5)).setName("Children Under 5"),
        new FilterGroup(baseFilters, new AgeFilter(2)).setName("Children Under 2")
    };

    public static SimpleSelectionFilter getDefaultFilter() {
        return baseFilters;
    }

    public static SimpleSelectionFilter[] getZoneFilters(Context context) {
        LocationTree tree = new LocationTreeFactory(context).build();

        List<SimpleSelectionFilter> filters = new ArrayList<SimpleSelectionFilter>();
        if (tree != null) {
            for (Object zone : tree.getLocationsForDepth(1)) {
                filters.add(new FilterGroup(
                        baseFilters, new ZoneFilter(zone.toString())).setName(zone.toString()));
            }
        }
        SimpleSelectionFilter[] filterArray = new SimpleSelectionFilter[filters.size()];
        filters.toArray(filterArray);
        return filterArray;
    }

    public static SimpleSelectionFilter[] getTentFilters(Context context) {
        LocationTree tree = new LocationTreeFactory(context).build();

        List<SimpleSelectionFilter> filters = new ArrayList<SimpleSelectionFilter>();
        if (tree != null) {
            for (Object tent : tree.getLocationsForDepth(2)) {
                filters.add(new FilterGroup(
                        baseFilters, new TentFilter(tent.toString())).setName(tent.toString()));
            }
        }
        SimpleSelectionFilter[] filterArray = new SimpleSelectionFilter[filters.size()];
        filters.toArray(filterArray);
        return filterArray;
    }

    public static SimpleSelectionFilter[] getOtherFilters() {
        return OTHER_FILTERS;
    }

    public static SimpleSelectionFilter[] getFiltersForDisplay(Context context) {
        List<SimpleSelectionFilter> allFilters = new ArrayList<SimpleSelectionFilter>();
        allFilters.add(getDefaultFilter());
        for (SimpleSelectionFilter filter : getZoneFilters(context)) {
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
