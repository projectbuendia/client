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

import org.projectbuendia.client.utils.Utils;

import java.util.Comparator;
import java.util.List;

/** Compares {@link Location}s based on specificity, logical zone order, and alphanumeric order. */
public class LocationComparator implements Comparator<Location> {
    private LocationTree mTree;

    public LocationComparator(LocationTree tree) {
        mTree = tree;
    }

    @Override public int compare(Location lhs, Location rhs) {
        List<Location> pathA = mTree.getAncestorsStartingFromRoot(lhs);
        List<Location> pathB = mTree.getAncestorsStartingFromRoot(rhs);
        for (int i = 0; i < Math.min(pathA.size(), pathB.size()); i++) {
            Location locationA = pathA.get(i);
            Location locationB = pathB.get(i);
            int result = (i == LocationTree.ABSOLUTE_DEPTH_ZONE)
                ? Zones.compare(locationA, locationB)
                : Utils.alphanumericComparator.compare(locationA.name, locationB.name);
            if (result != 0) {
                return result;
            }
        }
        return pathA.size() - pathB.size();
    }
}
