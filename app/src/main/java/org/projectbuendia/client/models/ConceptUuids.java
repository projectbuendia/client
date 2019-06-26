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

import org.projectbuendia.client.resolvables.ResStatus;

/**
 * Defines hardcoded concept ids expected to exist on the OpenMRS server. Over time, values in this
 * file should be phased out and replaced with modular configuration, either on the server or the
 * client.
 */
public class ConceptUuids {
    public static final String CONSCIOUS_STATE_UUID = "162643AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static final String FLUIDS_UUID = "e96f504e-229a-4933-84d1-358abbd687e3";
    public static final String GENERAL_CONDITION_UUID = "a3657203-cfed-44b8-8e3f-960f8d4cf3b3";
    public static final String HYDRATION_UUID = "162653AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static final String IV_UUID = "f50c9c63-3ff9-4c26-9d18-12bfc58a3d07";
    public static final String DYSPHAGIA_UUID = "888118789AAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static final String OXYGEN_UUID = "888162738AAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static final String PREGNANCY_UUID = "5272AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static final String PULSE_UUID = "5087AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static final String RESPIRATION_UUID = "5242AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static final String PCR_NP_UUID = "162826AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static final String PCR_L_UUID = "162827AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static final String PCR_GP_UUID = "777000025AAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static final String FIRST_SYMPTOM_DATE_UUID = "1730AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static final String ADMISSION_DATE_UUID = "162622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    public static final String GENERAL_CONDITION_WELL_UUID = "1855AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static final String GENERAL_CONDITION_UNWELL_UUID =
        "137793AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static final String GENERAL_CONDITION_CRITICAL_UUID =
        "2827e7ac-10c1-4d3f-9fa4-0239771d8548";
    public static final String GENERAL_CONDITION_PALLIATIVE_UUID =
        "7cea1f8f-88cb-4f9c-a9d6-dc28d6eaa520";
    public static final String GENERAL_CONDITION_CONVALESCENT_UUID =
        "119844AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static final String GENERAL_CONDITION_DISCHARGED_NON_CASE =
        "e4a20c4a-6f13-11e4-b315-040ccecfdba4";
    public static final String GENERAL_CONDITION_CURED =
        "159791AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static final String GENERAL_CONDITION_SUSPECTED_DEAD =
        "91dc5fcc-fa9e-4ccd-8cd0-0d203923493f";
    public static final String GENERAL_CONDITION_CONFIRMED_DEAD =
        "160432AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    public static final String[] GENERAL_CONDITION_UUIDS = new String[] {
        GENERAL_CONDITION_WELL_UUID,
        GENERAL_CONDITION_UNWELL_UUID,
        GENERAL_CONDITION_CRITICAL_UUID,
        GENERAL_CONDITION_PALLIATIVE_UUID,
        GENERAL_CONDITION_CONVALESCENT_UUID,
        GENERAL_CONDITION_SUSPECTED_DEAD,
        GENERAL_CONDITION_CONFIRMED_DEAD,
        GENERAL_CONDITION_CURED,
        GENERAL_CONDITION_DISCHARGED_NON_CASE
    };

    /** UUID for the (question) concept for the temperature. */
    public static final String TEMPERATURE_UUID = "5088AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    /** UUID for the (question) concept for the weight. */
    public static final String WEIGHT_UUID = "5089AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    /** UUID for the (question) concept for Diarrhea. */
    public static final String DIARRHEA_UUID = "1aa247f3-2d83-4efc-94bc-123b1a71b19f";

    /** UUID for the (question) concept for (any) bleeding. */
    public static final String BLEEDING_UUID = "147241AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    // TODO: This may break localization.
    /** Group name for Bleeding Sites section. */
    public static final String BLEEDING_SITES_NAME = "Bleeding site";

    /** UUID for the (question) concept for Vomiting. */
    public static final String VOMITING_UUID = "405ad95d-f6e1-4023-a459-28cffdb055c5";

    /** UUID for the (question) concept for pain. */
    public static final String PAIN_UUID = "f75da5de-404c-42d0-b484-b69a4896e093";

    /** UUID for the (question) concept for best conscious state (AVPU). */
    public static final String RESPONSIVENESS_UUID = "162643AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    /** UUID for the (question) concept for (severe) weakness. */
    public static final String WEAKNESS_UUID = "5226AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    /** UUID for the (question) concept for appetite. */
    public static final String APPETITE_UUID = "777000003AAAAAAAAAAAAAAAAAAAAAAAAAAA";

    /** UUID for the (question) concept for oedema. */
    public static final String OEDEMA_UUID = "460AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    /** UUID for the (question) concept for the notes field. */
    public static final String NOTES_UUID = "162169AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    /** UUID for the (answer) concept None. */
    public static final String NONE_UUID = "1107AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    /** UUID for the (answer) concept Mild. */
    public static final String MILD_UUID = "1148AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    /** UUID for the (answer) concept Moderate. */
    public static final String MODERATE_UUID = "1499AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    /** UUID for the (answer) concept Severe. */
    public static final String SEVERE_UUID = "1500AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    /** UUID for the (question) concept for Mobility. */
    public static final String MOBILITY_UUID = "30143d74-f654-4427-bb92-685f68f92c15";

    /** UUID for the (answer) concept of Yes. */
    public static final String YES_UUID = "1065AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    /** UUID for the (answer) concept of No. */
    public static final String NO_UUID = "1066AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    /** UUID for the (answer) concept of the answer is unknown. */
    public static final String UNKNOWN_UUID = "1067AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    /** UUID for indicating a patient can eat solid food. */
    public static final String SOLID_FOOD_UUID = "159597AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    /** UUID for Normal. */
    public static final String NORMAL_UUID = "1115AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    /** Returns the {@link ResStatus} for the specified condition UUID. */
    public static ResStatus getResStatus(String conditionUuid) {
        if (conditionUuid == null) {
            return ResStatus.UNKNOWN;
        }

        switch (conditionUuid) {
            case ConceptUuids.GENERAL_CONDITION_WELL_UUID:
                return ResStatus.WELL;
            case ConceptUuids.GENERAL_CONDITION_UNWELL_UUID:
                return ResStatus.UNWELL;
            case ConceptUuids.GENERAL_CONDITION_CRITICAL_UUID:
                return ResStatus.CRITICAL;
            case ConceptUuids.GENERAL_CONDITION_PALLIATIVE_UUID:
                return ResStatus.PALLIATIVE;
            case ConceptUuids.GENERAL_CONDITION_CONVALESCENT_UUID:
                return ResStatus.CONVALESCENT;
            case ConceptUuids.GENERAL_CONDITION_DISCHARGED_NON_CASE:
                return ResStatus.DISCHARGED_NON_CASE;
            case ConceptUuids.GENERAL_CONDITION_CURED:
                return ResStatus.DISCHARGED_CURED;
            case ConceptUuids.GENERAL_CONDITION_SUSPECTED_DEAD:
                return ResStatus.SUSPECTED_DEAD;
            case ConceptUuids.GENERAL_CONDITION_CONFIRMED_DEAD:
                return ResStatus.CONFIRMED_DEAD;
            default:
                return ResStatus.UNKNOWN;
        }
    }

    private ConceptUuids() {
    }
}
