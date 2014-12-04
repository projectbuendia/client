package org.msf.records.model;

import android.content.Context;

import java.util.TreeMap;
import java.util.TreeSet;

import org.msf.records.net.model.Location;

/**
 * A LocationTree represents a tree of Locations, with each level of the tree sorted by the given
 * locale.
 */
public class LocationTree implements Comparable<LocationTree> {
    private final String DEFAULT_LOCALE = "en";

    private LocationTree mParent;

    private TreeMap<String, LocationTree> mChildren;
    private Location mLocation;
    private String mSortLocale = DEFAULT_LOCALE;
    private int mPatientCount;

    public LocationTree(LocationTree parent, Location location, int patientCount) {
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
        return getLocationArrayForDepth(context, root, 2);
    }

    public static LocationTree[] getZones(Context context, LocationTree root) {
        return getLocationArrayForDepth(context, root, 1);
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
}
