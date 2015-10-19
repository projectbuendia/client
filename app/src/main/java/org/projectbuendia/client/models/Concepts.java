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

/**
 * Defines concept UUIDs expected to exist on the OpenMRS server.  Over time, values in this file
 * should be phased out and replaced with modular configuration, either on the server or the client.
 */
public interface Concepts {
    String CONSCIOUS_STATE_UUID = "162643AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    String FLUIDS_UUID = "e96f504e-229a-4933-84d1-358abbd687e3";
    String CONDITION_UUID = "a3657203-cfed-44b8-8e3f-960f8d4cf3b3";
    String HYDRATION_UUID = "162653AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    String IV_UUID = "f50c9c63-3ff9-4c26-9d18-12bfc58a3d07";
    String PREGNANCY_UUID = "5272AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    String PULSE_UUID = "5087AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    String RESPIRATION_UUID = "5242AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    String PCR_NP_UUID = "162826AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    String PCR_L_UUID = "162827AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    String FIRST_SYMPTOM_DATE_UUID = "1730AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    String ADMISSION_DATE_UUID = "162622AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    /** UUID for the (question) concept for the temperature. */
    String TEMPERATURE_UUID = "5088AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    /** UUID for the (question) concept for the weight. */
    String WEIGHT_UUID = "5089AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    /** UUID for the (question) concept for Diarrhea. */
    String DIARRHEA_UUID = "1aa247f3-2d83-4efc-94bc-123b1a71b19f";

    /** UUID for the (question) concept for (any) bleeding. */
    String BLEEDING_UUID = "147241AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    // TODO: This may break localization.
    /** Group name for Bleeding Sites section. */
    String BLEEDING_SITES_NAME = "Bleeding site";

    /** UUID for the (question) concept for Vomiting. */
    String VOMITING_UUID = "405ad95d-f6e1-4023-a459-28cffdb055c5";

    /** UUID for the (question) concept for pain. */
    String PAIN_UUID = "f75da5de-404c-42d0-b484-b69a4896e093";

    /** UUID for the (question) concept for best conscious state (AVPU). */
    String RESPONSIVENESS_UUID = "162643AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    /** UUID for the (question) concept for (severe) weakness. */
    String WEAKNESS_UUID = "5226AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    /** UUID for the (question) concept for appetite. */
    String APPETITE_UUID = "777000003AAAAAAAAAAAAAAAAAAAAAAAAAAA";

    /** UUID for the (question) concept for oedema. */
    String OEDEMA_UUID = "460AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    /** UUID for the (question) concept for the notes field. */
    String NOTES_UUID = "162169AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    /** UUID for the (answer) concept None. */
    String NONE_UUID = "1107AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    /** UUID for the (answer) concept Mild. */
    String MILD_UUID = "1148AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    /** UUID for the (answer) concept Moderate. */
    String MODERATE_UUID = "1499AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    /** UUID for the (answer) concept Severe. */
    String SEVERE_UUID = "1500AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    /** UUID for the (question) concept for Mobility. */
    String MOBILITY_UUID = "30143d74-f654-4427-bb92-685f68f92c15";

    /** UUID for the (answer) concept of Yes. */
    String YES_UUID = "1065AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    /** UUID for the (answer) concept of No. */
    String NO_UUID = "1066AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    /** UUID for the (answer) concept of the answer is unknown. */
    String UNKNOWN_UUID = "1067AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    /** UUID for indicating a patient can eat solid food. */
    String SOLID_FOOD_UUID = "159597AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    /** UUID for Normal. */
    String NORMAL_UUID = "1115AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
}
