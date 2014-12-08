package org.msf.records.location;

import android.content.Context;

import org.msf.records.model.Zone;
import org.msf.records.net.model.Location;

import java.util.Collection;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * A LocationTree represents a tree of Locations, with each level of the tree sorted by the given
 * locale.
 *
 * LocationTree should be used as a singleton.
 */
public class LocationTree implements Comparable<LocationTree> {
    private final String DEFAULT_LOCALE = "en";

    private LocationTree mParent;

    // Keep an index of all children to make grabbing an arbitrary location straightforward.
    private static HashMap<String, LocationTree> mAllChildren = new HashMap<String, LocationTree>();
    private TreeMap<String, LocationTree> mChildren;
    private Location mLocation;
    private String mSortLocale = DEFAULT_LOCALE;
    private int mPatientCount;

    private static final String TAG = "LocationTree";

    public static final int FACILITY_DEPTH = 0;
    public static final int ZONE_DEPTH = 1;
    public static final int TENT_DEPTH = 2;
    public static final int BED_DEPTH = 3;

    public static void clearTreeIndex() {
        mAllChildren.clear();
    }

    public static void putTreeIndex(String uuid, LocationTree locationTree) {
        mAllChildren.put(uuid, locationTree);
    }

    public static LocationTree getLocationForUuid(String uuid) {
        return mAllChildren.get(uuid);
    }

    public static LocationTree getZoneForUuid(String uuid) {
        LocationTree locationTree = getLocationForUuid(uuid);
        if (locationTree == null) {
            return null;
        }

        return locationTree.getAncestorOrThisWithDepth(ZONE_DEPTH);
    }

    public static LocationTree getTentForUuid(String uuid) {
        LocationTree locationTree = getLocationForUuid(uuid);
        if (locationTree == null) {
            return null;
        }

        return locationTree.getAncestorOrThisWithDepth(TENT_DEPTH);
    }

    // Limit location tree construction to this package.
    LocationTree(LocationTree parent, Location location, int patientCount) {
        mParent = parent;
        mLocation = location;
        mChildren = new TreeMap<String, LocationTree>();
        mPatientCount = patientCount;
    }

    public int getPatientCount() {
        int patientCount = mPatientCount;
        for (LocationTree child : mChildren.values()) {
            patientCount += child.getPatientCount();
        }
        return patientCount;
    }

    public LocationTree getAncestorOrThisWithDepth(int depth) {
        int myDepth = getDepth();
        int remainingDistance = myDepth - depth;

        LocationTree ancestor = this;
        while (remainingDistance > 0) {
            ancestor = ancestor.getParent();
            remainingDistance--;
        }

        return ancestor;
    }

    private int getDepth() {
        LocationTree tree = this;
        int depth = 0;
        while (tree.getParent() != null) {
            tree = tree.getParent();
            depth++;
        }
        return depth;
    }

    public LocationTree getParent() {
        return mParent;
    }

    public Location getLocation() {
        return mLocation;
    }

    public void setSortLocale(String sortLocale) {
        mSortLocale = sortLocale;
    }

    public TreeMap<String, LocationTree> getChildren() {
        return mChildren;
    }

    public TreeSet<LocationTree> getLocationsForDepth(int depth) {
        TreeSet<LocationTree> locations = new TreeSet<LocationTree>();
        if (depth == 0) {
            locations.add(this);
            return locations;
        }

        for (LocationTree childLocation : mChildren.values()) {
            locations.addAll(childLocation.getLocationsForDepth(depth - 1));
        }

        return locations;
    }

    public static LocationTree[] getTents(Context context, LocationTree root) {
        return getLocationArrayForDepth(context, root, TENT_DEPTH);
    }

    public static LocationTree[] getZones(Context context, LocationTree root) {
        return getLocationArrayForDepth(context, root, ZONE_DEPTH);
    }

    private static LocationTree[] getLocationArrayForDepth(
            Context context, LocationTree root, int depth) {
        TreeSet<LocationTree> locationTrees;
        if (root == null) {
            locationTrees = new TreeSet<LocationTree>();
        } else {
            locationTrees = root.getLocationsForDepth(depth);
        }

        LocationTree[] locationTreeArray = new LocationTree[locationTrees.size()];
        locationTrees.toArray(locationTreeArray);
        return locationTreeArray;
    }

    @Override
    public String toString() {
        if (!mLocation.names.containsKey(mSortLocale)) {
            return "";
        }

        return mLocation.names.get(mSortLocale);
    }

    @Override
    public int compareTo(LocationTree another) {
        // Short-circuit -- if these two LocationTrees are the same object, we're done.
        if (this == another) {
            return 0;
        }

        // On the off-chance that the other location is at a different depth, prefer
        // locations at a smaller depth (facility > zone > tent > bed).
        Integer depth = getDepth();
        Integer anotherDepth = another.getDepth();
        int depthComparison = depth.compareTo(anotherDepth);
        if (depthComparison != 0) {
            return depthComparison;
        }

        // Zone order takes precedence, but a value of 0 means that one or both locations
        // is not a zone.
        int zoneComparison = Zone.compareTo(getLocation(), another.getLocation());
        if (zoneComparison != 0) {
            return zoneComparison;
        }

        // Parent order is the next precedent (e.g. tents should be sorted by zone).
        if (getParent() != null && another.getParent() != null) {
            int parentComparison = getParent().compareTo(another.getParent());
            if (parentComparison != 0) {
                return parentComparison;
            }
        }

        // If neither location is a zone and there is no name for one or both locations in this
        // locale, return equal as we don't know how to compare them.
        if (!mLocation.names.containsKey(mSortLocale) ||
                !another.getLocation().names.containsKey(mSortLocale)) {
            return 0;
        }

        // Compare using the current locale.
        return mLocation.names.get(mSortLocale).compareTo(
                another.getLocation().names.get(mSortLocale));
    }

    public LocationTree[] getSubtreeLocationArray() {
        TreeMap<String, LocationTree> subtreeLocations = getAllSubtreeLocations();
        LocationTree[] locationArray = new LocationTree[subtreeLocations.size()];
        subtreeLocations.values().toArray(locationArray);
        return locationArray;
    }

    public TreeMap<String, LocationTree> getAllSubtreeLocations() {
        TreeMap<String, LocationTree> subtreeLocations = new TreeMap<String, LocationTree>();
        subtreeLocations.put(getLocation().uuid, this);
        for (LocationTree subtree : getChildren().values()) {
            subtreeLocations.putAll(subtree.getAllSubtreeLocations());
        }
        return subtreeLocations;
    }

    // TODO(akalachman): Cache this or get rid of it once data model is refactored.
    public static String getLocationSortClause(final String fieldName) {
        if (mAllChildren.size() == 0) {
            return "";
        }

        Collection<LocationTree> allLocations = mAllChildren.values();
        TreeSet<LocationTree> sortedLocations = new TreeSet<LocationTree>();
        sortedLocations.addAll(allLocations);

        StringBuilder sb = new StringBuilder(" CASE ");
        sb.append(fieldName);
        int i = 0;
        for (LocationTree tree : sortedLocations) {
            sb.append(" WHEN '");
            sb.append(tree.getLocation().uuid);
            sb.append("' THEN ");
            sb.append(i);
            i++;
        }
        sb.append(" END ");
        return sb.toString();
    }
}
