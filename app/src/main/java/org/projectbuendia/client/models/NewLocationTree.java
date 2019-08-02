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

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.Nullable;

import com.google.common.collect.ImmutableList;

import org.projectbuendia.client.providers.Contracts;
import org.projectbuendia.client.utils.Receiver;
import org.projectbuendia.client.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import static org.projectbuendia.client.utils.Utils.eq;

/** An ordered hierarchy of locations with their localized names. */
public class NewLocationTree {
    private static ContentResolver resolver = null;

    private static NewLocationTree loadedTree = null;
    private static String requestedLocale = null;

    private final NewLocation[] locations;
    private final Map<String, NewLocation> locationsByUuid = new HashMap<>();
    private final Map<String, String> parentUuidsByUuid = new HashMap<>();
    private final Set<String> nonleafLocations = new HashSet<>();

    private final Object patientCountLock = new Object();
    private final Map<String, Integer> numPatientsAtNode = new HashMap<>();
    private final Map<String, Integer> numPatientsInSubtree = new HashMap<>();
    private int totalNumPatients;

    public static void load(ContentResolver newResolver, String locale, Receiver<NewLocationTree> receiver) {
        synchronized (NewLocationTree.class) {
            if (loadedTree != null && eq(requestedLocale, locale)) {
                receiver.receive(loadedTree);
            }
        }
        // Utils.runInBackground(() -> loadTree(newResolver, locale), receiver);
        receiver.receive(loadTree(newResolver, locale));
    }

    public static void invalidate() {
        synchronized (NewLocationTree.class) {
            resolver = null;
            requestedLocale = null;
        }
    };

    private static NewLocationTree loadTree(ContentResolver newResolver, String locale) {
        synchronized (NewLocationTree.class) {
            resolver = newResolver;
            loadedTree = null;
            requestedLocale = locale;

            Uri uri = Contracts.getLocalizedLocationsUri(locale);
            try (Cursor cursor = resolver.query(uri, null, null, null, null)) {
                loadedTree = new NewLocationTree(cursor, locale);
            }
            return loadedTree;
        }
    }

    public NewLocationTree(Cursor cursor, String locale) {
        List<String> uuids = new ArrayList<>();
        Map<String, String> namesByUuid = new HashMap<>();
        Map<String, String> shortIdsByUuid = new HashMap<>();
        totalNumPatients = 0;

        while (!cursor.isLast() && !cursor.isAfterLast()) {
            cursor.moveToNext();

            String uuid = Utils.getString(cursor, Contracts.LocalizedLocations.UUID);
            String parentUuid = Utils.getString(cursor, Contracts.LocalizedLocations.PARENT_UUID);
            String name = Utils.getString(cursor, Contracts.LocalizedLocations.NAME);
            int numPatients = Utils.getInt(cursor, Contracts.LocalizedLocations.PATIENT_COUNT, 0);

            uuids.add(uuid);
            parentUuidsByUuid.put(uuid, parentUuid);
            namesByUuid.put(uuid, name);
            numPatientsAtNode.put(uuid, numPatients);
            numPatientsInSubtree.put(uuid, 0);  // counts will be added below
            totalNumPatients += numPatients;
            if (parentUuid != null) {
                nonleafLocations.add(parentUuid);
            }
        }

        // Sort into a global ordering that is consistent with the ordering
        // we want for any selected group of siblings.
        Collections.sort(uuids, (a, b) -> Utils.ALPHANUMERIC_COMPARATOR.compare(
            namesByUuid.get(a), namesByUuid.get(b)
        ));

        // Use this global ordering to assign each item a short, uniform-length ID.
        int len = String.valueOf(uuids.size()).length();
        String format = "%0" + len + "d";
        for (int i = 0; i < uuids.size(); i++) {
            shortIdsByUuid.put(uuids.get(i), Utils.format(format, i));
        }

        locations = new NewLocation[uuids.size()];
        for (int i = 0; i < uuids.size(); i++) {
            String uuid = uuids.get(i);
            String name = namesByUuid.get(uuid);
            int count = numPatientsAtNode.get(uuid);

            // Use the short IDs to construct a sortable path string for each node.
            // Each path component ends with a terminating character so that
            // a.path.startsWith(b.path) if and only if a is in b's subtree.
            String path = "";
            for (String u = uuid; u != null; u = parentUuidsByUuid.get(u)) {
                path = shortIdsByUuid.get(u) + "/" + path;
                numPatientsInSubtree.put(u, numPatientsInSubtree.get(u) + count);
            }

            locations[i] = new NewLocation(uuid, path, name);
            locationsByUuid.put(uuid, locations[i]);
        }

        // Finally, sort by path, yielding an array of nodes in depth-first
        // order with every subtree in the proper order.
        Arrays.sort(locations);
    }

    public void updatePatientCounts(Cursor cursor) {
        synchronized (patientCountLock) {
            numPatientsAtNode.clear();
            numPatientsInSubtree.clear();
            totalNumPatients = 0;

            while (!cursor.isLast() && !cursor.isAfterLast()) {
                cursor.moveToNext();

                String uuid = Utils.getString(cursor, Contracts.LocalizedLocations.UUID);
                int numPatients = Utils.getInt(cursor, Contracts.LocalizedLocations.PATIENT_COUNT, 0);

                numPatientsAtNode.put(uuid, numPatients);
                numPatientsInSubtree.put(uuid, 0);  // counts will be added below
                totalNumPatients += numPatients;
            }

            for (NewLocation location : locations) {
                int count = numPatientsAtNode.get(location.uuid);
                for (String u = location.uuid; u != null; u = parentUuidsByUuid.get(u)) {
                    numPatientsInSubtree.put(u, numPatientsInSubtree.get(u) + count);
                }
            }
        }
    }

    /** Returns true if the specified UUID exists in this tree. */
    private boolean contains(String uuid) {
        return locationsByUuid.containsKey(uuid);
    }

    /** Returns true if the specified location exists in this tree. */
    public boolean contains(NewLocation location) {
        return contains(location.uuid);
    }

    /** Gets the location with a given UUID. */
    public NewLocation get(String uuid) {
        return locationsByUuid.get(uuid);
    }

    /** Given the UUID of a location, gets the UUID of its parent location. */
    private @Nullable String getParent(@Nonnull String uuid) {
        return parentUuidsByUuid.get(uuid);
    }

    /** Gets the parent location of the given location. */
    public @Nullable NewLocation getParent(@Nonnull NewLocation location) {
        return get(getParent(location.uuid));
    }

    /** Returns true if the specified location is a leaf node. */
    private boolean isLeaf(@Nonnull String uuid) {
        return !nonleafLocations.contains(uuid);
    }

    /** Returns true if the given location is a leaf node. */
    public boolean isLeaf(@Nonnull NewLocation location) {
        return isLeaf(location.uuid);
    }

    /** Given a UUID, counts the patients just at its node. */
    private int countPatientsAt(@Nonnull String uuid) {
        synchronized (patientCountLock) {
            return Utils.toNonnull(numPatientsAtNode.get(uuid), 0);
        }
    }

    /** Given a node, counts the patients just at this node. */
    public int countPatientsAt(@Nonnull NewLocation node) {
        return countPatientsAt(node.uuid);
    }

    /** Given a UUID, counts all the patients in its subtree. */
    private int countPatientsIn(@Nonnull String uuid) {
        synchronized (patientCountLock) {
            return Utils.toNonnull(numPatientsInSubtree.get(uuid), 0);
        }
    }

    /** Given a node, counts all the patients in its subtree. */
    public int countPatientsIn(@Nonnull NewLocation root) {
        return countPatientsIn(root.uuid);
    }

    /** Gets the count of all patients in the forest. */
    public int countAllPatients() {
        synchronized (patientCountLock) {
            return totalNumPatients;
        }
    }

    /** Returns an ordered list of the roots of all the trees. */
    public @Nonnull List<NewLocation> getRoots() {
        List<NewLocation> roots = new ArrayList<>();
        for (NewLocation location : locations) {
            if (location.depth == 1) {
                roots.add(location);
            }
        }
        return roots;
    }

    /** Given a node, returns an ordered list of its children. */
    public @Nonnull List<NewLocation> getChildren(@Nonnull NewLocation parent) {
        List<NewLocation> children = new ArrayList<>();
        for (NewLocation location : locations) {
            if (location.isInSubtree(parent) && location.depth == parent.depth + 1) {
                children.add(location);
            }
        }
        return children;
    }

    /** Given a node, returns an iterator over all its descendants. */
    public @Nonnull Iterable<NewLocation> getDescendants(@Nonnull NewLocation root) {
        List<NewLocation> descendants = new ArrayList<>();
        for (NewLocation location : locations) {
            if (location.isInSubtree(root) && !eq(location, root)) {
                descendants.add(location);
            }
        }
        return ImmutableList.copyOf(descendants);
    }
}
