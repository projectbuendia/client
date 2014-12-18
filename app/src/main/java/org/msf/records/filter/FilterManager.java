package org.msf.records.filter;

import java.util.ArrayList;
import java.util.List;

import org.msf.records.events.location.LocationsLoadedEvent;
import org.msf.records.filter.FilterGroup.FilterType;
import org.msf.records.location.LocationTree;
import org.msf.records.location.LocationTree.LocationSubtree;

import de.greenrobot.event.EventBus;

/**
 * FilterManager is a container for all available patient filters that will be displayed to the
 * user, categorized by filter type.
 */
public class FilterManager {
	
    private static LocationTree mRoot = null;

    // Id and name filters should always be applied.
    private static final FilterGroup BASE_FILTERS = new FilterGroup(
            FilterType.OR, new IdFilter(), new NameFilter()).setName("All Patients");
    private static final SimpleSelectionFilter[] OTHER_FILTERS = new SimpleSelectionFilter[] {
//        new FilterGroup(BASE_FILTERS, new PregnantFilter()).setName("Pregnant"),
        new FilterGroup(BASE_FILTERS, new AgeFilter(5)).setName("Children Under 5"),
        new FilterGroup(BASE_FILTERS, new AgeFilter(2)).setName("Children Under 2")
    };

    @SuppressWarnings("unused") // Called by reflection from event bus.
    private static class LocationSyncSubscriber {
        public synchronized void onEvent(LocationsLoadedEvent event) {
            mRoot = event.locationTree;
        }
    }

    // TODO(rjlothian): This is likely to cause problems for testability. Remove it.
    static {
        EventBus.getDefault().register(new LocationSyncSubscriber());
    }

    public static SimpleSelectionFilter getDefaultFilter() {
        return BASE_FILTERS;
    }

    public static SimpleSelectionFilter[] getZoneFilters() {
        List<SimpleSelectionFilter> filters = new ArrayList<SimpleSelectionFilter>();
        if (mRoot != null) {
            for (LocationSubtree zone : mRoot.getLocationsForDepth(1)) {
                filters.add(new FilterGroup(
                        BASE_FILTERS,
                        new LocationUuidFilter(zone)).setName(zone.toString()));
            }
        }
        SimpleSelectionFilter[] filterArray = new SimpleSelectionFilter[filters.size()];
        filters.toArray(filterArray);
        return filterArray;
    }

    public static SimpleSelectionFilter[] getOtherFilters() {
        return OTHER_FILTERS;
    }

    public static SimpleSelectionFilter[] getFiltersForDisplay() {
        List<SimpleSelectionFilter> allFilters = new ArrayList<SimpleSelectionFilter>();
        allFilters.add(getDefaultFilter());
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
