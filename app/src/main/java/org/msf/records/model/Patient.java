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

    public Patient(){

    }

    public Patient(String uuid, String id, String given_name, String family_name, String important_information,
                   String status, Boolean pregnant, String gender, Long admission_timestamp, Long created_timestamp,
                   Long first_showed_symptoms_timestamp, String origin_location, String next_of_kin,
                   PatientLocation assigned_location, PatientAge age) {
        this.uuid = uuid;
        this.id = id;
        this.given_name = given_name;
        this.family_name = family_name;
        this.important_information = important_information;
        this.status = status;
        this.pregnant = pregnant;
        this.gender = gender;
        this.admission_timestamp = admission_timestamp;
        this.created_timestamp = created_timestamp;
        this.first_showed_symptoms_timestamp = first_showed_symptoms_timestamp;
        this.origin_location = origin_location;
        this.next_of_kin = next_of_kin;
        this.assigned_location = assigned_location;
        this.age = age;
    }

    /**
     * Overwrite the fields in this GSON object with everything non-null in the source.
     * PatientAge and PatientLocation are overwritten completely (not merged)
     *
     * @param source the source of changes
     */
    public void writeFrom(Patient source) {
        // TODO(nfortescue): Do this with a nice reflection based loop
        if (source.uuid != null) {
            if (!source.uuid.equals(this.uuid)) {
                throw new IllegalArgumentException("Cannot overwrite with different uuid");
            }
        }
        if (source.id != null) {
            if (!source.id.equals(this.id)) {
                throw new IllegalArgumentException("Cannot overwrite with different id");
            }
        }
        if (source.given_name != null) {
            this.given_name = source.given_name;
        }
        if (source.family_name != null) {
            this.family_name = source.family_name;
        }
        if (source.important_information != null) {
            this.important_information = source.important_information;
        }
        if (source.status != null) {
            this.status = source.status;
        }
        if (source.pregnant != null) {
            this.pregnant = source.pregnant;
        }
        if (source.gender != null) {
            this.gender = source.gender;
        }
        if (source.admission_timestamp != null) {
            this.admission_timestamp = source.admission_timestamp;
        }
        if (source.created_timestamp != null) {
            this.created_timestamp = source.created_timestamp;
        }
        if (source.first_showed_symptoms_timestamp != null) {
            this.first_showed_symptoms_timestamp = source.first_showed_symptoms_timestamp;
        }
        // Deliberately do not merge location recursively, as you should set it all at once.
        if (source.assigned_location != null) {
            this.assigned_location = source.assigned_location;
        }
        // Deliberately do not merge age recursively, as you should set it all at once.
        if (source.age != null) {
            this.age = source.age;
        }
    }
}
