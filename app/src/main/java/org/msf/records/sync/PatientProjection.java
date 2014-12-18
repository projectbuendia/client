package org.msf.records.sync;

/**
 * Provides a standard patient projection with all known fields.
 * Since most projections require most fields, using this projection
 * prevents the propagation of custom projections.
 */
public class PatientProjection {
    private static final String[] PROJECTION = new String[] {
            PatientProviderContract.PatientColumns._ID,
            PatientProviderContract.PatientColumns.COLUMN_NAME_GIVEN_NAME,
            PatientProviderContract.PatientColumns.COLUMN_NAME_FAMILY_NAME,
            PatientProviderContract.PatientColumns.COLUMN_NAME_UUID,
            PatientProviderContract.PatientColumns.COLUMN_NAME_ADMISSION_TIMESTAMP,
            PatientProviderContract.PatientColumns.COLUMN_NAME_BIRTHDATE,
            PatientProviderContract.PatientColumns.COLUMN_NAME_GENDER,
            PatientProviderContract.PatientColumns.COLUMN_NAME_LOCATION_UUID
    };

    private static final String[] PATIENT_COUNTS_PROJECTION = new String[] {
            LocationProviderContract.LocationColumns._ID,
            PatientProviderContract.PatientColumns.COLUMN_NAME_LOCATION_UUID,
            PatientProviderContract.PatientColumns.COLUMN_NAME_TENT_PATIENT_COUNT
    };

    public static final int COLUMN_ID = 0;
    public static final int COLUMN_GIVEN_NAME = 1;
    public static final int COLUMN_FAMILY_NAME = 2;
    public static final int COLUMN_UUID = 3;
    public static final int COLUMN_ADMISSION_TIMESTAMP = 4;
    public static final int COLUMN_BIRTHDATE = 5;
    public static final int COLUMN_GENDER = 6;
    public static final int COLUMN_LOCATION_UUID = 7;

    public static final int COUNTS_COLUMN_ID = 0;
    public static final int COUNTS_COLUMN_LOCATION_UUID = 1;
    public static final int COUNTS_COLUMN_TENT_PATIENT_COUNT = 2;

    public static String[] getProjectionColumns() {
        return PROJECTION;
    }

    public static String[] getPatientCountsProjection() { return PATIENT_COUNTS_PROJECTION; }
}
