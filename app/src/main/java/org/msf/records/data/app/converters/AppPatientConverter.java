package org.msf.records.data.app.converters;

import android.database.Cursor;

import javax.annotation.concurrent.Immutable;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.msf.records.data.app.AppPatient;
import org.msf.records.sync.PatientProjection;

/**
 * A {@link AppTypeConverter} that converts {@link AppPatient}s.
 */
@Immutable
public final class AppPatientConverter implements AppTypeConverter<AppPatient> {

    @Override
    public AppPatient fromCursor(Cursor cursor) {
    	return AppPatient.builder()
        		.setId(cursor.getString(PatientProjection.COLUMN_ID))
        		.setUuid(cursor.getString(PatientProjection.COLUMN_UUID))
        		.setGivenName(cursor.getString(PatientProjection.COLUMN_GIVEN_NAME))
    			.setFamilyName(cursor.getString(PatientProjection.COLUMN_FAMILY_NAME))
        		.setAge(
    					getAgeFromYearsAndMonths(
    							cursor.getInt(PatientProjection.COLUMN_AGE_YEARS),
    							cursor.getInt(PatientProjection.COLUMN_AGE_MONTHS)))
				.setGender(
						getGenderFromString(cursor.getString(PatientProjection.COLUMN_GENDER)))
				.setAdmissiondateTime(
						new DateTime(cursor.getLong(PatientProjection.COLUMN_ADMISSION_TIMESTAMP) * 1000))
				.setLocationUuid(
						cursor.getString(PatientProjection.COLUMN_LOCATION_UUID))
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

    private static Duration getAgeFromYearsAndMonths(int years, int months) {
        if (years != 0 && months != 0) {
            // TODO(dxchen0: This indicates a data error. Decide the right thing to do.
            return Duration.standardDays(years * 365);
        }

        if (years != 0) {
            return Duration.standardDays(years * 365);
        } else {
            return Duration.standardDays(months * 30);
        }
    }
}
