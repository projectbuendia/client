package org.msf.records.net.model;

/**
 * A simple Java bean representing a JSON object used to encode information about encounters
 * (between a patient and clinician) and the observations made there.
 *
 * Before use must have {@see CustomeSerialization.registerTo(GsonBuilder)}.
 */
public class PatientChart {

    public String uuid;
    public Encounter[] encounters;
}
