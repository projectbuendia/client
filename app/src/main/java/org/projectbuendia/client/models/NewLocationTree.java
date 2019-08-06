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
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import static org.projectbuendia.client.utils.Utils.eq;

/** An ordered hierarchy of locations with their localized names. */
public class NewLocationTree {
    private static ContentResolver resolver = null;

    private static NewLocationTree loadedTree = null;
    private static String requestedLocale = null;

    private final NewLocation[] locations;
    private final Map<String, NewLocation> locationsByUuid;

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
        Map<String, String> parentUuidsByUuid = new HashMap<>();
        Map<String, String> namesByUuid = new HashMap<>();
        Map<String, Integer> countsByUuid = new HashMap<>();
        Map<String, String> shortIdsByUuid = new HashMap<>();

        while (!cursor.isLast() && !cursor.isAfterLast()) {
            cursor.moveToNext();

            String uuid = Utils.getString(cursor, Contracts.LocalizedLocations.UUID);
            String parentUuid = Utils.getString(cursor, Contracts.LocalizedLocations.PARENT_UUID);
            String name = Utils.getString(cursor, Contracts.LocalizedLocations.NAME);
            int numPatients = Utils.getInt(cursor, Contracts.LocalizedLocations.PATIENT_COUNT, 0);

            uuids.add(uuid);
            parentUuidsByUuid.put(uuid, parentUuid);
            namesByUuid.put(uuid, name);
            countsByUuid.put(uuid, numPatients);
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

        // Use the short IDs to construct a sortable path string for each node.
        locations = new NewLocation[uuids.size()];
        locationsByUuid = new HashMap<>();

        for (int i = 0; i < uuids.size(); i++) {
            String uuid = uuids.get(i);
            String parentUuid = parentUuidsByUuid.get(uuid);
            String name = namesByUuid.get(uuid);
            int count = countsByUuid.get(uuid);

            // Path components end with a terminating character so that
            // a.path.startsWith(b.path) if and only if a is in b's subtree.
            String path = shortIdsByUuid.get(uuid) + "/";
            while (parentUuid != null && shortIdsByUuid.containsKey(parentUuid)) {
                path = shortIdsByUuid.get(parentUuid) + "/" + path;
                parentUuid = parentUuidsByUuid.get(parentUuid);
            }

            locations[i] = new NewLocation(uuid, path, name, count);
            locationsByUuid.put(uuid, locations[i]);
        }

        // Finally, sort by path, yielding an array of nodes in depth-first
        // order with every subtree in the proper order.
        Arrays.sort(locations);
    }

    /** Gets the location with a given UUID. */
    public NewLocation get(String uuid) {
        return locationsByUuid.get(uuid);
    }

    /** Given a node, returns an ordered list of its children, or, if null, of all root nodes. */
    public @Nonnull Iterable<NewLocation> getChildren(@Nullable NewLocation parent) {
        int depth = parent != null ? parent.depth : 0;
        List<NewLocation> children = new ArrayList<>();
        for (NewLocation location : locations) {
            if (location.isInSubtree(parent) && location.depth == depth + 1) {
                children.add(location);
            }
        }
        return ImmutableList.copyOf(children);
    }

    /** Given a node, returns an iterator over all its descendants, or, if null, all nodes. */
    public @Nonnull Iterable<NewLocation> getDescendants(@Nullable NewLocation root) {
        List<NewLocation> descendants = new ArrayList<>();
        for (NewLocation location : locations) {
            if (location.isInSubtree(root) && !eq(location, root)) {
                descendants.add(location);
            }
        }
        return ImmutableList.copyOf(descendants);
    }

    /** Given a node, counts all the patients in its subtree, or, if null, the entire tree. */
    public int getNumPatientsInSubtree(@Nullable NewLocation root) {
        int numPatients = 0;
        for (NewLocation location : locations) {
            if (location.isInSubtree(root)) {
                numPatients += location.numPatients;
            }
        }
        return numPatients;
    }
}
