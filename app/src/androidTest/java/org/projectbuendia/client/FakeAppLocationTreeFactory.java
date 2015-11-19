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

package org.projectbuendia.client;

import org.projectbuendia.client.models.Location;
import org.projectbuendia.client.models.LocationTree;
import org.projectbuendia.client.models.Zones;

/** Constructs a fake {@link LocationTree} for use in tests. */
public class FakeAppLocationTreeFactory {
    public static final String ROOT_UUID = "foo";
    public static final String SUSPECT_1_UUID = "tent_s1";
    public static final String SUSPECT_2_UUID = "tent_s2";

    public static final String SITE_NAME = "Fake Site";
    public static final String TRIAGE_ZONE_NAME = "Triage";
    public static final String DISCHARGED_ZONE_NAME = "Discharged";
    public static final String SUSPECT_ZONE_NAME = "Suspect";
    public static final String SUSPECT_1_TENT_NAME = "S1";
    public static final String SUSPECT_2_TENT_NAME = "S2";

    /**
     * Builds an {@link LocationTree} with a facility, the Triage and Discharged zones, and
     * a Suspect zone containing two tents.
     * @return the constructed {@link LocationTree}
     */
    public static LocationTree build() {
        FakeTypedCursor<Location> locationCursor =
            new FakeTypedCursor<>(
                getSiteLocation(),
                getTriageZoneLocation(),
                getDischargedZoneLocation(),
                getSuspectZoneLocation(),
                getSuspect1TentLocation(),
                getSuspect2TentLocation()
            );
        return LocationTree.forTypedCursor(locationCursor);
    }

    private static Location getSiteLocation() {
        return new Location(ROOT_UUID, null, SITE_NAME, 0);
    }

    private static Location getTriageZoneLocation() {
        return new Location(Zones.TRIAGE_ZONE_UUID, ROOT_UUID, TRIAGE_ZONE_NAME, 0);
    }

    private static Location getDischargedZoneLocation() {
        return new Location(Zones.DISCHARGED_ZONE_UUID, ROOT_UUID, DISCHARGED_ZONE_NAME, 0);
    }

    private static Location getSuspectZoneLocation() {
        return new Location(Zones.SUSPECT_ZONE_UUID, ROOT_UUID, SUSPECT_ZONE_NAME, 0);
    }

    private static Location getSuspect1TentLocation() {
        return new Location(SUSPECT_1_UUID, Zones.SUSPECT_ZONE_UUID, SUSPECT_1_TENT_NAME, 0);
    }

    private static Location getSuspect2TentLocation() {
        return new Location(SUSPECT_2_UUID, Zones.SUSPECT_ZONE_UUID, SUSPECT_2_TENT_NAME, 0);
    }

    public static LocationTree emptyTree() {
        FakeTypedCursor<Location> locationCursor = new FakeTypedCursor<>();
        return LocationTree.forTypedCursor(locationCursor);
    }
}
