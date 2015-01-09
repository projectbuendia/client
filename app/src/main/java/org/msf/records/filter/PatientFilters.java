package org.msf.records.filter;

import org.msf.records.data.app.AppLocation;
import org.msf.records.data.app.AppLocationTree;
import org.msf.records.events.location.LocationsLoadedEvent;
import org.msf.records.filter.FilterGroup.FilterType;
import org.msf.records.location.LocationTree;
import org.msf.records.location.LocationTree.LocationSubtree;
import org.msf.records.model.Concept;

import de.greenrobot.event.EventBus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * All available patient filters available to the user, categorized by filter type.
 */
public final class PatientFilters {

    // Id and name filters should always be applied.
    private static final FilterGroup BASE_FILTERS =
            new FilterGroup(
                    FilterType.OR,
                    new IdFilter(),
                    new NameFilter()).setName("All Patients");

    private static final SimpleSelectionFilter[] OTHER_FILTERS = new SimpleSelectionFilter[] {
        // TODO(akalachman): Localize filter names.
        new FilterGroup(BASE_FILTERS, new ConceptFilter(Concept.PREGNANCY_UUID, Concept.YES_UUID))
                .setName("Pregnant"),
        new FilterGroup(BASE_FILTERS, new AgeFilter(5)).setName("Children Under 5"),
        new FilterGroup(BASE_FILTERS, new AgeFilter(2)).setName("Children Under 2")
    };

    public static SimpleSelectionFilter getDefaultFilter() {
        return BASE_FILTERS;
    }

    public static SimpleSelectionFilter[] getZoneFilters(AppLocationTree locationTree) {
        List<SimpleSelectionFilter> filters = new ArrayList<>();

        for (AppLocation zone : locationTree.getChildren(locationTree.getRoot())) {
            filters.add(new FilterGroup(
                    BASE_FILTERS,
                    new LocationUuidFilter(locationTree, zone)).setName(zone.toString()));
        }
        SimpleSelectionFilter[] filterArray = new SimpleSelectionFilter[filters.size()];
        filters.toArray(filterArray);
        return filterArray;
    }

    public static SimpleSelectionFilter[] getOtherFilters() {
        return OTHER_FILTERS;
    }

    public static SimpleSelectionFilter[] getFiltersForDisplay(AppLocationTree locationTree) {
        List<SimpleSelectionFilter> allFilters = new ArrayList<>();
        allFilters.add(getDefaultFilter());
        Collections.addAll(allFilters, getZoneFilters(locationTree));
        allFilters.add(null); // Section break
        Collections.addAll(allFilters, getOtherFilters());

        SimpleSelectionFilter[] filterArray = new SimpleSelectionFilter[allFilters.size()];
        allFilters.toArray(filterArray);
        return filterArray;
    }
}
