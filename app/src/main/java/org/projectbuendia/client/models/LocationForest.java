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

import android.support.annotation.Nullable;

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
public class LocationForest {
    private final Location[] locations;
    private final Map<String, Location> locationsByUuid = new HashMap<>();
    private final Map<String, String> parentUuidsByUuid = new HashMap<>();
    private final Set<String> nonleafUuids = new HashSet<>();

    private final Object patientCountLock = new Object();
    private final Map<String, Integer> numPatientsAtNode = new HashMap<>();
    private final Map<String, Integer> numPatientsInSubtree = new HashMap<>();
    private int totalNumPatients;

    public LocationForest(TypedCursor<LocationQueryResult> cursor) {
        List<String> uuids = new ArrayList<>();
        Map<String, String> namesByUuid = new HashMap<>();
        Map<String, String> shortIdsByUuid = new HashMap<>();
        totalNumPatients = 0;

        for (LocationQueryResult result : cursor) {
            uuids.add(result.uuid);
            parentUuidsByUuid.put(result.uuid, result.parentUuid);
            nonleafUuids.add(result.parentUuid);
            namesByUuid.put(result.uuid, result.name);
            numPatientsAtNode.put(result.uuid, result.numPatients);
            numPatientsInSubtree.put(result.uuid, 0);  // counts will be added below
            totalNumPatients += result.numPatients;
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

        locations = new Location[uuids.size()];
        for (int i = 0; i < uuids.size(); i++) {
            String uuid = uuids.get(i);

            // If there is prefix in square brackets in front of the name,
            // we hide it.  This provides a way to control the sorting order
            // of locations that can be done entirely from the OpenMRS web
            // interface.  Locations are sorted alphanumerically, so numbers
            // will sort properly (e.g. "2" will come before "11").
            // The prefix is not part of the location name and is not shown.
            String name = namesByUuid.get(uuid).replaceAll("^\\s*\\[.*?\\]\\s*", "");
            int count = numPatientsAtNode.get(uuid);

            // Use the short IDs to construct a sortable path string for each node.
            // Each path component ends with a terminating character so that
            // a.path.startsWith(b.path) if and only if a is in b's subtree.
            String path = "";
            for (String u = uuid; u != null; u = parentUuidsByUuid.get(u)) {
                path = shortIdsByUuid.get(u) + "/" + path;
                numPatientsInSubtree.put(u, numPatientsInSubtree.get(u) + count);
            }

            locations[i] = new Location(uuid, path, name);
            locationsByUuid.put(uuid, locations[i]);
        }

        // Finally, sort by path, yielding an array of nodes in depth-first
        // order with every subtree in the proper order.
        Arrays.sort(locations);
    }

    public void updatePatientCounts(TypedCursor<LocationQueryResult> cursor) {
        synchronized (patientCountLock) {
            numPatientsAtNode.clear();
            numPatientsInSubtree.clear();
            totalNumPatients = 0;

            for (Location location : locations) {
                numPatientsInSubtree.put(location.uuid, 0);  // counts will be added below
            }

            for (LocationQueryResult result : cursor) {
                numPatientsAtNode.put(result.uuid, result.numPatients);
                totalNumPatients += result.numPatients;
                for (String u = result.uuid; u != null; u = parentUuidsByUuid.get(u)) {
                    numPatientsInSubtree.put(u, numPatientsInSubtree.get(u) + result.numPatients);
                }
            }
        }
    }

    /** Returns true if the specified location exists in this forest. */
    public boolean contains(Location location) {
        return locationsByUuid.containsKey(location.uuid);
    }

    /** Gets the location with a given UUID. */
    public Location get(String uuid) {
        return locationsByUuid.get(uuid);
    }

    /** Gets the number of locations in this forest. */
    public int size() {
        return locations.length;
    }

    /** Gets the parent location of the given location. */
    public @Nullable Location getParent(@Nonnull Location location) {
        return get(parentUuidsByUuid.get(location.uuid));
    }

    /** Returns true if the given location is a leaf node. */
    public boolean isLeaf(@Nonnull Location location) {
        return !nonleafUuids.contains(location.uuid);
    }

    /** Given a UUID, counts the patients just at its node. */
    public int countPatientsAt(@Nonnull Location node) {
        synchronized (patientCountLock) {
            return Utils.toNonnull(numPatientsAtNode.get(node.uuid), 0);
        }
    }

    /** Given a UUID, counts all the patients in its subtree. */
    public int countPatientsIn(@Nonnull Location root) {
        synchronized (patientCountLock) {
            return Utils.toNonnull(numPatientsInSubtree.get(root.uuid), 0);
        }
    }

    /** Gets the count of all patients in the forest. */
    public int countAllPatients() {
        synchronized (patientCountLock) {
            return totalNumPatients;
        }
    }

    /** Iterate over all the nodes in depth-first order. */
    public @Nonnull Iterable<Location> allNodes() {
        return Arrays.asList(locations);
    }

    /** Gets a list of all the leaf nodes in depth-first order. */
    public @Nonnull List<Location> getLeaves() {
        List<Location> leaves = new ArrayList<>();
        for (Location location : locations) {
            if (isLeaf(location)) leaves.add(location);
        }
        return leaves;
    }

    /** Given a node, returns an ordered list of its immediate children. */
    public @Nonnull List<Location> getChildren(@Nonnull Location parent) {
        List<Location> children = new ArrayList<>();
        for (Location location : locations) {
            if (eq(parentUuidsByUuid.get(location.uuid), parent.uuid)) {
                children.add(location);
            }
        }
        return children;
    }


    /** Given a node, returns a list containing it and its descendants in depth-first order. */
    public @Nonnull List<Location> getSubtree(@Nonnull Location root) {
        List<Location> descendants = new ArrayList<>();
        for (Location location : locations) {
            if (location.isInSubtree(root)) {
                descendants.add(location);
            }
        }
        return descendants;
    }
}
