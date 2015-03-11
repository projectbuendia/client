package org.msf.records.sync;

import org.msf.records.sync.providers.Contracts;

/**
 * Provides a standard patient projection with all known fields.
 * Since most projections require most fields, using this projection
 * prevents the propagation of custom projections.
 */
public class PatientProjection {
    private static final String[] PROJECTION = new String[] {
            Contracts.Patients._ID,
            Contracts.Patients.GIVEN_NAME,
            Contracts.Patients.FAMILY_NAME,
            Contracts.Patients.UUID,
            Contracts.Patients.BIRTHDATE,
            Contracts.Patients.GENDER,
            Contracts.Patients.LOCATION_UUID
    };

    private static final String[] PATIENT_COUNTS_PROJECTION = new String[] {
            Contracts.Locations._ID,
            Contracts.PatientCounts.LOCATION_UUID,
            Contracts.PatientCounts.TENT_PATIENT_COUNT
    };

    public static final int COLUMN_ID = 0;
    public static final int COLUMN_GIVEN_NAME = 1;
    public static final int COLUMN_FAMILY_NAME = 2;
    public static final int COLUMN_UUID = 3;
    public static final int COLUMN_BIRTHDATE = 4;
    public static final int COLUMN_GENDER = 5;
    public static final int COLUMN_LOCATION_UUID = 6;

    public static final int COUNTS_COLUMN_ID = 0;
    public static final int COUNTS_COLUMN_LOCATION_UUID = 1;
    public static final int COUNTS_COLUMN_TENT_PATIENT_COUNT = 2;

    public static String[] getProjectionColumns() {
        return PROJECTION;
    }

    public static String[] getPatientCountsProjection() { return PATIENT_COUNTS_PROJECTION; }
}
