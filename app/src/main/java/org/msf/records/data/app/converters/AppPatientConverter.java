package org.msf.records.data.app.converters;

import android.database.Cursor;
import android.util.Log;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.LocalDate;
import org.msf.records.data.app.AppPatient;
import org.msf.records.sync.PatientProjection;
import org.msf.records.utils.Utils;

/**
 * A {@link AppTypeConverter} that converts {@link AppPatient}s.
 */
public class AppPatientConverter implements AppTypeConverter<AppPatient> {

    private static final String TAG = AppPatientConverter.class.getSimpleName();

    @Override
    public AppPatient fromCursor(Cursor cursor) {
        return AppPatient.builder()
                .setId(cursor.getString(PatientProjection.COLUMN_ID))
                .setUuid(cursor.getString(PatientProjection.COLUMN_UUID))
                .setGivenName(cursor.getString(PatientProjection.COLUMN_GIVEN_NAME))
                .setFamilyName(cursor.getString(PatientProjection.COLUMN_FAMILY_NAME))
                .setBirthdate(Utils.stringToLocalDate(
                        cursor.getString(PatientProjection.COLUMN_BIRTHDATE)))
                .setGender(getGenderFromString(
                        cursor.getString(PatientProjection.COLUMN_GENDER)))
                .setAdmissionDateTime(new DateTime(
                        cursor.getLong(PatientProjection.COLUMN_ADMISSION_TIMESTAMP) * 1000))
                .setLocationUuid(cursor.getString(PatientProjection.COLUMN_LOCATION_UUID))
                .build();
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
}
