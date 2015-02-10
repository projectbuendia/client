package org.msf.records.net.model;

import org.joda.time.DateTime;

import java.util.Map;

/**
 * A single encounter between a patient and a clinician in a patient chart. A simple Java bean
 * for GSON converting to and from a JSON encoding.
 *
 * Before use must have {@see CustomeSerialization.registerTo(GsonBuilder)}.
 */
public class Encounter {
    /**
     * The encounterUuid of the encounter.
     */
    public String uuid;
    public DateTime timestamp;
    public String enterer_id;
    /**
     * Keys are encounterUuid strings for the concept representing the concept observed. Values are the
     * value observed. To find out what the type is the type in the concept dictionary must be
     * inspected. Common values are doubles and String representing concept uuids of coded concepts.
     */
    public Map<Object, Object> observations;

}
