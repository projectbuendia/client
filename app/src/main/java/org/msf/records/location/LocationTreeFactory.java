package org.msf.records.location;

import android.content.Context;
import android.database.Cursor;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import org.msf.records.App;
import org.msf.records.filter.AllFilter;
import org.msf.records.filter.FilterQueryProviderFactory;
import org.msf.records.filter.SimpleSelectionFilter;
import org.msf.records.model.LocalizedString;
import org.msf.records.net.model.Location;
import org.msf.records.sync.LocationProjection;
import org.msf.records.sync.LocationProviderContract;
import org.msf.records.sync.PatientProjection;
import org.msf.records.sync.PatientProviderContract;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Constructs a {@link LocationTree} using a database Cursor and listens for changes
 * in location data.
 */
public class LocationTreeFactory {

    private final FilterQueryProviderFactory mLocationQueryFactory;
    private final FilterQueryProviderFactory mLocationNamesQueryFactory;
    private final FilterQueryProviderFactory mPatientCountsQueryFactory;

    private SimpleSelectionFilter mLocationFilter = new AllFilter();
    private SimpleSelectionFilter mLocationNameFilter = new AllFilter();
    private final SimpleSelectionFilter mPatientCountsFilter = new AllFilter();

    private final Multimap<String, Location> mLocationsByParentUuid;
    private final Map<String, LocalizedString.Builder> mLocationNamesByUuid;
    /**
     * Map from location UUID to number of patients at that location. This excludes any patients contained with
     * a smaller location within that location.
     */
    private final Map<String, Integer> mPatientCountsMap;

    LocationTreeFactory(Context context) {
        mLocationQueryFactory = new FilterQueryProviderFactory(context);
        mLocationQueryFactory.setUri(LocationProviderContract.LOCATIONS_CONTENT_URI);
        mLocationQueryFactory.setSortClause(null);
        mLocationQueryFactory.setProjection(LocationProjection.getLocationProjection());

        mLocationNamesQueryFactory = new FilterQueryProviderFactory(context);
        mLocationNamesQueryFactory.setUri(LocationProviderContract.LOCATION_NAMES_CONTENT_URI);
        mLocationNamesQueryFactory.setSortClause(null);
        mLocationNamesQueryFactory.setProjection(LocationProjection.getLocationNamesProjection());

        mPatientCountsQueryFactory = new FilterQueryProviderFactory(context);
        mPatientCountsQueryFactory.setUri(PatientProviderContract.CONTENT_URI_TENT_PATIENT_COUNTS);
        mPatientCountsQueryFactory.setSortClause(null);
        mPatientCountsQueryFactory.setProjection(PatientProjection.getPatientCountsProjection());

        mLocationsByParentUuid = HashMultimap.create();
        mLocationNamesByUuid = new HashMap<>();
        mPatientCountsMap = new HashMap<>();
    }

    public LocationTree build() {
        Cursor patientCountsCursor =
                mPatientCountsQueryFactory.getFilterQueryProvider(mPatientCountsFilter)
                        .runQuery("");
        buildPatientCounts(patientCountsCursor);
        patientCountsCursor.close();

        try (Cursor locationCursor =
                mLocationQueryFactory.getFilterQueryProvider(mLocationFilter)
                        .runQuery("")) {
            buildLocationMap(locationCursor);
        }

        try (Cursor locationNamesCursor = mLocationNamesQueryFactory
                .getFilterQueryProvider(mLocationNameFilter)
                .runQuery("")) {
            buildLocationNamesMap(locationNamesCursor);
        }
        return buildTree();
    }

    public void setLocationFilter(SimpleSelectionFilter locationFilter) {
        mLocationFilter = locationFilter;
    }

    public void setLocationNameFilter(SimpleSelectionFilter locationNameFilter) {
        mLocationNameFilter = locationNameFilter;
    }

    private void buildPatientCounts(Cursor cursor) {
        mPatientCountsMap.clear();
        while (cursor.moveToNext()) {
            mPatientCountsMap.put(
                    cursor.getString(PatientProjection.COUNTS_COLUMN_LOCATION_UUID),
                    cursor.getInt(PatientProjection.COUNTS_COLUMN_TENT_PATIENT_COUNT));
        }
    }

    // Initializes mLocationsByParent from the given cursor. Does NOT close the cursor.
    private void buildLocationMap(Cursor cursor) {
        mLocationsByParentUuid.clear();
        while (cursor.moveToNext()) {
            String uuid = cursor.getString(LocationProjection.LOCATION_LOCATION_UUID_COLUMN);
            String parent_uuid = cursor.getString(LocationProjection.LOCATION_PARENT_UUID_COLUMN);

            if (uuid == null) {
                continue;
            }

            Location location = new Location();
            location.uuid = uuid;
            location.parent_uuid = parent_uuid;
            mLocationsByParentUuid.put(parent_uuid, location);
        }
    }

    // Initializes mLocationNamesMap from the given cursor. Does NOT close the cursor.
    private void buildLocationNamesMap(Cursor cursor) {
    	mLocationNamesByUuid.clear();
        while (cursor.moveToNext()) {
            String name = cursor.getString(LocationProjection.LOCATION_NAME_NAME_COLUMN);
            String locale = cursor.getString(LocationProjection.LOCATION_NAME_LOCALE_COLUMN);
            String uuid = cursor.getString(LocationProjection.LOCATION_NAME_LOCATION_UUID_COLUMN);

            if (name == null || locale == null || uuid == null) {
                continue;
            }

            if (!mLocationNamesByUuid.containsKey(uuid)) {
            	mLocationNamesByUuid.put(uuid, new LocalizedString.Builder());
            }
            mLocationNamesByUuid.get(uuid).addTranslation(locale, name);
        }
    }

    // Constructs the LocationTree or returns null if the root node is missing.
    @Nullable private LocationTree buildTree() {
        if (!mLocationsByParentUuid.containsKey(null)) {
            return null;
        }

        // Map location names to this location as necessary.
        for (Location location : mLocationsByParentUuid.values()) {
            if (mLocationNamesByUuid.containsKey(location.uuid)) {
                location.names = mLocationNamesByUuid.get(location.uuid).build().asMap();
            }
        }

       return new LocationTree(
    		   	App.getInstance().getResources(),
        		mLocationsByParentUuid.values(),
        		mPatientCountsMap);
    }
}
