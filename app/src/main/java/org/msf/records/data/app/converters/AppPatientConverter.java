package org.msf.records.data.app.converters;

import android.database.Cursor;

import org.joda.time.DateTime;
import org.msf.records.data.app.AppPatient;
import org.msf.records.sync.PatientProjection;
import org.msf.records.utils.Utils;

import javax.annotation.concurrent.Immutable;

/**
 * A {@link AppTypeConverter} that converts {@link AppPatient}s.
 */
@Immutable
public class AppPatientConverter implements AppTypeConverter<AppPatient> {

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
                .setLocationUuid(cursor.getString(PatientProjection.COLUMN_LOCATION_UUID))
                .build();
    }

    private static int getGenderFromString(String genderString) {
        switch (genderString) {
            case "M":
                return AppPatient.GENDER_MALE;
            case "F":
                return AppPatient.GENDER_FEMALE;
            default:
                return AppPatient.GENDER_UNKNOWN;
        }
    }
}
