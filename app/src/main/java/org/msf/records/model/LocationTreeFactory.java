package org.msf.records.model;

import android.content.Context;
import android.database.Cursor;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;

import org.msf.records.filter.AllFilter;
import org.msf.records.filter.FilterQueryProviderFactory;
import org.msf.records.filter.SimpleSelectionFilter;
import org.msf.records.net.model.Location;
import org.msf.records.sync.LocationProjection;
import org.msf.records.sync.LocationProviderContract;
import org.msf.records.sync.PatientProjection;
import org.msf.records.sync.PatientProviderContract;

import java.util.HashMap;
import java.util.Map;

/**
 * LocationTreeFactory constructs a LocationTree using a database Cursor and listens for changes
 * in location data.
 */
public class LocationTreeFactory {
    private final String TAG = "LocationTree";

    private FilterQueryProviderFactory mLocationQueryFactory;
    private FilterQueryProviderFactory mLocationNamesQueryFactory;
    private FilterQueryProviderFactory mPatientCountsQueryFactory;

    private SimpleSelectionFilter mLocationFilter = new AllFilter();
    private SimpleSelectionFilter mLocationNameFilter = new AllFilter();
    private SimpleSelectionFilter mPatientCountsFilter = new AllFilter();

    private Multimap<String, Location> mLocationsByParent;
    private Map<String, HashMap<String, String>> mLocationNamesMap;
    private Map<String, Integer> mPatientCountsMap;

    private final Context mContext;

    public LocationTreeFactory(Context context) {
        mContext = context;

        mLocationQueryFactory = new FilterQueryProviderFactory();
        mLocationQueryFactory.setUri(LocationProviderContract.LOCATIONS_CONTENT_URI);
        mLocationQueryFactory.setSortClause(null);
        mLocationQueryFactory.setProjection(LocationProjection.getLocationProjection());

        mLocationNamesQueryFactory = new FilterQueryProviderFactory();
        mLocationNamesQueryFactory.setUri(LocationProviderContract.LOCATION_NAMES_CONTENT_URI);
        mLocationNamesQueryFactory.setSortClause(null);
        mLocationNamesQueryFactory.setProjection(LocationProjection.getLocationNamesProjection());

        mPatientCountsQueryFactory = new FilterQueryProviderFactory();
        mPatientCountsQueryFactory.setUri(PatientProviderContract.CONTENT_URI_TENT_PATIENT_COUNTS);
        mPatientCountsQueryFactory.setSortClause(null);
        mPatientCountsQueryFactory.setProjection(PatientProjection.getPatientCountsProjection());

        mLocationsByParent = HashMultimap.create();
        mLocationNamesMap = new HashMap<String, HashMap<String, String>>();
        mPatientCountsMap = new HashMap<String, Integer>();
    }

    public LocationTree build() {
        Cursor patientCountsCursor =
                mPatientCountsQueryFactory.getFilterQueryProvider(mContext, mPatientCountsFilter)
                        .runQuery("");
        buildPatientCounts(patientCountsCursor);
        patientCountsCursor.close();

        Cursor locationCursor =
                mLocationQueryFactory.getFilterQueryProvider(mContext, mLocationFilter)
                        .runQuery("");
        buildLocationMap(locationCursor);
        locationCursor.close();

        Cursor locationNamesCursor =
                mLocationNamesQueryFactory.getFilterQueryProvider(mContext, mLocationNameFilter)
                        .runQuery("");
        buildLocationNamesMap(locationNamesCursor);

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
        mLocationsByParent.clear();
        while (cursor.moveToNext()) {
            String uuid = cursor.getString(LocationProjection.LOCATION_LOCATION_UUID_COLUMN);
            String parent_uuid = cursor.getString(LocationProjection.LOCATION_PARENT_UUID_COLUMN);

            if (uuid == null) {
                continue;
            }

            Location location = new Location();
            location.uuid = uuid;
            location.parent_uuid = parent_uuid;
            mLocationsByParent.put(parent_uuid, location);
        }
    }

    // Initializes mLocationNamesMap from the given cursor. Does NOT close the cursor.
    private void buildLocationNamesMap(Cursor cursor) {
        mLocationNamesMap.clear();
        while (cursor.moveToNext()) {
            String name = cursor.getString(LocationProjection.LOCATION_NAME_NAME_COLUMN);
            String locale = cursor.getString(LocationProjection.LOCATION_NAME_LOCALE_COLUMN);
            String uuid = cursor.getString(LocationProjection.LOCATION_NAME_LOCATION_UUID_COLUMN);

            if (name == null || locale == null || uuid == null) {
                continue;
            }

            if (!mLocationNamesMap.containsKey(uuid)) {
                mLocationNamesMap.put(uuid, new HashMap<String, String>());
            }
            mLocationNamesMap.get(uuid).put(locale, name);
        }
    }

    // Constructs the LocationTree or returns null if the root node is missing.
    private LocationTree buildTree() {
        if (!mLocationsByParent.containsKey(null)) {
            return null;
        }

        // Map location names to this location as necessary.
        for (Location location : mLocationsByParent.values()) {
            if (mLocationNamesMap.containsKey(location.uuid)) {
                location.names = mLocationNamesMap.get(location.uuid);
            }
        }

        // Start the tree from the single known root. Forests are NOT supported.
        Location root = Iterables.get(mLocationsByParent.get(null), 0);

        LocationTree.clearTreeIndex();
        LocationTree tree = new LocationTree(null, root, getPatientCount(root.uuid));
        LocationTree.putTreeIndex(root.uuid, tree);

        // With all locations initialized, recursively add children to the tree.
        addChildren(tree);

        return tree;
    }

    private void addChildren(LocationTree root) {
        if (!mLocationsByParent.containsKey(root.getLocation().uuid)) {
            return;
        }

        for (Location location : mLocationsByParent.get(root.getLocation().uuid)) {
            LocationTree childTree =
                    new LocationTree(root, location, getPatientCount(location.uuid));
            root.getChildren().put(location.uuid, childTree);
            LocationTree.putTreeIndex(location.uuid, childTree);
            addChildren(childTree);
        }
    }

    private int getPatientCount(String uuid) {
        if (!mPatientCountsMap.containsKey(uuid)) {
            return 0;
        }

        return mPatientCountsMap.get(uuid);
    }
}
