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

import org.projectbuendia.client.models.LocationForest;
import org.projectbuendia.client.models.LocationQueryResult;

/** Constructs a fake {@link LocationForest} for use in tests. */
public class FakeForestFactory {
    public static final String ROOT_UUID = "foo";
    public static final String SUSPECT_1_UUID = "tent_s1";
    public static final String SUSPECT_2_UUID = "tent_s2";

    public static final String SITE_NAME = "Fake Site";
    public static final String TRIAGE_ZONE_NAME = "Triage";
    public static final String DISCHARGED_ZONE_NAME = "Discharged";
    public static final String SUSPECT_ZONE_NAME = "Suspect";
    public static final String SUSPECT_1_TENT_NAME = "S1";
    public static final String SUSPECT_2_TENT_NAME = "S2";
    public static final String SUSPECT_ZONE_UUID = "2f1e2418-ede6-481a-ad80-b9939a7fde8e";
    public static final String TRIAGE_ZONE_UUID = "3f75ca61-ec1a-4739-af09-25a84e3dd237";
    public static final String DISCHARGED_ZONE_UUID = "d7ca63c3-6ea0-4357-82fd-0910cc17a2cb";

    /**
     * Builds an {@link LocationForest} with a facility, the Triage and Discharged zones, and
     * a Suspect zone containing two tents.
     * @return the constructed {@link LocationForest}
     */
    public static LocationForest build() {
        return new LocationForest(new FakeTypedCursor<>(
            getSiteLocation(),
            getTriageZoneLocation(),
            getDischargedZoneLocation(),
            getSuspectZoneLocation(),
            getSuspect1TentLocation(),
            getSuspect2TentLocation()
        ));
    }

    private static LocationQueryResult getSiteLocation() {
        return new LocationQueryResult(ROOT_UUID, null, SITE_NAME, 0);
    }

    private static LocationQueryResult getTriageZoneLocation() {
        return new LocationQueryResult(TRIAGE_ZONE_UUID, ROOT_UUID, TRIAGE_ZONE_NAME, 0);
    }

    private static LocationQueryResult getDischargedZoneLocation() {
        return new LocationQueryResult(DISCHARGED_ZONE_UUID, ROOT_UUID, DISCHARGED_ZONE_NAME, 0);
    }

    private static LocationQueryResult getSuspectZoneLocation() {
        return new LocationQueryResult(SUSPECT_ZONE_UUID, ROOT_UUID, SUSPECT_ZONE_NAME, 0);
    }

    private static LocationQueryResult getSuspect1TentLocation() {
        return new LocationQueryResult(SUSPECT_1_UUID, SUSPECT_ZONE_UUID, SUSPECT_1_TENT_NAME, 0);
    }

    private static LocationQueryResult getSuspect2TentLocation() {
        return new LocationQueryResult(SUSPECT_2_UUID, SUSPECT_ZONE_UUID, SUSPECT_2_TENT_NAME, 0);
    }

    public static LocationForest emptyForest() {
        return new LocationForest(new FakeTypedCursor<>());
    }
}
