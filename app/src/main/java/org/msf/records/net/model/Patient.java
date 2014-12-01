package org.msf.records.net.model;

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

    public Patient() {}

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
