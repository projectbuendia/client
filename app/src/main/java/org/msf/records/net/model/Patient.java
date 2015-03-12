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
}
