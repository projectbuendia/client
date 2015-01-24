package org.msf.records.model;

import org.msf.records.data.res.ResStatus;

public class Concept {
    public static final String CONSCIOUS_STATE_UUID = "162643AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static final String FLUIDS_UUID = "e96f504e-229a-4933-84d1-358abbd687e3";
    public static final String GENERAL_CONDITION_UUID = "a3657203-cfed-44b8-8e3f-960f8d4cf3b3";
    public static final String HYDRATION_UUID = "162653AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static final String IV_UUID = "f50c9c63-3ff9-4c26-9d18-12bfc58a3d07";
    public static final String MOBILITY_UUID = "30143d74-f654-4427-bb92-685f68f92c15";
    public static final String PREGNANCY_UUID = "5272AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static final String TEMPERATURE_UUID = "5088AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static final String PULSE_UUID = "5087AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static final String RESPIRATION_UUID = "5242AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static final String PAIN_UUID = "f75da5de-404c-42d0-b484-b69a4896e093";
    public static final String PCR_NP_UUID = "ab2be5ca-2c61-4cda-9890-36fff0313821";
    public static final String PCR_L_UUID = "ec513d2c-d86c-4aec-ab45-0a0a89f2a0b8";

    public static final String YES_UUID = "1065AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static final String NO_UUID = "1066AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static final String UNKNOWN_UUID = "1067AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

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
     * Returns the {@link ResStatus} for the specified condition UUID.
     */
    public static ResStatus getResStatus(String conditionUuid) {
        if (conditionUuid == null) {
            return ResStatus.UNKNOWN;
        }

        switch (conditionUuid) {
            case Concept.GENERAL_CONDITION_WELL_UUID:
                return ResStatus.WELL;
            case Concept.GENERAL_CONDITION_UNWELL_UUID:
                return ResStatus.UNWELL;
            case Concept.GENERAL_CONDITION_CRITICAL_UUID:
                return ResStatus.CRITICAL;
            case Concept.GENERAL_CONDITION_PALLIATIVE_UUID:
                return ResStatus.PALLIATIVE;
            case Concept.GENERAL_CONDITION_CONVALESCENT_UUID:
                return ResStatus.CONVALESCENT;
            case Concept.GENERAL_CONDITION_DISCHARGED_NON_CASE:
                return ResStatus.DISCHARGED_NON_CASE;
            case Concept.GENERAL_CONDITION_CURED:
                return ResStatus.DISCHARGED_CURED;
            case Concept.GENERAL_CONDITION_SUSPECTED_DEAD:
                return ResStatus.SUSPECTED_DEAD;
            case Concept.GENERAL_CONDITION_CONFIRMED_DEAD:
                return ResStatus.CONFIRMED_DEAD;
            default:
                return ResStatus.UNKNOWN;
        }
    }

    private Concept() {}
}
