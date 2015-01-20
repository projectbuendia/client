package org.msf.records.filter.db;

import org.msf.records.data.app.AppLocation;
import org.msf.records.data.app.AppLocationTree;
import org.msf.records.model.Concept;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * All available patient filters available to the user, categorized by filter type.
 */
public final class PatientDbFilters {
    private static final SimpleSelectionFilter[] OTHER_FILTERS = new SimpleSelectionFilter[] {
        // TODO(akalachman): Localize filter names and extract elsewhere.
        // TODO(akalachman): Remove FilterGroup dep (only used for setName).
        new SimpleSelectionFilterGroup(
                new ConceptFilter(Concept.PREGNANCY_UUID, Concept.YES_UUID)).setName("Pregnant"),
        new SimpleSelectionFilterGroup(new AgeFilter(5)).setName("Children Under 5"),
        new SimpleSelectionFilterGroup(new AgeFilter(2)).setName("Children Under 2")
    };

    private static final SimpleSelectionFilter DEFAULT_FILTER = new AllFilter();

    public static SimpleSelectionFilter getDefaultFilter() {
        return DEFAULT_FILTER;
    }

    /**
     * Returns an array of {@link SimpleSelectionFilter}'s, each representing a zone.
     */
    public static SimpleSelectionFilter[] getZoneFilters(AppLocationTree locationTree) {
        List<SimpleSelectionFilter> filters = new ArrayList<>();

        for (AppLocation zone : locationTree.getChildren(locationTree.getRoot())) {
            // TODO(akalachman): Remove FilterGroup dep (only used for setName).
            filters.add(new SimpleSelectionFilterGroup(
                   new LocationUuidFilter(locationTree, zone)).setName(zone.toString()));
        }
        SimpleSelectionFilter[] filterArray = new SimpleSelectionFilter[filters.size()];
        filters.toArray(filterArray);
        return filterArray;
    }

    /**
     * Returns an array of all {@link SimpleSelectionFilter}'s that are unrelated to user location
     * (for example, based on pregnancy or age).
     */
    public static SimpleSelectionFilter[] getOtherFilters() {
        return OTHER_FILTERS;
    }

    /**
     * Returns an array of all {@link SimpleSelectionFilter}'s that should be displayed to the user.
     */
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
