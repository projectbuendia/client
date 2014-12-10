package org.msf.records.data.app.converters;

import android.database.Cursor;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.msf.records.data.app.AppPatient;
import org.msf.records.sync.PatientProjection;

/**
 * A {@link AppTypeConverter} that converts {@link AppPatient}s.
 */
public class AppPatientConverter implements AppTypeConverter<AppPatient> {

    @Override
    public AppPatient fromCursor(Cursor cursor) {
        AppPatient patient = new AppPatient();

        patient.mId = cursor.getString(PatientProjection.COLUMN_ID);

        patient.mUuid = cursor.getString(PatientProjection.COLUMN_UUID);
        patient.mGivenName = cursor.getString(PatientProjection.COLUMN_GIVEN_NAME);
        patient.mFamilyName = cursor.getString(PatientProjection.COLUMN_FAMILY_NAME);
        patient.mAge = getAgeFromYearsAndMonths(
                cursor.getInt(PatientProjection.COLUMN_AGE_YEARS),
                cursor.getInt(PatientProjection.COLUMN_AGE_MONTHS));
        patient.mGender = getGenderFromString(cursor.getString(PatientProjection.COLUMN_GENDER));

        patient.mAdmissionDateTime =
                new DateTime(cursor.getLong(PatientProjection.COLUMN_ADMISSION_TIMESTAMP) * 1000);

        patient.mLocationUuid = cursor.getString(PatientProjection.COLUMN_LOCATION_UUID);

        return patient;
    }

    private static int getGenderFromString(String genderString) {
        if ("M".equals(genderString)) {
            return AppPatient.GENDER_MALE;
        } else if ("F".equals(genderString)) {
            return AppPatient.GENDER_FEMALE;
        } else {
            return AppPatient.GENDER_UNKNOWN;
        }
    }

    private static Period getAgeFromYearsAndMonths(int years, int months) {
        if (years != 0 && months != 0) {
            // TODO(dxchen0: This indicates a data error. Decide the right thing to do.
            return Period.years(years);
        }

        if (years != 0) {
            return Period.years(years);
        } else {
            return Period.months(months);
        }
    }
}
