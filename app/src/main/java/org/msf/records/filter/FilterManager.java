package org.msf.records.filter;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import org.msf.records.events.location.LocationsLoadedEvent;
import org.msf.records.filter.FilterGroup.FilterType;
import org.msf.records.location.LocationManager;
import org.msf.records.location.LocationTree;

import de.greenrobot.event.EventBus;

/**
 * FilterManager is a container for all available patient filters that will be displayed to the
 * user, categorized by filter type.
 */
public class FilterManager {
    private static final String TAG = "FilterManager";
    private static LocationTree mRoot = null;

    // Id and name filters should always be applied.
    private static final FilterGroup baseFilters = new FilterGroup(
            FilterType.OR, new IdFilter(), new NameFilter()).setName("All Patients");
    private static final SimpleSelectionFilter[] OTHER_FILTERS = new SimpleSelectionFilter[] {
        // TODO(akalachman): Enable pregnancy filter.
        // new FilterGroup(baseFilters, new PregnantFilter()).setName("Pregnant"),
        new FilterGroup(baseFilters, new AgeFilter(5)).setName("Children Under 5"),
        new FilterGroup(baseFilters, new AgeFilter(2)).setName("Children Under 2")
    };

    private static class LocationSyncSubscriber {
        public synchronized void onEvent(LocationsLoadedEvent event) {
            mRoot = event.mLocationTree;
        }
    }

    static {
        EventBus.getDefault().register(new LocationSyncSubscriber());
    }

    public static SimpleSelectionFilter getDefaultFilter() {
        return baseFilters;
    }

    public static SimpleSelectionFilter[] getZoneFilters(Context context) {
        List<SimpleSelectionFilter> filters = new ArrayList<SimpleSelectionFilter>();
        if (mRoot != null) {
            for (LocationTree zone : mRoot.getLocationsForDepth(1)) {
                filters.add(new FilterGroup(
                        baseFilters,
                        new LocationUuidFilter(zone.getLocation().uuid)).setName(zone.toString()));
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
