package org.msf.records.net.model;

import java.util.Map;

/**
 * A single medical concept, usually a question on answer in an observation on a patient. A simple
 * Java bean for GSON converting to and from a JSON encoding. Stores localization and type
 * information.
 */
public class Concept {

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
    public static final String MOBILITY_WALKING_UUID = "c2a547f7-6329-4273-80c2-eae804897efd";
    /**
     * UUID for the (answer) concept Walking with Difficulty, used for Mobility.
     */
    public static final String MOBILITY_WALKING_WITH_DIFFICULTY_UUID =
            "b5267d48-2456-4ffc-bbf7-4ab2f7c6f7c3";
    /**
     * UUID for the (answer) concept Assisted, used for Mobility.
     */
    public static final String MOBILITY_ASSISTED_UUID = "765d620f-5db3-47ec-9884-9e32f8e978a9";
    /**
     * UUID for the (answer) concept Bed-Bound, used for Mobility.
     */
    public static final String MOBILITY_BED_BOUND_UUID = "3ac0cbb9-f52b-453e-b867-f3cca2e804a3";


    public String uuid;
    /**
     * The server side id. Prefer the UUID for sending to the server, but this is needed for some
     * xforms tasks.
     */
    public Integer xform_id;
    public ConceptType type;

    /**
     * A map from locales to the name in that locale. Eg en->heart, fr->couer, ...
     */
    public Map<String, String> names;
}
