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

import org.projectbuendia.client.utils.Logger;
import org.projectbuendia.client.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import static org.projectbuendia.client.utils.Utils.eq;

/** An ordered hierarchy of locations with their localized names. */
public class LocationForest {
    private static final Logger LOG = Logger.create();

    private final Location[] locations;
    private final Map<String, Location> locationsByUuid = new HashMap<>();
    private final Map<String, String> parentUuidsByUuid = new HashMap<>();
    private final Map<String, String> pathsByUuid = new HashMap<>();
    private final Set<String> nonleafUuids = new HashSet<>();
    private final Location defaultLocation;
    private final Map<String, Integer> numPatientsAtNode = new HashMap<>();
    private final Map<String, Integer> numPatientsInSubtree = new HashMap<>();
    private int totalNumPatients;

    private final Object patientCountLock = new Object();
    private Runnable onPatientCountsUpdatedListener = null;

    public LocationForest() {
        locations = new Location[0];
        defaultLocation = null;
    }

    public LocationForest(Iterable<LocationQueryResult> cursor) {
        List<String> uuids = new ArrayList<>();
        Map<String, String> namesByUuid = new HashMap<>();
        Map<String, String> shortIdsByUuid = new HashMap<>();
        int totalNumPatients = 0;

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

        Pattern EXTRAS_PATTERN = Pattern.compile("\\[(.*?)\\]");
        locations = new Location[uuids.size()];
        String defaultUuid = null;
        for (int i = 0; i < uuids.size(); i++) {
            String uuid = uuids.get(i);

            // Parts of the name that are enclosed in square brackets are
            // not shown; this makes it possible to attach extra information
            // to locations in a way that can be done entirely from the
            // OpenMRS web interface.  In particular, putting a number in
            // a bracketed prefix will control the sorting order of locations,
            // because locations are sorted alphanumerically by name (e.g.
            // "2" will come before "11").
            String name = namesByUuid.get(uuid);
            Matcher matcher = EXTRAS_PATTERN.matcher(name);
            if (matcher.find()) {
                String extras = matcher.group(1);
                name = EXTRAS_PATTERN.matcher(name).replaceAll("").trim();
                if (extras.contains("*")) {
                    defaultUuid = uuid;
                }
            }

            // Use the short IDs to construct a sortable path string for each node.
            // Each path component ends with a terminating character so that
            // a.path.startsWith(b.path) if and only if a is in b's subtree.
            String path = "";
            int count = numPatientsAtNode.get(uuid);
            for (String u = uuid; u != null; u = parentUuidsByUuid.get(u)) {
                path = shortIdsByUuid.get(u) + "/" + path;
                numPatientsInSubtree.put(u, numPatientsInSubtree.get(u) + count);
            }

            locations[i] = new Location(uuid, name);
            pathsByUuid.put(uuid, path);
            locationsByUuid.put(uuid, locations[i]);
        }

        // Finally, sort by path, yielding an array of nodes in depth-first
        // order with every subtree in the proper order.
        sort(locations);

        // The default location is either set with an asterisk in the name
        // (see above) or defaults to the first leaf node.
        if (defaultUuid == null) {
            for (Location location : locations) {
                if (isLeaf(location)) {
                    defaultUuid = location.uuid;
                    break;
                }
            }
        }
        defaultLocation = locationsByUuid.get(defaultUuid); // nullable

        LOG.i("Loaded LocationForest with %d locations; default = %s",
            locations.length, defaultLocation);
    }

    public void sort(Location[] locations) {
        Arrays.sort(locations, (a, b) -> {
            String pathA = Utils.toNonnull(pathsByUuid.get(a.uuid));
            String pathB = Utils.toNonnull(pathsByUuid.get(b.uuid));
            return pathA.compareTo(pathB);
        });
    }

    public void updatePatientCounts(Map<String, Integer> patientCountsByLocationUuid) {
        synchronized (patientCountLock) {
            totalNumPatients = 0;
            numPatientsAtNode.clear();
            numPatientsInSubtree.clear();

            for (Location location : locations) {
                int count = Utils.getOrDefault(patientCountsByLocationUuid, location.uuid, 0);
                numPatientsAtNode.put(location.uuid, count);
                for (String u = location.uuid; u != null; u = parentUuidsByUuid.get(u)) {
                    numPatientsInSubtree.put(u,
                        Utils.getOrDefault(numPatientsInSubtree, u, 0) + count);
                }
            }
            LOG.i("Updated existing LocationForest; total patients: %d", totalNumPatients);
        }
        if (onPatientCountsUpdatedListener != null) {
            onPatientCountsUpdatedListener.run();
        }
    }

    public void setOnPatientCountsUpdatedListener(Runnable listener) {
        onPatientCountsUpdatedListener = listener;
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

    /** Returns the depth of a node (zero for root nodes, -1 for nonexistent nodes). */
    public int getDepth(@Nonnull Location location) {
        String path = Utils.getOrDefault(pathsByUuid, location.uuid, "");
        // Note: split() omits trailing empty parts; "foo/".split("/").length is 1.
        return path.split("/").length - 1;
    }

    /** Returns true if the given location is a leaf node. */
    public boolean isLeaf(@Nonnull Location location) {
        return !nonleafUuids.contains(location.uuid);
    }

    /** Given a UUID, counts the patients just at its node. */
    public int countPatientsAt(@Nonnull Location node) {
        synchronized (patientCountLock) {
            return Utils.getOrDefault(numPatientsAtNode, node.uuid, 0);
        }
    }

    /** Given a UUID, counts all the patients in its subtree. */
    public int countPatientsIn(@Nonnull Location root) {
        synchronized (patientCountLock) {
            return Utils.getOrDefault(numPatientsInSubtree, root.uuid, 0);
        }
    }

    /** Gets the count of all patients in the forest. */
    public int countAllPatients() {
        synchronized (patientCountLock) {
            return totalNumPatients;
        }
    }

    /** Iterate over all the nodes in depth-first order. */
    public @Nonnull Collection<Location> allNodes() {
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
        String rootPath = pathsByUuid.get(root.uuid);
        if (rootPath != null) {
            for (Location location : locations) {
                if (pathsByUuid.get(location.uuid).startsWith(rootPath)) {
                    descendants.add(location);
                }
            }
        }
        return descendants;
    }

    /** Returns the default location where new patients will be placed. */
    public @Nullable Location getDefaultLocation() {  // null only when size() == 0
        return defaultLocation;
    }
}
