package org.msf.records.model;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;

import org.msf.records.filter.AllFilter;
import org.msf.records.filter.FilterQueryProviderFactory;
import org.msf.records.filter.SimpleSelectionFilter;
import org.msf.records.net.model.Location;
import org.msf.records.sync.LocationProjection;
import org.msf.records.sync.LocationProviderContract;

import java.util.HashMap;
import java.util.Map;

/**
 * LocationTreeFactory constructs a LocationTree using a database Cursor and listens for changes
 * in location data.
 */
public class LocationTreeFactory implements LoaderManager.LoaderCallbacks<Cursor> {
    private final String TAG = "LocationTree";

    // TODO(akalachman): Consolidate loader ids.
    private final int LOCATION_LOADER_ID = 5;
    private final int LOCATION_NAMES_LOADER_ID = 6;

    private static final String TRANSACTION_ID_KEY = "transaction_id";

    private FilterQueryProviderFactory mLocationQueryFactory;
    private FilterQueryProviderFactory mLocationNamesQueryFactory;

    private SimpleSelectionFilter mLocationFilter = new AllFilter();
    private SimpleSelectionFilter mLocationNameFilter = new AllFilter();

    private Multimap<String, Location> mLocationsByParent;
    private Map<String, HashMap<String, String>> mLocationNamesMap;

    private final Context mContext;
    private final LoaderManager mLoaderManager;
    private final OnTreeConstructedListener mOnTreeConstructedListener;
    private final OnTreeConstructionErrorListener mOnTreeConstructionErrorListener;

    private boolean mRefreshingLocations = false;
    private boolean mRefreshingLocationNames = false;

    public enum ErrorCode {
        NO_ROOT_LOCATION_ERROR, LOCATION_FETCH_ERROR, LOCATION_NAME_FETCH_ERROR
    };

    /**
     * OnTreeConstructedListeners are notified when a LocationTree is built.
     */
    public interface OnTreeConstructedListener {
        public void onTreeConstructed(LocationTree tree);
    }

    /**
     * OnTreeConstructionErrorListeners are notified when a LocationTree could not be built.
     */
    public interface OnTreeConstructionErrorListener {
        public void onTreeConstructionError(ErrorCode errorCode);
    }

    public LocationTreeFactory(
            Context context,
            LoaderManager loaderManager,
            OnTreeConstructedListener onTreeConstructedListener,
            OnTreeConstructionErrorListener onTreeConstructionErrorListener) {
        mContext = context;
        mLoaderManager = loaderManager;
        mOnTreeConstructedListener = onTreeConstructedListener;
        mOnTreeConstructionErrorListener = onTreeConstructionErrorListener;

        mLocationQueryFactory = new FilterQueryProviderFactory();
        mLocationQueryFactory.setUri(LocationProviderContract.LOCATIONS_CONTENT_URI);
        mLocationQueryFactory.setSortClause(null);
        mLocationQueryFactory.setProjection(LocationProjection.getLocationProjection());

        mLocationNamesQueryFactory = new FilterQueryProviderFactory();
        mLocationNamesQueryFactory.setUri(LocationProviderContract.LOCATION_NAMES_CONTENT_URI);
        mLocationNamesQueryFactory.setSortClause(null);
        mLocationNamesQueryFactory.setProjection(LocationProjection.getLocationNamesProjection());

        mLocationsByParent = HashMultimap.create();
        mLocationNamesMap = new HashMap<String, HashMap<String, String>>();

        mLoaderManager.restartLoader(LOCATION_LOADER_ID, null, this);
        mLoaderManager.restartLoader(LOCATION_NAMES_LOADER_ID, null, this);
    }

    public void setLocationFilter(SimpleSelectionFilter locationFilter) {
        mLocationFilter = locationFilter;
    }

    public void setLocationNameFilter(SimpleSelectionFilter locationNameFilter) {
        mLocationNameFilter = locationNameFilter;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        switch (i) {
            case LOCATION_LOADER_ID:
                synchronized (this) {
                    mRefreshingLocations = true;
                    return mLocationQueryFactory.getCursorLoader(mContext, mLocationFilter, "");
                }
            case LOCATION_NAMES_LOADER_ID:
                synchronized (this) {
                    mRefreshingLocationNames = true;
                    return mLocationNamesQueryFactory.getCursorLoader(
                            mContext, mLocationNameFilter, "");
                }
            default:
                Log.w(TAG, "Unexpected loader id: " + i);
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        int id = cursorLoader.getId();

        switch (id) {
            case LOCATION_LOADER_ID:
                mRefreshingLocations = false;
                buildLocationMap(cursor);
                break;
            case LOCATION_NAMES_LOADER_ID:
                mRefreshingLocationNames = false;
                buildLocationNamesMap(cursor);
                break;
        }

        if (cursor != null) {
            cursor.close();
        }

        if (!mRefreshingLocations && !mRefreshingLocationNames) {
            LocationTree locationTree = buildTree();
            if (locationTree == null) {
                mOnTreeConstructionErrorListener.onTreeConstructionError(
                        ErrorCode.NO_ROOT_LOCATION_ERROR);
            } else {
                mOnTreeConstructedListener.onTreeConstructed(locationTree);
            }
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
        LocationTree tree = new LocationTree(root);

        // With all locations initialized, recursively add children to the tree.
        addChildren(tree);

        return tree;
    }

    private void addChildren(LocationTree root) {
        if (!mLocationsByParent.containsKey(root.getLocation().uuid)) {
            return;
        }

        for (Location location : mLocationsByParent.get(root.getLocation().uuid)) {
            LocationTree childTree = new LocationTree(location);
            root.getChildren().put(location.uuid, childTree);
            addChildren(childTree);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        int id = cursorLoader.getId();

        switch (id) {
            case LOCATION_LOADER_ID:
                mOnTreeConstructionErrorListener.onTreeConstructionError(
                        ErrorCode.LOCATION_FETCH_ERROR);
                break;
            case LOCATION_NAMES_LOADER_ID:
                mOnTreeConstructionErrorListener.onTreeConstructionError(
                        ErrorCode.LOCATION_NAME_FETCH_ERROR);
                break;
        }
    }
}
