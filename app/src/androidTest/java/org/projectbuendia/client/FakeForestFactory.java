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

import com.google.common.collect.ImmutableList;

import org.projectbuendia.models.LocationForest;

import java.util.Arrays;

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
    public static final String SUSPECT_ZONE_UUID = "test_location_suspect";
    public static final String TRIAGE_ZONE_UUID = "test_location_triage";
    public static final String DISCHARGED_ZONE_UUID = "test_location_discharged";

    /**
     * Builds an {@link LocationForest} with a facility, the Triage and Discharged zones, and
     * a Suspect zone containing two tents.
     * @return the constructed {@link LocationForest}
     */
    public static LocationForest build() {
        return new LocationForest(Arrays.asList(
            getSiteLocation(),
            getTriageZoneLocation(),
            getDischargedZoneLocation(),
            getSuspectZoneLocation(),
            getSuspect1TentLocation(),
            getSuspect2TentLocation()
        ));
    }

    private static LocationForest.Record getSiteLocation() {
        return new LocationForest.Record(ROOT_UUID, null, SITE_NAME, 0);
    }

    private static LocationForest.Record getTriageZoneLocation() {
        return new LocationForest.Record(TRIAGE_ZONE_UUID, ROOT_UUID, TRIAGE_ZONE_NAME, 0);
    }

    private static LocationForest.Record getDischargedZoneLocation() {
        return new LocationForest.Record(DISCHARGED_ZONE_UUID, ROOT_UUID, DISCHARGED_ZONE_NAME, 0);
    }

    private static LocationForest.Record getSuspectZoneLocation() {
        return new LocationForest.Record(SUSPECT_ZONE_UUID, ROOT_UUID, SUSPECT_ZONE_NAME, 0);
    }

    private static LocationForest.Record getSuspect1TentLocation() {
        return new LocationForest.Record(SUSPECT_1_UUID, SUSPECT_ZONE_UUID, SUSPECT_1_TENT_NAME, 0);
    }

    private static LocationForest.Record getSuspect2TentLocation() {
        return new LocationForest.Record(SUSPECT_2_UUID, SUSPECT_ZONE_UUID, SUSPECT_2_TENT_NAME, 0);
    }

    public static LocationForest emptyForest() {
        return new LocationForest(ImmutableList.of());
    }
}
