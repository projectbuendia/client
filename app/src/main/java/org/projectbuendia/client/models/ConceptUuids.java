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

    // Dates shown as day numbers at top left.
    public static final String FIRST_SYMPTOM_DATE_UUID = toUuid(1730);
    public static final String ADMISSION_DATE_UUID = toUuid(162622);

    // Ebola lab test results shown in a fixed tile.
    public static final String PCR_NP_UUID = toUuid(162826);
    public static final String PCR_L_UUID = toUuid(162827);
    public static final String PCR_GP_UUID = toUuid(777000025);

    // Essential status flags shown at top right: pregnancy, IV access, on O2, cannot eat.
    public static final String IV_UUID = toUuid(777000011);
    public static final String DYSPHAGIA_UUID = toUuid(888118789);
    public static final String OXYGEN_UUID = toUuid(888162738);
    public static final String PREGNANCY_UUID = toUuid(5272);

    // Condition question and values.  Used to colour-code patient ID chips in patient lists.
    public static final String GENERAL_CONDITION_UUID = "a3657203-cfed-44b8-8e3f-960f8d4cf3b3";
    public static final String GENERAL_CONDITION_WELL_UUID = toUuid(1855);
    public static final String GENERAL_CONDITION_UNWELL_UUID = toUuid(137793);
    public static final String GENERAL_CONDITION_CRITICAL_UUID = "2827e7ac-10c1-4d3f-9fa4-0239771d8548";
    public static final String GENERAL_CONDITION_PALLIATIVE_UUID = "7cea1f8f-88cb-4f9c-a9d6-dc28d6eaa520";
    public static final String GENERAL_CONDITION_CONVALESCENT_UUID = toUuid(119844);
    public static final String GENERAL_CONDITION_DISCHARGED_NON_CASE_UUID = "e4a20c4a-6f13-11e4-b315-040ccecfdba4";
    public static final String GENERAL_CONDITION_DISCHARGED_CURED_UUID = toUuid(159791);
    public static final String GENERAL_CONDITION_SUSPECTED_DEAD_UUID = "91dc5fcc-fa9e-4ccd-8cd0-0d203923493f";
    public static final String GENERAL_CONDITION_CONFIRMED_DEAD_UUID = toUuid(160432);

    public static final String[] GENERAL_CONDITION_UUIDS = new String[] {
        GENERAL_CONDITION_WELL_UUID,
        GENERAL_CONDITION_UNWELL_UUID,
        GENERAL_CONDITION_CRITICAL_UUID,
        GENERAL_CONDITION_PALLIATIVE_UUID,
        GENERAL_CONDITION_CONVALESCENT_UUID,
        GENERAL_CONDITION_DISCHARGED_NON_CASE_UUID,
        GENERAL_CONDITION_DISCHARGED_CURED_UUID,
        GENERAL_CONDITION_SUSPECTED_DEAD_UUID,
        GENERAL_CONDITION_CONFIRMED_DEAD_UUID
    };


    // ==== Concept UUIDs for observation-like events stored as observations.

    // This is a custom Buendia-specific concept to indicate that a treatment order
    // has been carried out (e.g. a prescribed medication has been administered).
    // The timestamp of an observation for this concept should be the time the order
    // was executed, and the value of the observation should be the UUID of the order.
    public static final String ORDER_EXECUTED_UUID = "buendia_concept_order_executed";
    public static final String LOCATION_UUID = "buendia_concept_location";


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
        .put(GENERAL_CONDITION_PALLIATIVE_UUID, ResStatus.PALLIATIVE)
        .put(GENERAL_CONDITION_CONVALESCENT_UUID, ResStatus.CONVALESCENT)
        .put(GENERAL_CONDITION_DISCHARGED_NON_CASE_UUID, ResStatus.DISCHARGED_NON_CASE)
        .put(GENERAL_CONDITION_DISCHARGED_CURED_UUID, ResStatus.DISCHARGED_CURED)
        .put(GENERAL_CONDITION_SUSPECTED_DEAD_UUID, ResStatus.SUSPECTED_DEAD)
        .put(GENERAL_CONDITION_CONFIRMED_DEAD_UUID, ResStatus.CONFIRMED_DEAD)
        .build();

    /** Returns the {@link ResStatus} for the specified condition UUID. */
    public static ResStatus getResStatus(String conditionUuid) {
        return Utils.getOrDefault(STATUS_BY_CONDITION_UUID, conditionUuid, ResStatus.UNKNOWN);
    }

    private ConceptUuids() {
    }
}
