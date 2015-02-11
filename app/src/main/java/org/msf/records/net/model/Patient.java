package org.msf.records.net.model;

import com.google.common.base.MoreObjects;

import org.joda.time.LocalDate;

import java.io.Serializable;

/**
 * An object that represents a patient.
 */
public class Patient implements Serializable {
    public static final int GENDER_UNKNOWN = 0;
    public static final int GENDER_MALE = 1;
    public static final int GENDER_FEMALE = 2;

    // Internal identifier.
    public String uuid;
    // User-specified identifier.
    public String id;
    public String given_name;
    public String family_name;

    public String important_information;

    public String gender;  // must be "M" or "F"
    public LocalDate birthdate;

    // All timestamps are in seconds since 1970-01-01 00:00 UTC.
    public Long admission_timestamp;
    public Long created_timestamp;
    public Long first_showed_symptoms_timestamp;

    public String origin_location;
    // Not yet ready.
    public String next_of_kin;

    public Location assigned_location;

    public Patient() {}

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("uuid", uuid)
                .add("id", id)
                .add("given_name", given_name)
                .add("family_name", family_name)
                .add("important_information", important_information)
                .add("gender", gender)
                .add("birthdate", birthdate.toString())
                .add("admission_timestamp", admission_timestamp)
                .add("created_timestamp", created_timestamp)
                .add("first_showed_symptoms_timestamp", first_showed_symptoms_timestamp)
                .add("assigned_location", assigned_location)
                .toString();
    }

    /**
     * Overwrite the fields in this GSON object with everything non-null in the source.
     * assigned_location is overwritten completely (not merged).
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
        if (source.gender != null) {
            this.gender = source.gender;
        }
        if (source.birthdate != null) {
            this.birthdate = source.birthdate;
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
    }
}
