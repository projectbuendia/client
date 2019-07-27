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

    // Dates shown as day numbers at top left.
    public static final String FIRST_SYMPTOM_DATE_UUID = toUuid(1730);
    public static final String ADMISSION_DATE_UUID = toUuid(162622);

    // Ebola lab test results shown in a fixed tile.
    public static final String PCR_NP_UUID = toUuid(162826);
    public static final String PCR_L_UUID = toUuid(162827);
    public static final String PCR_GP_UUID = toUuid(777000025);

    // Essential status flags shown at top right: pregnant, IV access, on O2, cannot eat.
    public static final String IV_UUID = "f50c9c63-3ff9-4c26-9d18-12bfc58a3d07";
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


    // ==== Pulse; used only for logging messages to the server.

    // TODO(ping): We should do remote logging a different way.
    public static final String PULSE_UUID = toUuid(5087);


    // ==== UUIDs referenced only for sorting by severity or interpreting as false/null.

    public static final String NO_UUID = toUuid(1066);  // answer: no
    public static final String NONE_UUID = toUuid(1107);  // answer: none
    public static final String NORMAL_UUID = toUuid(1115);  // answer: normal
    public static final String SOLID_FOOD_UUID = toUuid(159597);  // answer: patient can eat solid food
    public static final String MILD_UUID = toUuid(1148);  // answer: mild (severity)
    public static final String MODERATE_UUID = toUuid(1499);  // answer: moderate (severity)
    public static final String SEVERE_UUID = toUuid(1500);  // answer: severe
    public static final String YES_UUID = toUuid(1065);  // answer: yes
    public static final String UNKNOWN_UUID = toUuid(1067);  // answer: answer is unknown


    public static final String toUuid(int id) {
        return Utils.expandUuid(id);
    }

    public static final String toUuid(boolean bool) {
        return bool ? YES_UUID : NO_UUID;
    }

    public static final boolean isYes(String uuid) {
        return YES_UUID.equals(uuid);
    }

    public static final boolean isYes(Obs obs) {
        return obs != null && YES_UUID.equals(obs.value);
    }

    /** UUIDs for concepts that mean everything is normal; there is no worrying symptom. */
    public static boolean isNormal(String uuid) {
        return NORMAL_UUID.equals(uuid)
            || NONE_UUID.equals(uuid)
            || NO_UUID.equals(uuid)
            || SOLID_FOOD_UUID.equals(uuid);
    }

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
