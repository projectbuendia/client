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

/** Defines a hardcoded set of possible zones returned by the server and their semantics and UI. */
// TODO/robustness: Get rid of these constants and use the actual locations on the server!
public class Zones {

    public static final String CONFIRMED_ZONE_UUID = "b9038895-9c9d-4908-9e0d-51fd535ddd3c";
    public static final String MORGUE_ZONE_UUID = "4ef642b9-9843-4d0d-9b2b-84fe1984801f";
    public static final String OUTSIDE_ZONE_UUID = "00eee068-4d2a-4b41-bfe1-41e3066ab213";
    public static final String PROBABLE_ZONE_UUID = "3b11e7c8-a68a-4a5f-afb3-a4a053592d0e";
    public static final String SUSPECT_ZONE_UUID = "2f1e2418-ede6-481a-ad80-b9939a7fde8e";
    public static final String TRIAGE_ZONE_UUID = "3f75ca61-ec1a-4739-af09-25a84e3dd237";
    public static final String DISCHARGED_ZONE_UUID = "d7ca63c3-6ea0-4357-82fd-0910cc17a2cb";
    // Where to place patients with no location.
    public static final String DEFAULT_LOCATION_UUID = TRIAGE_ZONE_UUID;

    private Zones() {
        // Zone contains only static methods.
    }
}
