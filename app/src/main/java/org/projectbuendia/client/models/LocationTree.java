// Copyright 2015 The Project Buendia Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at: http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distrib-
// uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
// specific language governing permissions and limitations under the License.

package org.projectbuendia.client.models;

import android.database.ContentObserver;
import android.support.annotation.Nullable;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSortedSet;

import org.projectbuendia.client.utils.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A tree containing a hierarchy of {@link Location} objects, where the root is assumed to be a
 * single medical center.
 */
public class LocationTree implements Observable {

    public static final int ABSOLUTE_DEPTH_ROOT = 0;
    public static final int ABSOLUTE_DEPTH_ZONE = 1;
    public static final int ABSOLUTE_DEPTH_TENT = 2;
    public static final int ABSOLUTE_DEPTH_BED = 3;
    private static final Logger LOG = Logger.create();
    private final TypedCursor<Location> mCursor;
    private final Location mRoot;
    private final Map<String, Location> mUuidsToLocations;
    private final Map<String, Location> mUuidsToParents;
    private final ImmutableSetMultimap<String, Location> mUuidsToChildren;

    /**
     * Creates a {@link LocationTree} from a {@link TypedCursor} of {@link Location}s.
     * If there are no locations in the local database, the location tree will have a null
     * root node (i.e. getRoot() == null).
     * <p/>
     * <p>Callers must call {@link #close} when done with an instance of this class.
     * @throws IllegalArgumentException if the location tree contains multiple root nodes or if the
     *                                  the location tree has no root node or if the location tree
     *                                  contains any nodes whose parents are missing
     */
    public static LocationTree forTypedCursor(TypedCursor<Location> cursor) {
        Location root = null;
        Map<String, Location> uuidsToLocations = new HashMap<>();
        Map<String, Location> uuidsToParents = new HashMap<>();
        ImmutableSetMultimap.Builder<String, Location> uuidsToChildrenBuilder =
            ImmutableSetMultimap.builder();

        // First, create mappings from location UUIDs to the locations themselves and to their
        // children.
        for (Location location : cursor) {
            if (location.parentUuid == null) {
                if (root != null) {
                    LOG.w(
                        "Creating location tree with multiple root nodes. Both location '"
                            + root.name + "' (UUID '" + root.uuid + "') and location '"
                            + location.name + "' (UUID '" + location.uuid + "') have "
                            + "no parent nodes. The first location will be considered "
                            + "the root node.");
                }

                root = location;
            } else {
                uuidsToChildrenBuilder.put(location.parentUuid, location);
            }

            uuidsToLocations.put(location.uuid, location);
        }

        if (root == null) {
            LOG.w("Creating a location tree with no root node. This tree has no data.");

            return new LocationTree(
                cursor, null, uuidsToLocations, uuidsToParents, uuidsToChildrenBuilder.build());
        }

        // Then, create a mapping from location UUIDs to their parents.
        for (Location location : uuidsToLocations.values()) {
            if (location.parentUuid == null) continue;
            Location parent = uuidsToLocations.get(location.parentUuid);
            if (parent == null) {
                // TODO: Consider making this a warning rather than an exception.
                throw new IllegalArgumentException(
                    "Unable to create tree because a location's parent does not exist. "
                        + "Location '" + location.name + "' (UUID '" + location.uuid
                        + "' points to parent location with UUID '"
                        + location.parentUuid + "', which does not exist.");
            }

            uuidsToParents.put(location.uuid, parent);
        }

        return new LocationTree(
            cursor, root, uuidsToLocations, uuidsToParents, uuidsToChildrenBuilder.build());
    }

    @Nullable
    public Location getRoot() {
        return mRoot;
    }

    /**
     * Returns all immediate children of a given {@link Location}, or an empty set if the
     * {@link Location} is null or has no children.
     */
    public ImmutableSet<Location> getChildren(@Nullable Location location) {
        if (location == null) {
            return ImmutableSet.of();
        }

        ImmutableSet<Location> children = mUuidsToChildren.get(location.uuid);
        return children == null ? ImmutableSet.<Location> of() : children;
    }

    /**
     * Returns the sorted descendants of the root location at the specified absolute depth.
     * <p/>
     * <p>The named values {@link #ABSOLUTE_DEPTH_ROOT}, {@link #ABSOLUTE_DEPTH_ZONE},
     * {@link #ABSOLUTE_DEPTH_TENT}, and {@link #ABSOLUTE_DEPTH_BED} can be used for the
     * {@code level} parameter.
     */
    public ImmutableSortedSet<Location> getDescendantsAtDepth(int absoluteDepth) {
        return getDescendantsAtDepth(mRoot, absoluteDepth);
    }

    /**
     * Returns the sorted descendants of the specified location at the specified depth relative to
     * that location.
     */
    public ImmutableSortedSet<Location> getDescendantsAtDepth(
        Location location, int relativeDepth) {
        if (location == null) {
            return ImmutableSortedSet.of();
        }

        if (relativeDepth == 0) {
            ImmutableSortedSet.Builder<Location> thisLocationSet =
                ImmutableSortedSet.orderedBy(new LocationComparator(this));
            thisLocationSet.add(location);
            return thisLocationSet.build();
        }

        ImmutableSortedSet.Builder<Location> descendants =
            ImmutableSortedSet.orderedBy(new LocationComparator(this));
        for (Location child : getChildren(location)) {
            descendants.addAll(getDescendantsAtDepth(child, relativeDepth - 1));
        }

        return descendants.build();
    }

    /**
     * Returns a {@link List} representing a branch of {@link Location}s starting at the root
     * of the location tree and terminating at the given {@link Location}.
     */
    public List<Location> getAncestorsStartingFromRoot(Location node) {
        List<Location> result = new ArrayList<>();
        Location current = node;
        while (current != null) {
            result.add(current);
            current = getParent(current);
        }
        Collections.reverse(result);
        return result;
    }

    /** Returns the parent of a given {@link Location}. */
    @Nullable
    public Location getParent(@Nullable Location location) {
        if (location == null) {
            return null;
        }

        return mUuidsToParents.get(location.uuid);
    }

    @Nullable
    public Location findByUuid(String uuid) {
        return mUuidsToLocations.get(uuid);
    }

    /**
     * Returns a list of all AppLocations within a subtree rooted at the given {@link Location}.
     * @param subroot the Location that will form the root of the subtree
     * @return a List of AppLocations in a subtree with the given root
     */
    public List<Location> locationsInSubtree(Location subroot) {
        List<Location> result = new ArrayList<>();
        result.add(subroot);
        addChildrenToCollection(result, subroot);
        return result;
    }

    /** Returns the total number of patients in this location and its descendant locations. */
    public long getTotalPatientCount(Location location) {
        if (location == null) {
            return 0;
        }

        long count = location.patientCount;
        for (Location child : getChildren(location)) {
            count += getTotalPatientCount(child);
        }
        return count;
    }

    @Override public void registerContentObserver(ContentObserver observer) {
        mCursor.registerContentObserver(observer);
    }

    @Override public void unregisterContentObserver(ContentObserver observer) {
        mCursor.unregisterContentObserver(observer);
    }

    @Override public void close() {
        mCursor.close();
    }

    private LocationTree(
        TypedCursor<Location> cursor,
        Location root,
        Map<String, Location> uuidsToLocations,
        Map<String, Location> uuidsToParents,
        ImmutableSetMultimap<String, Location> uuidsToChildren) {
        mCursor = cursor;
        mRoot = root;
        mUuidsToLocations = uuidsToLocations;
        mUuidsToParents = uuidsToParents;
        mUuidsToChildren = uuidsToChildren;
    }

    private void addChildrenToCollection(Collection<Location> collection, Location root) {
        for (Location child : getChildren(root)) {
            collection.add(child);
            addChildrenToCollection(collection, child);
        }
    }
}
