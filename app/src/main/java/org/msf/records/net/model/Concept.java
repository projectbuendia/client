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
