package org.msf.records.model;

import org.msf.records.data.res.ResStatus;

public class Concepts {
    public static final String CONSCIOUS_STATE_UUID = "162643AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static final String FLUIDS_UUID = "e96f504e-229a-4933-84d1-358abbd687e3";
    public static final String GENERAL_CONDITION_UUID = "a3657203-cfed-44b8-8e3f-960f8d4cf3b3";
    public static final String HYDRATION_UUID = "162653AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static final String IV_UUID = "f50c9c63-3ff9-4c26-9d18-12bfc58a3d07";
    public static final String PREGNANCY_UUID = "5272AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static final String PULSE_UUID = "5087AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static final String RESPIRATION_UUID = "5242AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static final String PCR_NP_UUID = "162826AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static final String PCR_L_UUID = "162827AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
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
    /**
     * UUID for the (question) concept for the temperature.
     */
    public static final String TEMPERATURE_UUID = "5088AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    /**
     * UUID for the (question) concept for the weight.
     */
    public static final String WEIGHT_UUID = "5089AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    /**
     * UUID for the (question) concept for Diarrhea.
     */
    public static final String DIARRHEA_UUID = "1aa247f3-2d83-4efc-94bc-123b1a71b19f";
    /**
     * UUID for the (question) concept for (any) bleeding.
     */
    public static final String BLEEDING_UUID = "147241AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    /**
     * Group name for Bleeding Sites section.
     * TODO(akalachman): This may break localization.
     */
    public static final String BLEEDING_SITES_NAME = "Bleeding site";
    /**
     * UUID for the (question) concept for Vomiting.
     */
    public static final String VOMITING_UUID = "405ad95d-f6e1-4023-a459-28cffdb055c5";
    /**
     * UUID for the (question) concept for pain.
     */
    public static final String PAIN_UUID = "f75da5de-404c-42d0-b484-b69a4896e093";
    /**
     * UUID for the (question) concept for best conscious state (AVPU).
     */
    public static final String RESPONSIVENESS_UUID = "162643AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    /**
     * UUID for the (question) concept for (severe) weakness.
     */
    public static final String WEAKNESS_UUID = "5226AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    /**
     * UUID for the (question) concept for the notes field.
     */
    public static final String NOTES_UUID = "162169AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    /**
     * UUID for the (answer) concept None, used for Diarrhea and vomiting.
     */
    public static final String NONE_UUID = "1107AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    /**
     * UUID for the (answer) concept Mild, used for Diarrhea and vomiting.
     */
    public static final String MILD_UUID = "1498AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    /**
     * UUID for the (answer) concept Moderate, used for Diarrhea and vomiting.
     */
    public static final String MODERATE_UUID = "1499AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    /**
     * UUID for the (answer) concept Moderate, used for Diarrhea and vomiting.
     */
    public static final String SEVERE_UUID = "1500AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    /**
     * UUID for the (answer) concept Alert, used for Best Conscious State (AVPU).
     */
    public static final String RESPONSIVENESS_ALERT_UUID = "160282AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    /**
     * UUID for the (answer) concept Responds to Voice, used for Best Conscious State (AVPU).
     */
    public static final String RESPONSIVENESS_VOICE_UUID = "162645AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    /**
     * UUID for the (answer) concept Responds to Pain, used for Best Conscious State (AVPU).
     */
    public static final String RESPONSIVENESS_PAIN_UUID = "162644AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    /**
     * UUID for the (answer) concept Unresponsive, used for Best Conscious State (AVPU).
     */
    public static final String RESPONSIVENESS_UNRESPONSIVE_UUID =
            "159508AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    /**
     * UUID for the (question) concept for Mobility.
     */
    public static final String MOBILITY_UUID = "30143d74-f654-4427-bb92-685f68f92c15";
    /**
     * UUID for the (answer) concept Walking, used for Mobility.
     */
    public static final String MOBILITY_WALKING_UUID = "162750AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    /**
     * UUID for the (answer) concept Walking with Difficulty, used for Mobility.
     */
    public static final String MOBILITY_WALKING_WITH_DIFFICULTY_UUID =
            "b5267d48-2456-4ffc-bbf7-4ab2f7c6f7c3";
    /**
     * UUID for the (answer) concept Assisted, used for Mobility.
     */
    public static final String MOBILITY_ASSISTED_UUID = "162751AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    /**
     * UUID for the (answer) concept Bed-Bound, used for Mobility.
     */
    public static final String MOBILITY_BED_BOUND_UUID = "162752AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    /**
     * UUID for the (answer) concept of Yes.
     */
    public static final String YES_UUID = "1065AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    /**
     * UUID for the (answer) concept of No.
     */
    public static final String NO_UUID = "1066AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    /**
     * UUID for the (answer) concept of the answer is unknown.
     */
    public static final String UNKNOWN_UUID = "1067AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    /**
     * UUID for indicating a patient can eat solid food.
     */
    public static final String SOLID_FOOD_UUID = "159597AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    /**
     * UUID for Normal.
     */
    public static final String NORMAL_UUID = "1115AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    /**
     * Returns the {@link ResStatus} for the specified condition UUID.
     */
    public static ResStatus getResStatus(String conditionUuid) {
        if (conditionUuid == null) {
            return ResStatus.UNKNOWN;
        }

        switch (conditionUuid) {
            case Concepts.GENERAL_CONDITION_WELL_UUID:
                return ResStatus.WELL;
            case Concepts.GENERAL_CONDITION_UNWELL_UUID:
                return ResStatus.UNWELL;
            case Concepts.GENERAL_CONDITION_CRITICAL_UUID:
                return ResStatus.CRITICAL;
            case Concepts.GENERAL_CONDITION_PALLIATIVE_UUID:
                return ResStatus.PALLIATIVE;
            case Concepts.GENERAL_CONDITION_CONVALESCENT_UUID:
                return ResStatus.CONVALESCENT;
            case Concepts.GENERAL_CONDITION_DISCHARGED_NON_CASE:
                return ResStatus.DISCHARGED_NON_CASE;
            case Concepts.GENERAL_CONDITION_CURED:
                return ResStatus.DISCHARGED_CURED;
            case Concepts.GENERAL_CONDITION_SUSPECTED_DEAD:
                return ResStatus.SUSPECTED_DEAD;
            case Concepts.GENERAL_CONDITION_CONFIRMED_DEAD:
                return ResStatus.CONFIRMED_DEAD;
            default:
                return ResStatus.UNKNOWN;
        }
    }

    private Concepts() {}
}
