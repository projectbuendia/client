package org.msf.records.data.app;

import android.support.annotation.Nullable;
import android.util.Log;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.concurrent.Immutable;

/**
 * A tree that contains all app model locations.
 */
@Immutable
public class AppLocationTree {

    private static final String TAG = AppLocationTree.class.getSimpleName();

    /**
     * Creates a {@link AppLocationTree} from a {@link TypedCursor} of {@link AppLocation}s.
     *
     * <p>This method closes the cursor once it finishes constructing a tree.
     *
     * @throws IllegalArgumentException if the location tree contains multiple root nodes or if the
     *                                  the location tree has no root node or if the location tree
     *                                  contains any nodes whose parents are missing
     */
    static AppLocationTree fromTypedCursor(TypedCursor<AppLocation> cursor) {
        AppLocation root = null;
        Map<String, AppLocation> uuidsToLocations = new HashMap<>();
        Map<String, AppLocation> uuidsToParents = new HashMap<>();
        ImmutableSetMultimap.Builder<String, AppLocation> uuidsToChildrenBuilder
                = ImmutableSetMultimap.builder();

        try {
            // First, create mappings from location UUIDs to the locations themselves and to their
            // children.
            for (AppLocation location : cursor) {
                if (location.parentUuid == null) {
                    if (root != null) {
                        Log.w(
                                TAG,
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
                throw new IllegalArgumentException("Unable to create a tree with no root node.");
            }

            // Then, create a mapping from location UUIDs to their parents.
            for (AppLocation location : uuidsToLocations.values()) {
                if (location.parentUuid == null) {
                    continue;
                }

                AppLocation parent = uuidsToLocations.get(location.parentUuid);
                if (parent == null) {
                    throw new IllegalArgumentException(
                            "Unable to create tree because a location's parent does not exist. "
                                    + "Location '" + location.name + "' (UUID '" + location.uuid
                                    + "' points to parent location with UUID '"
                                    + location.parentUuid + "', which does not exist.");
                }

                uuidsToParents.put(location.uuid, parent);
            }

            return new AppLocationTree(
                    root, uuidsToLocations, uuidsToParents, uuidsToChildrenBuilder.build());
        } finally {
            cursor.close();
        }
    }

    private final AppLocation mRoot;
    private final Map<String, AppLocation> mUuidsToLocations;
    private final Map<String, AppLocation> mUuidsToParents;
    private final ImmutableSetMultimap<String, AppLocation> mUuidsToChildren;

    private AppLocationTree(
            AppLocation root,
            Map<String, AppLocation> uuidsToLocations,
            Map<String, AppLocation> uuidsToParents,
            ImmutableSetMultimap<String, AppLocation> uuidsToChildren) {
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
        return mUuidsToParents.get(location.uuid);
    }

    @Nullable
    public ImmutableSet<AppLocation> getChildren(AppLocation location) {
        return mUuidsToChildren.get(location.uuid);
    }

    @Nullable
    public AppLocation findByUuid(String uuid) {
        return mUuidsToLocations.get(uuid);
    }
}
