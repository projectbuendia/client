package org.msf.records.model;

import java.io.Serializable;

/**
 * Created by Gil on 03/10/2014.
 */
public class Patient implements Serializable {
    // Internal identifier.
    public String uuid;
    // User-specified identifier.
    public String id;
    public String given_name;
    public String family_name;

    public String important_information;

    /**
     * Accepted values:
     * suspected, probable, confirmed, non-case, convalescent,
     * can_be_discharged, dischraged, suspected_dead, confirmed_dead
     */
    public String status;

    public Boolean pregnant;

    // Must be "M" or "F".
    public String gender;

    @Deprecated
    public String movement;
    @Deprecated
    public String eating;

    public Long admission_timestamp;
    public Long created_timestamp;
    public Long first_showed_symptoms_timestamp;

    public String origin_location;
    // Not yet ready.
    public String next_of_kin;

    public PatientLocation assigned_location;
    public PatientAge age;
}
