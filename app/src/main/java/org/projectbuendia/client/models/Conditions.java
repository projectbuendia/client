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

import com.google.common.collect.ImmutableMap;

import org.projectbuendia.client.resolvables.Condition;
import org.projectbuendia.client.utils.Utils;

import java.util.Map;

public class Conditions {
    public static final String WELL_UUID = "1855AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static final String UNWELL_UUID = "137793AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static final String CRITICAL_UUID = "2827e7ac-10c1-4d3f-9fa4-0239771d8548";
    public static final String PALLIATIVE_UUID = "7cea1f8f-88cb-4f9c-a9d6-dc28d6eaa520";
    public static final String CONVALESCENT_UUID = "119844AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static final String DISCHARGED_NON_CASE_UUID = "e4a20c4a-6f13-11e4-b315-040ccecfdba4";
    public static final String DISCHARGED_CURED_UUID = "159791AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static final String SUSPECTED_DEAD_UUID = "91dc5fcc-fa9e-4ccd-8cd0-0d203923493f";
    public static final String CONFIRMED_DEAD_UUID = "160432AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static final String[] UUIDS = new String[] {
        WELL_UUID,
        UNWELL_UUID,
        CRITICAL_UUID,
        PALLIATIVE_UUID,
        CONVALESCENT_UUID,
        SUSPECTED_DEAD_UUID,
        CONFIRMED_DEAD_UUID,
        DISCHARGED_CURED_UUID,
        DISCHARGED_NON_CASE_UUID
    };
    public static final Map<String, Condition> CONDITIONS_BY_UUID =
        ImmutableMap.<String, Condition>builder()
        .put(WELL_UUID, Condition.WELL)
        .put(UNWELL_UUID, Condition.UNWELL)
        .put(CRITICAL_UUID, Condition.CRITICAL)
        .put(PALLIATIVE_UUID, Condition.PALLIATIVE)
        .put(CONVALESCENT_UUID, Condition.CONVALESCENT)
        .put(SUSPECTED_DEAD_UUID, Condition.SUSPECTED_DEAD)
        .put(CONFIRMED_DEAD_UUID, Condition.CONFIRMED_DEAD)
        .put(DISCHARGED_CURED_UUID, Condition.DISCHARGED_CURED)
        .put(DISCHARGED_NON_CASE_UUID, Condition.DISCHARGED_NON_CASE)
        .build();

    public static Condition getCondition(String uuid) {
        return Utils.valueOrDefault(CONDITIONS_BY_UUID.get(uuid), Condition.UNKNOWN);
    }

    private Conditions() { } // Condition contains only static methods.
}
