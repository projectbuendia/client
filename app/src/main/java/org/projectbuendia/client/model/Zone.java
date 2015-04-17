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

package org.projectbuendia.client.model;

import org.projectbuendia.client.data.app.AppLocation;
import org.projectbuendia.client.data.res.ResZone;

import java.util.Arrays;
import java.util.List;

/**
 * Defines a hardcoded set of possible zones returned by the server and semantics/UI about those
 * zones. Over time, hardcoded UUID's should be phased out and replaced with modular configuration,
 * either on the server or the client.
 */
public class Zone {

    private Zone() {
        // Zone contains only static methods.
    }

    public static final String CONFIRMED_ZONE_UUID = "b9038895-9c9d-4908-9e0d-51fd535ddd3c";
    public static final String MORGUE_ZONE_UUID = "4ef642b9-9843-4d0d-9b2b-84fe1984801f";
    public static final String OUTSIDE_ZONE_UUID = "00eee068-4d2a-4b41-bfe1-41e3066ab213";
    public static final String PROBABLE_ZONE_UUID = "3b11e7c8-a68a-4a5f-afb3-a4a053592d0e";
    public static final String SUSPECT_ZONE_UUID = "2f1e2418-ede6-481a-ad80-b9939a7fde8e";
    public static final String TRIAGE_ZONE_UUID = "3f75ca61-ec1a-4739-af09-25a84e3dd237";
    public static final String DISCHARGED_ZONE_UUID = "d7ca63c3-6ea0-4357-82fd-0910cc17a2cb";

    // Where to place patients with no location.
    public static final String DEFAULT_LOCATION = TRIAGE_ZONE_UUID;

    private static final List<String> ORDERED_ZONES = Arrays.asList(
            TRIAGE_ZONE_UUID,
            SUSPECT_ZONE_UUID,
            PROBABLE_ZONE_UUID,
            CONFIRMED_ZONE_UUID,
            MORGUE_ZONE_UUID,
            OUTSIDE_ZONE_UUID,
            DISCHARGED_ZONE_UUID
    );

    /** Compares two zones so that they sort in the order given in ORDERED_ZONES. */
    public static int compare(AppLocation a, AppLocation b) {
        return Integer.compare(ORDERED_ZONES.indexOf(a.uuid), ORDERED_ZONES.indexOf(b.uuid));
    }

    /** Returns the {@link ResZone} for the specified zone UUID. */
    public static ResZone getResZone(String uuid) {
        switch (uuid) {
            case SUSPECT_ZONE_UUID:
                return ResZone.SUSPECT;
            case PROBABLE_ZONE_UUID:
                return ResZone.PROBABLE;
            case CONFIRMED_ZONE_UUID:
                return ResZone.CONFIRMED;
            case MORGUE_ZONE_UUID:
            case OUTSIDE_ZONE_UUID:
            case TRIAGE_ZONE_UUID:
            default:
                return ResZone.UNKNOWN;
        }
    }
}
