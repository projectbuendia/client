package org.msf.records.data.app;

import android.database.ContentObserver;
import android.support.annotation.Nullable;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;

import java.util.ArrayList;
import java.util.Collection;
import org.msf.records.utils.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A tree that contains all app model locations.
 */
public class AppLocationTree implements AppModelObservable {

    private static final Logger LOG = Logger.create();

    public static final int ABSOLUTE_DEPTH_ROOT = 0;
    public static final int ABSOLUTE_DEPTH_ZONE = 1;
    public static final int ABSOLUTE_DEPTH_TENT = 2;
    public static final int ABSOLUTE_DEPTH_BED = 3;

    /**
     * Creates a {@link AppLocationTree} from a {@link TypedCursor} of {@link AppLocation}s.
     *
     * <p>Callers must call {@link #close} when done with an instance of this class.
     *
     * @throws IllegalArgumentException if the location tree contains multiple root nodes or if the
     *                                  the location tree has no root node or if the location tree
     *                                  contains any nodes whose parents are missing
     */
    public static AppLocationTree forTypedCursor(TypedCursor<AppLocation> cursor) {
        AppLocation root = null;
        Map<String, AppLocation> uuidsToLocations = new HashMap<>();
        Map<String, AppLocation> uuidsToParents = new HashMap<>();
        ImmutableSetMultimap.Builder<String, AppLocation> uuidsToChildrenBuilder =
                ImmutableSetMultimap.builder();

        // First, create mappings from location UUIDs to the locations themselves and to their
        // children.
        for (AppLocation location : cursor) {
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

            return new AppLocationTree(
                    cursor, null, uuidsToLocations, uuidsToParents, uuidsToChildrenBuilder.build());
        }

        // Then, create a mapping from location UUIDs to their parents.
        for (AppLocation location : uuidsToLocations.values()) {
            if (location.parentUuid == null) {
                continue;
            }
            AppLocation parent = uuidsToLocations.get(location.parentUuid);
            if (parent == null) {
                // TODO(dxchen): Consider making this a warning rather than an exception.
                throw new IllegalArgumentException(
                        "Unable to create tree because a location's parent does not exist. "
                                + "Location '" + location.name + "' (UUID '" + location.uuid
                                + "' points to parent location with UUID '"
                                + location.parentUuid + "', which does not exist.");
            }

            uuidsToParents.put(location.uuid, parent);
        }

        return new AppLocationTree(
                cursor, root, uuidsToLocations, uuidsToParents, uuidsToChildrenBuilder.build());
    }

    private final TypedCursor<AppLocation> mCursor;
    private final AppLocation mRoot;
    private final Map<String, AppLocation> mUuidsToLocations;
    private final Map<String, AppLocation> mUuidsToParents;
    private final ImmutableSetMultimap<String, AppLocation> mUuidsToChildren;

    private AppLocationTree(
            TypedCursor<AppLocation> cursor,
            AppLocation root,
            Map<String, AppLocation> uuidsToLocations,
            Map<String, AppLocation> uuidsToParents,
            ImmutableSetMultimap<String, AppLocation> uuidsToChildren) {
        mCursor = cursor;
        mRoot = root;
        mUuidsToLocations = uuidsToLocations;
        mUuidsToParents = uuidsToParents;
        mUuidsToChildren = uuidsToChildren;
    }

    @Nullable
    public AppLocation getRoot() {
        return mRoot;
    }

    @Nullable
    public AppLocation getParent(AppLocation location) {
        if (location == null) {
            return null;
        }

        return mUuidsToParents.get(location.uuid);
    }

    public ImmutableSet<AppLocation> getChildren(AppLocation location) {
        if (location == null) {
            return ImmutableSet.of();
        }

        ImmutableSet<AppLocation> children = mUuidsToChildren.get(location.uuid);
        return children == null ? ImmutableSet.<AppLocation>of() : children;
    }

    /**
     * Returns the descendants of the root location at the specified absolute depth.
     *
     * <p>The named values {@link #ABSOLUTE_DEPTH_ROOT}, {@link #ABSOLUTE_DEPTH_ZONE},
     * {@link #ABSOLUTE_DEPTH_TENT}, and {@link #ABSOLUTE_DEPTH_BED} can be used for the
     * {@code level} parameter.
     */
    public ImmutableSet<AppLocation> getDescendantsAtDepth(int absoluteDepth) {
        return getDescendantsAtDepth(mRoot, absoluteDepth);
    }

    /**
     * Returns the descendants of the specified location at the specified depth relative to that
     * location.
     */
    public ImmutableSet<AppLocation> getDescendantsAtDepth(
            AppLocation location, int relativeDepth) {
        if (location == null) {
            return ImmutableSet.of();
        }

        if (relativeDepth == 0) {
            return ImmutableSet.of(location);
        }

        ImmutableSet.Builder<AppLocation> descendants = ImmutableSet.builder();
        for (AppLocation child : getChildren(location)) {
            descendants.addAll(getDescendantsAtDepth(child, relativeDepth - 1));
        }

        return descendants.build();
    }

    @Nullable
    public AppLocation findByUuid(String uuid) {
        return mUuidsToLocations.get(uuid);
    }

    /**
     * Returns a list of all AppLocations within a subtree rooted at the given
     * {@link org.msf.records.data.app.AppLocation}.
     * @param subroot the AppLocation that will form the root of the subtree
     * @return a List of AppLocations in a subtree with the given root
     */
    public List<AppLocation> locationsInSubtree(AppLocation subroot) {
        List<AppLocation> result = new ArrayList<>();
        result.add(subroot);
        addChildren(result, subroot);
        return result;
    }

    private void addChildren(Collection<AppLocation> collection, AppLocation root) {
        for (AppLocation child : getChildren(root)) {
            collection.add(child);
            addChildren(collection, child);
        }
    }

    /**
     * Returns the total number of patients in this location and its descendant locations.
     */
    public int getTotalPatientCount(AppLocation location) {
        if (location == null) {
            return 0;
        }

        int count = location.patientCount;
        for (AppLocation child : getChildren(location)) {
            count += getTotalPatientCount(child);
        }
        return count;
    }

    @Override
    public void registerContentObserver(ContentObserver observer) {
        mCursor.registerContentObserver(observer);
    }

    @Override
    public void unregisterContentObserver(ContentObserver observer) {
        mCursor.unregisterContentObserver(observer);
    }

    @Override
    public void close() {
        mCursor.close();
    }
}
