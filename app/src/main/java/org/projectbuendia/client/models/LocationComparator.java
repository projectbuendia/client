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

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/** Compares {@link Location}s based on specificity, logical zone order, and alphanumeric order. */
public class LocationComparator implements Comparator<Location> {
    private static final List<String> ORDERED_ZONES = Arrays.asList(
        Zones.TRIAGE_ZONE_UUID,
        Zones.SUSPECT_ZONE_UUID,
        Zones.PROBABLE_ZONE_UUID,
        Zones.CONFIRMED_ZONE_UUID,
        Zones.MORGUE_ZONE_UUID,
        Zones.OUTSIDE_ZONE_UUID,
        Zones.DISCHARGED_ZONE_UUID
    );
    private LocationTree mTree;

    public LocationComparator(LocationTree tree) {
        mTree = tree;
    }

    /** Compares two zones so that they sort in the order given in ORDERED_ZONES. */
    public static int compareZones(Location a, Location b) {
        return Integer.compare(ORDERED_ZONES.indexOf(a.uuid), ORDERED_ZONES.indexOf(b.uuid));
    }

    @Override public int compare(Location lhs, Location rhs) {
        List<Location> pathA = mTree.getAncestorsStartingFromRoot(lhs);
        List<Location> pathB = mTree.getAncestorsStartingFromRoot(rhs);
        for (int i = 0; i < Math.min(pathA.size(), pathB.size()); i++) {
            Location locationA = pathA.get(i);
            Location locationB = pathB.get(i);
            int result = (i == LocationTree.ABSOLUTE_DEPTH_ZONE)
                ? compareZones(locationA, locationB)
                : Utils.ALPHANUMERIC_COMPARATOR.compare(locationA.name, locationB.name);
            if (result != 0) {
                return result;
            }
        }
        return pathA.size() - pathB.size();
    }
}
