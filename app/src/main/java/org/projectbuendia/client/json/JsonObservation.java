package org.projectbuendia.client.json;

import org.joda.time.DateTime;

/**
 * An plain-object representation of an observation received from the server.
 */
public class JsonObservation {
    public String uuid;
    public String patient_uuid;
    public String encounter_uuid;
    public DateTime timestamp;
    public String concept_uuid;
    public String value;
    public boolean voided;
}
