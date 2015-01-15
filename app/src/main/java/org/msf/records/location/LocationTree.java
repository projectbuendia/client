package org.msf.records.location;

import android.content.res.Resources;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.msf.records.R;
import org.msf.records.model.Zone;
import org.msf.records.net.model.Location;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;

/**
 * A LocationTree represents a tree of Locations, with each level of the tree sorted by the given
 * locale.
 */
public final class LocationTree {

    // TODO(rjlothian): Fix all code that depends on having a singleton and rRemove this.
    @Nullable public static LocationTree singletonInstance;

    public static final int FACILITY_DEPTH = 0;
    public static final int ZONE_DEPTH = 1;
    public static final int TENT_DEPTH = 2;
    public static final int BED_DEPTH = 3;

    public final class LocationSubtree implements Comparable<LocationSubtree> {
		private Location mLocation;
	    private int mPatientCount;
	    private final TreeMap<String, LocationSubtree> mChildren = new TreeMap<>();
        private Comparator<LocationSubtree> mComparator = new SubtreeComparator();

	    public Location getLocation() {
	    	return mLocation;
	    }

	    public int getPatientCount() {
	        int patientCount = mPatientCount;
	        for (LocationSubtree child : mChildren.values()) {
	            patientCount += child.getPatientCount();
	        }
	        return patientCount;
	    }

        public void incrementPatientCount() {
            mPatientCount++;
        }

        public void decrementPatientCount() {
            mPatientCount--;
        }

        public List<LocationSubtree> thisAndAllDescendents() {
	    	List<LocationSubtree> result = new ArrayList<>();
	    	addToCollectionRecursively(result);
	    	return result;
	    }

	    private void addToCollectionRecursively(Collection<LocationSubtree> collection) {
	    	collection.add(this);
	    	for (LocationSubtree child : mChildren.values()) {
	    		child.addToCollectionRecursively(collection);
	    	}
	    }

	    @Override
	    public String toString() {
            String mLocale = DEFAULT_LOCALE;
            if (mLocation == null
	        		|| mLocation.names == null
	        		|| mLocation.names.get(mLocale) == null) {
	            // This location is null, try to recover.
	            int depth = getDepthOfSubtree(this);
	            switch (depth) {
	                case ZONE_DEPTH:
	                    return mResources.getString(R.string.unknown_zone);
	                case TENT_DEPTH:
	                	LocationSubtree parent = getParent(this);
	                    if (parent == null) {
	                        return mResources.getString(R.string.unknown_tent);
	                    } else {
	                        return mResources.getString(
	                                R.string.unknown_tent_in_zone,
	                                parent.toString());
	                    }
	                default:
	                    return mResources.getString(R.string.unknown_location);
	            }
	        }

	        return mLocation.names.get(mLocale);
	    }

        @Override
        public int compareTo(LocationSubtree another) {
            return mComparator.compare(this, another);
        }
    }

    private static final String DEFAULT_LOCALE = "en";

    private final Resources mResources;
    private final Map<String, LocationSubtree> mUuidToSubtree = new HashMap<>();
    private final LocationSubtree mTreeRoot;

    /**
     * @param patientCountByUuid gives the number of patients at each location, specified by UUID.
     *        This excludes any patients contained with a smaller location within that location.
     */
    public LocationTree(
    		Resources resources,
    		Collection<Location> locations,
    	    Map<String, Integer> patientCountByUuid) {

    	mResources = resources;
    	Location root = null;
    	Multimap<String, Location> locationByParentUuid = ArrayListMultimap.create();
    	for (Location location : locations) {
    		if (location.parent_uuid == null) {
    			Preconditions.checkArgument(root == null, "Should only have one root (parent_uuid null) element");
    			root = location;
    		}
			locationByParentUuid.put(location.parent_uuid, location);
    	}
    	Preconditions.checkArgument(root != null, "Should have a root (parent_uuid null) element");

    	mTreeRoot = new LocationSubtree();
    	mTreeRoot.mLocation = root;
    	mTreeRoot.mPatientCount = getOrZeroIfMissing(patientCountByUuid, root.uuid);


        // Recursively add children to the tree.
        addChildren(mTreeRoot, locationByParentUuid, patientCountByUuid);

        populateMap(mTreeRoot, mUuidToSubtree);
    }

    public LocationSubtree getRoot() {
    	return mTreeRoot;
    }

    private static void populateMap(LocationSubtree subtree, Map<String, LocationSubtree> uuidToSubtree) {
    	uuidToSubtree.put(subtree.mLocation.uuid, subtree);
    	for (LocationSubtree child : subtree.mChildren.values()) {
    		populateMap(child, uuidToSubtree);
    	}
    }

    @Nullable private LocationSubtree getParent(LocationSubtree subtree) {
    	return mUuidToSubtree.get(subtree.mLocation.parent_uuid);
    }

    private void addChildren(
    		LocationSubtree root,
    		Multimap<String, Location> locationByParentUuid,
    	    Map<String, Integer> patientCountByUuid) {
        for (Location location : locationByParentUuid.get(root.mLocation.uuid)) {
            LocationSubtree subtree = new LocationSubtree();
            subtree.mLocation = location;
            subtree.mPatientCount = getOrZeroIfMissing(patientCountByUuid, location.uuid);
            root.mChildren.put(location.uuid, subtree);
            addChildren(subtree, locationByParentUuid, patientCountByUuid);
        }
    }

    @Nullable public LocationSubtree getLocationByUuid(String uuid) {
    	return mUuidToSubtree.get(uuid);
    }

    /**
     * Returns the zone containing the given location UUID, or null if no such zone exists.
     */
    @Nullable public LocationSubtree getZoneForUuid(String uuid) {
        LocationSubtree locationSubtree = mUuidToSubtree.get(uuid);
        if (locationSubtree == null) {
            return null;
        }
        return getAncestorOrThisWithDepth(locationSubtree, ZONE_DEPTH);
    }

    /**
     * Returns the tent containing the given location UUID, or null if no such tent exists.
     */
    @Nullable public LocationSubtree getTentForUuid(String uuid) {
    	LocationSubtree locationSubtree = mUuidToSubtree.get(uuid);
        if (locationSubtree == null) {
            return null;
        }
        return getAncestorOrThisWithDepth(locationSubtree, TENT_DEPTH);
    }

    @Nullable private LocationSubtree getAncestorOrThisWithDepth(LocationSubtree startItem, int depth) {
    	Deque<LocationSubtree> ancestorStack = new ArrayDeque<>();
    	@Nullable LocationSubtree currentItem = startItem;
    	while (currentItem != null) {
    		ancestorStack.push(currentItem);
    		currentItem = mUuidToSubtree.get(currentItem.mLocation.parent_uuid);
    	}
    	int currentDepth = 0;
    	while (!ancestorStack.isEmpty()) {
    		currentItem = ancestorStack.pop();
    		if (currentDepth == depth) {
    			return currentItem;
    		}
    		currentDepth++;
    	}
    	return null;
	}

    /**
     * Returns the whole tree as a map from parent UUID to subtree.
     */
    public TreeMap<String, LocationSubtree> getChildren() {
        TreeMap<String, LocationSubtree> result = new TreeMap<>();
        result.putAll(mUuidToSubtree);
        return result;
    }

    /**
     * Returns a list of all locations with the given depth in the tree, ordered by the
     * standard location ordering.
     */
    public List<LocationSubtree> getLocationsForDepth(int depth) {
        ImmutableList<LocationSubtree> currentLevel = ImmutableList.of(mTreeRoot);
        for (int currentDepth = 0; currentDepth < depth; currentDepth++) {
        	ImmutableList.Builder<LocationSubtree> newLevel = ImmutableList.builder();
        	for (LocationSubtree item : currentLevel) {
        		newLevel.addAll(item.mChildren.values());
        	}
        	currentLevel = newLevel.build();
        }
        return Ordering.from(new SubtreeComparator()).sortedCopy(currentLevel);
    }

    /**
     * Returns a list of the tents in the tree, ordered by the standard location ordering.
     */
    public List<LocationSubtree> getTents() {
        return getLocationsForDepth(TENT_DEPTH);
    }

    /**
     * Returns a list of the zones in the tree, ordered by {@link Zone#compareTo(Location, Location)}.
     */
    public List<LocationSubtree> getZones() {
        return getLocationsForDepth(ZONE_DEPTH);
    }

    private int getDepthOfSubtree(LocationSubtree subtree) {
    	int depth = 0;
    	for (;;) {
    		subtree = mUuidToSubtree.get(subtree.mLocation.parent_uuid);
    		if (subtree == null) {
    			return depth;
    		}
    		depth++;
    	}
    }

//    @Override
//    public String toString() {
//        if (mLocation == null || mLocation.names == null ||
//                !mLocation.names.containsKey(mSortLocale)) {
//            Resources resources = App.getInstance().getResources();
//
//            // This location is null, try to recover.
//            int depth = getDepth();
//            switch (depth) {
//                case ZONE_DEPTH:
//                    return resources.getString(R.string.unknown_zone);
//                case TENT_DEPTH:
//                    LocationTree parent = getParent();
//                    if (parent == null) {
//                        return resources.getString(R.string.unknown_tent);
//                    } else {
//                        return resources.getString(
//                                R.string.unknown_tent_in_zone, getParent().toString());
//                    }
//                default:
//                    return resources.getString(R.string.unknown_location);
//            }
//        }
//
//        return mLocation.names.get(mSortLocale);
//    }

    private List<LocationSubtree> getAncestorsStartingFromRoot(LocationSubtree node) {
		List<LocationSubtree> result = new ArrayList<>();
		LocationSubtree current = node;
		while (current != null) {
			result.add(current);
			current = mUuidToSubtree.get(current.mLocation.parent_uuid);
		}
		Collections.reverse(result);
		return result;
    }

    public final class SubtreeComparator implements Comparator<LocationSubtree> {
        /**
         * Compares two objects that may each be Integer or String.  All Integers
         * sort before all Strings; Integers compare according to their numeric
         * value and Strings compare according to their string value.
         */
        private int compareObjects(Object a, Object b) {
            if (a instanceof Integer && b instanceof String) return -1;
            if (a instanceof String && b instanceof Integer) return 1;
            if (a instanceof Integer && b instanceof Integer) {
                return (Integer) a - (Integer) b;
            }
            if (a instanceof String && b instanceof String) {
                return ((String) a).compareTo((String) b);
            }
            throw new IllegalArgumentException("arguments must be Numbers or Strings");
        }

        /**
         * Compares two lists lexicographically by element, just like Python does.
         */
        private int compareLists(List<Object> a, List<Object> b) {
            int i;
            for (i = 0; i < a.size() && i < b.size(); i++) {
                int compare = compareObjects(a.get(i), b.get(i));
                if (compare != 0) return compare;
            }
            if (i < a.size()) return -1;
            if (i < b.size()) return 1;
            return 0;
        }

        /**
         * Turns a string into an array of Numbers (from sequences of digits) and
         * Strings (from sequences of letters).  Other characters are ignored.
         */
        private List<Object> getParts(String str) {
            Pattern numberOrWord = Pattern.compile("[0-9]+|[a-zA-Z]+");
            Matcher matcher = numberOrWord.matcher(str == null ? "" : str);
            List<Object> parts = new ArrayList<>();
            for (int pos = 0; matcher.find(pos); pos = matcher.end()) {
                String part = matcher.group();
                parts.add(part.matches("\\d.*") ? Integer.valueOf(part) : part);
            }
            return parts;
        }

    	@Override
    	public int compare(LocationSubtree lhs, LocationSubtree rhs) {
    		if (lhs == rhs) return 0;

    		List<LocationSubtree> pathA = getAncestorsStartingFromRoot(lhs);
    		List<LocationSubtree> pathB = getAncestorsStartingFromRoot(rhs);
    		for (int i = 0;; i++) {
    			if (i >= pathA.size() && i >= pathB.size()) {
    				return 0;
    			} else if (i >= pathA.size()) {
    				return -1;
    			} else if (i >= pathB.size()) {
    				return 1;
    			} else {
    				LocationSubtree subtreeA = pathA.get(i);
    				LocationSubtree subtreeB = pathB.get(i);

    				int compare;
	    			if (i == ZONE_DEPTH) {
	    				compare = Zone.compareTo(subtreeA.getLocation(), subtreeB.getLocation());
	    			} else {
                        String nameA = subtreeA.mLocation.names.get(DEFAULT_LOCALE);
                        String nameB = subtreeB.mLocation.names.get(DEFAULT_LOCALE);
                        compare = compareLists(getParts(nameA), getParts(nameB));
	    			}
	    			if (compare != 0) {
	    				return compare;
	    			}
    			}
    		}
    	}
    }

    /**
     * Returns {@code map.get(key)} or 0 if no entry for that key exists.
     */
    private static <T> int getOrZeroIfMissing(Map<T, Integer> map, T key) {
    	if (map.containsKey(key)) {
    		return map.get(key);
    	} else {
    		return 0;
    	}
    }
}
