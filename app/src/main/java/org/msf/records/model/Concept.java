package org.msf.records.model;

import org.msf.records.R;

public class Concept {
    public static final String CONSCIOUS_STATE_UUID = "162643AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static final String FLUIDS_UUID = "e96f504e-229a-4933-84d1-358abbd687e3";
    public static final String GENERAL_CONDITION_UUID = "a3657203-cfed-44b8-8e3f-960f8d4cf3b3";
    public static final String HYDRATION_UUID = "162653AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static final String IV_UUID = "f50c9c63-3ff9-4c26-9d18-12bfc58a3d07";
    public static final String MOBILITY_UUID = "30143d74-f654-4427-bb92-685f68f92c15";
    public static final String PREGNANCY_UUID = "5272AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static final String TEMPERATURE_UUID = "5088AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    public static final String YES_UUID = "1065AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static final String NO_UUID = "1066AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static final String UNKNOWN_UUID = "1067AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    public static final String GENERAL_CONDITION_VERY_POOR_UUID =
            "2827e7ac-10c1-4d3f-9fa4-0239771d8548";
    public static final String GENERAL_CONDITION_POOR_UUID =
            "162132AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static final String GENERAL_CONDITION_FAIR_UUID = "162133AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    public static final String GENERAL_CONDITION_GOOD_UUID = "ae03f060-e6af-4390-a22a-eaabdb54ad69";

    public static int getColorResourceForGeneralCondition(String generalCondition) {
        if (generalCondition == null) {
            return R.color.general_condition_unknown;
        }

        switch (generalCondition) {
            case Concept.GENERAL_CONDITION_GOOD_UUID:
                return R.color.general_condition_good;
            case Concept.GENERAL_CONDITION_FAIR_UUID:
                return R.color.general_condition_fair;
            case Concept.GENERAL_CONDITION_POOR_UUID:
                return R.color.general_condition_poor;
            case Concept.GENERAL_CONDITION_VERY_POOR_UUID:
                return R.color.general_condition_very_poor;
            default:
                return R.color.general_condition_unknown;
        }
    }
}
