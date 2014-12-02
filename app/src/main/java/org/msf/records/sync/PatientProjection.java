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
            PatientProviderContract.PatientColumns.COLUMN_NAME_STATUS,
            PatientProviderContract.PatientColumns.COLUMN_NAME_ADMISSION_TIMESTAMP,
            PatientProviderContract.PatientColumns.COLUMN_NAME_LOCATION_ZONE,
            PatientProviderContract.PatientColumns.COLUMN_NAME_LOCATION_TENT,
            PatientProviderContract.PatientColumns.COLUMN_NAME_AGE_MONTHS,
            PatientProviderContract.PatientColumns.COLUMN_NAME_AGE_YEARS,
            PatientProviderContract.PatientColumns.COLUMN_NAME_GENDER,
    };

    public static final int COLUMN_ID = 0;
    public static final int COLUMN_GIVEN_NAME = 1;
    public static final int COLUMN_FAMILY_NAME = 2;
    public static final int COLUMN_UUID = 3;
    public static final int COLUMN_STATUS = 4;
    public static final int COLUMN_ADMISSION_TIMESTAMP = 5;
    public static final int COLUMN_LOCATION_ZONE = 6;
    public static final int COLUMN_LOCATION_TENT = 7;
    public static final int COLUMN_AGE_MONTHS = 8;
    public static final int COLUMN_AGE_YEARS = 9;
    public static final int COLUMN_GENDER = 10;

    public static String[] getProjectionColumns() {
        return PROJECTION;
    }
}
