package org.msf.records.model;

import java.io.Serializable;

/**
 * Created by Gil on 03/10/2014.
 */
public class Patient implements Serializable {


    public String id;
    public String given_name;
    public String family_name;

    public String important_information;
    public String status;
    public String pregnancy_start_date;

    public String gender;
    public String movement;
    public String eating;

    public Long created_timestamp_utc;
    public Long first_showed_symptoms_timestamp_utc;

    public String origin_location;
    public String next_of_kin;


    public PatientLocation assigned_location;
    public PatientAge age;

}
