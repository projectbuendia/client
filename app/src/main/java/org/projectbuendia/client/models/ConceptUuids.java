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

import org.projectbuendia.client.resolvables.ResStatus;
import org.projectbuendia.client.utils.Utils;

import java.util.Map;

/**
 * Defines hardcoded concept ids expected to exist on the OpenMRS server. Over time, values in this
 * file should be phased out and replaced with modular configuration, either on the server or the
 * client.
 */
public class ConceptUuids {
    // ==== UUIDs used for special treatment in the UI.

    private static String toUuid(int id) {
        return Utils.expandUuid(id);
    }

    // Date used for calculating "Day 1", "Day 2", etc. table headings.
    public static final String ADMISSION_DATETIME_UUID = toUuid(8001640);

    // Concepts whose values are prefilled in forms.
    public static final String PREGNANCY_UUID = toUuid(2005272);
    public static final String IV_UUID = toUuid(2900012);

    // Condition question and values.  Used to colour-code patient ID chips in patient lists.
    public static final String GENERAL_CONDITION_UUID = toUuid(2900018);
    public static final String GENERAL_CONDITION_WELL_UUID = toUuid(4001855);
    public static final String GENERAL_CONDITION_UNWELL_UUID = toUuid(4900039);
    public static final String GENERAL_CONDITION_CRITICAL_UUID = toUuid(4900040);

    public static final String[] GENERAL_CONDITION_UUIDS = new String[] {
        GENERAL_CONDITION_WELL_UUID,
        GENERAL_CONDITION_UNWELL_UUID,
        GENERAL_CONDITION_CRITICAL_UUID
    };


    // ==== Concept UUIDs for observation-like events stored as observations.

    // This is a custom Buendia-specific concept to indicate that a treatment order
    // has been carried out (e.g. a prescribed medication has been administered).
    // The timestamp of an observation for this concept should be the time the order
    // was executed, and the value of the observation should be the UUID of the order.
    public static final String ORDER_EXECUTED_UUID = "buendia_concept_order_executed";

    // This is a custom Buendia-specific concept to indicate where a patient is
    // placed, as a string consisting of a Location UUID, a slash, and a bed number.
    public static final String PLACEMENT_UUID = "buendia_concept_placement";


    // ==== Pulse; used only for health checks and logging messages to the server.

    public static final String PULSE_UUID = toUuid(5087);


    // ==== UUIDs referenced only for sort ordering.

    public static final String NO_UUID = toUuid(1066);  // answer: no
    public static final String NONE_UUID = toUuid(1107);  // answer: none
    public static final String NORMAL_UUID = toUuid(1115);  // answer: normal
    public static final String SOLID_FOOD_UUID = toUuid(159597);  // answer: patient can eat solid food
    public static final String MILD_UUID = toUuid(1148);  // answer: mild (severity)
    public static final String MODERATE_UUID = toUuid(1499);  // answer: moderate (severity)
    public static final String SEVERE_UUID = toUuid(1500);  // answer: severe
    public static final String YES_UUID = toUuid(1065);  // answer: yes
    public static final String UNKNOWN_UUID = toUuid(1067);  // answer: answer is unknown

    /**
     * A number specifying the ordering of coded values.  These are arranged from least to
     * most severe, or earliest to latest in typical temporal sequence, so that the maximum value
     * in a list of values for a particular concept is the most severe value or latest value.
     */
    private static final Map<String, Integer> CODED_VALUE_ORDERING =
        new ImmutableMap.Builder<String, Integer>()
            .put(ConceptUuids.NO_UUID, -100)
            .put(ConceptUuids.NONE_UUID, -1)
            .put(ConceptUuids.NORMAL_UUID, -1)
            .put(ConceptUuids.SOLID_FOOD_UUID, -1)
            .put(ConceptUuids.MILD_UUID, 1)
            .put(ConceptUuids.MODERATE_UUID, 2)
            .put(ConceptUuids.SEVERE_UUID, 3)
            .put(ConceptUuids.YES_UUID, 100).build();

    public static final int compareUuids(String a, String b) {
        int result = Integer.compare(
            Utils.getOrDefault(CODED_VALUE_ORDERING, a, 0),
            Utils.getOrDefault(CODED_VALUE_ORDERING, b, 0));
        if (result != 0) return result;
        return a.compareTo(b);
    }


    // ==== Boolean interpretation of UUIDs.

    public static final String toUuid(boolean bool) {
        return bool ? YES_UUID : NO_UUID;
    }

    public static final boolean isYes(String uuid) {
        return YES_UUID.equals(uuid);
    }

    public static final boolean isYes(Obs obs) {
        return obs != null && YES_UUID.equals(obs.value);
    }

    public static boolean isNormal(String uuid) {
        return NORMAL_UUID.equals(uuid)
            || NONE_UUID.equals(uuid)
            || NO_UUID.equals(uuid)
            || SOLID_FOOD_UUID.equals(uuid);
    }


    // ==== Mapping of general condition values to ResStatus values.

    public static final Map<String, ResStatus> STATUS_BY_CONDITION_UUID = new ImmutableMap.Builder<String, ResStatus>()
        .put(GENERAL_CONDITION_WELL_UUID, ResStatus.WELL)
        .put(GENERAL_CONDITION_UNWELL_UUID, ResStatus.UNWELL)
        .put(GENERAL_CONDITION_CRITICAL_UUID, ResStatus.CRITICAL)
        .build();

    /** Returns the {@link ResStatus} for the specified condition UUID. */
    public static ResStatus getResStatus(String conditionUuid) {
        return Utils.getOrDefault(STATUS_BY_CONDITION_UUID, conditionUuid, ResStatus.UNKNOWN);
    }

    private ConceptUuids() {
    }
}
