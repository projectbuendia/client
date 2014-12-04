package org.msf.records.model;

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

    public LocationTree(LocationTree parent, Location location) {
        mParent = parent;
        mLocation = location;
        mChildren = new TreeMap<String, LocationTree>();
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

    @Override
    public String toString() {
        if (!mLocation.names.containsKey(mSortLocale)) {
            return "";
        }

        return mLocation.names.get(mSortLocale);
    }

    @Override
    public int compareTo(LocationTree another) {
        if (!mLocation.names.containsKey(mSortLocale) ||
                !another.getLocation().names.containsKey(mSortLocale)) {
            return 0;
        }

        return mLocation.names.get(mSortLocale).compareTo(
                another.getLocation().names.get(mSortLocale));
    }
}
