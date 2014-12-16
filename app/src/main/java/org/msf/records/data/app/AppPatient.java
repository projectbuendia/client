package org.msf.records.data.app;

import android.content.ContentValues;
import android.util.Log;

import com.google.common.base.Optional;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONException;
import org.json.JSONObject;
import org.msf.records.model.Zone;
import org.msf.records.net.Server;
import org.msf.records.net.model.Patient;
import org.msf.records.sync.PatientProviderContract;

import javax.annotation.concurrent.Immutable;

/**
 * Represents a patient in the app model.
 */
@Immutable
public final class AppPatient extends AppTypeBase<String> {

    private static final String TAG = AppPatient.class.getSimpleName();

    private static final DateTimeFormatter BIRTHDATE_FORMATTER =
            DateTimeFormat.forPattern("yyyy-MM-dd");

    public static final int GENDER_UNKNOWN = 0;
    public static final int GENDER_MALE = 1;
    public static final int GENDER_FEMALE = 2;

    public final String uuid;
    public final String givenName;
    public final String familyName;
    public final int gender;
    public final Duration age;
    public final DateTime admissionDateTime;
    public final String locationUuid;

    private AppPatient(Builder builder) {
    	this.id = builder.id;
    	this.uuid = builder.uuid;
    	this.givenName = builder.givenName;
    	this.familyName = builder.familyName;
        this.gender = builder.gender;
    	this.age = builder.age;
    	this.admissionDateTime = builder.admissionDateTime;
    	this.locationUuid = builder.locationUuid;
    }

    public static AppPatient fromNet(Patient patient) {
        AppPatient.Builder builder = AppPatient.builder();
        builder.id = patient.id;
        builder.uuid = patient.uuid;
        builder.givenName = patient.given_name;
        builder.familyName = patient.family_name;
        builder.gender = "M".equals(patient.gender) ? GENDER_MALE : GENDER_FEMALE;
        builder.age = patient.age == null ? null : patient.age.toDuration();
        builder.locationUuid = (patient.assigned_location == null) ?
                Zone.DEFAULT_LOCATION :
                patient.assigned_location.uuid;

        return builder.build();
    }

    public ContentValues toContentValues() {
        ContentValues contentValues = new ContentValues();

        contentValues.put(
                PatientProviderContract.PatientColumns._ID,
                id);
        contentValues.put(
                PatientProviderContract.PatientColumns.COLUMN_NAME_UUID,
                uuid);
        contentValues.put(
                PatientProviderContract.PatientColumns.COLUMN_NAME_GIVEN_NAME,
                givenName);
        contentValues.put(
                PatientProviderContract.PatientColumns.COLUMN_NAME_FAMILY_NAME,
                familyName);
        contentValues.put(
                PatientProviderContract.PatientColumns.COLUMN_NAME_GENDER,
                gender == Patient.GENDER_MALE ? "M" : "F");
        Period period = new Period(age, DateTime.now());
        if (period.getYears() >= 2) {
            contentValues.put(
                    PatientProviderContract.PatientColumns.COLUMN_NAME_AGE_YEARS,
                    period.getYears());
        } else {
            contentValues.put(
                    PatientProviderContract.PatientColumns.COLUMN_NAME_AGE_MONTHS,
                    period.getYears() * 12 + period.getMonths());
        }
        contentValues.put(
                PatientProviderContract.PatientColumns.COLUMN_NAME_ADMISSION_TIMESTAMP,
                admissionDateTime == null ? null : admissionDateTime.getMillis());
        contentValues.put(
                PatientProviderContract.PatientColumns.COLUMN_NAME_LOCATION_UUID,
                locationUuid);

        return contentValues;
    }

    public static class Delta {

        public Optional<String> id = Optional.absent();
        public Optional<String> givenName = Optional.absent();
        public Optional<String> familyName = Optional.absent();
        public Optional<Integer> gender = Optional.absent();
        public Optional<DateTime> birthdate = Optional.absent();

        public Optional<String> assignedLocationUuid = Optional.absent();

        /**
         * Serializes the fields changed in the delta to a {@link JSONObject}.
         *
         * @return whether serialization succeeded
         */
        public boolean toJson(JSONObject json) {
            try {
                if (id.isPresent()) {
                    json.put(Server.PATIENT_ID_KEY, id.get());
                }
                if (givenName.isPresent()) {
                    json.put(Server.PATIENT_GIVEN_NAME_KEY, givenName.get());
                }
                if (familyName.isPresent()) {
                    json.put(Server.PATIENT_FAMILY_NAME_KEY, familyName.get());
                }
                if (gender.isPresent()) {
                    json.put(
                            Server.PATIENT_GENDER_KEY,
                            gender.get() == Patient.GENDER_MALE ? "M" : "F");
                }
                if (birthdate.isPresent()) {
                    json.put(Server.PATIENT_BIRTHDATE_KEY, getDateTimeString(birthdate.get()));
                }
                if (assignedLocationUuid.isPresent()) {
                    json.put(
                            Server.PATIENT_ASSIGNED_LOCATION,
                            getLocationObject(assignedLocationUuid.get()));
                }

                return true;
            } catch (JSONException e) {
                Log.w(TAG, "Unable to serialize a patient delta to JSON.", e);

                return false;
            }
        }

        /**
         * Returns the {@link ContentValues} corresponding to the delta.
         */
        public ContentValues toContentValues() {
            ContentValues contentValues = new ContentValues();

            if (id.isPresent()) {
                contentValues.put(
                        PatientProviderContract.PatientColumns._ID,
                        id.get());
            }
            if (givenName.isPresent()) {
                contentValues.put(
                        PatientProviderContract.PatientColumns.COLUMN_NAME_GIVEN_NAME,
                        givenName.get());
            }
            if (familyName.isPresent()) {
                contentValues.put(
                        PatientProviderContract.PatientColumns.COLUMN_NAME_FAMILY_NAME,
                        familyName.get());
            }
            if (gender.isPresent()) {
                contentValues.put(
                        PatientProviderContract.PatientColumns.COLUMN_NAME_GENDER,
                        gender.get() == Patient.GENDER_MALE ? "M" : "F");
            }
            if (birthdate.isPresent()) {
                Period period = new Period(birthdate.get(), DateTime.now());
                if (period.getYears() >= 2) {
                    contentValues.put(
                            PatientProviderContract.PatientColumns.COLUMN_NAME_AGE_YEARS,
                            period.getYears());
                } else {
                    contentValues.put(
                            PatientProviderContract.PatientColumns.COLUMN_NAME_AGE_MONTHS,
                            period.getYears() * 12 + period.getMonths());
                }
            }
            if (assignedLocationUuid.isPresent()) {
                contentValues.put(
                        PatientProviderContract.PatientColumns.COLUMN_NAME_LOCATION_UUID,
                        assignedLocationUuid.get());
            }

            return contentValues;
        }
    }

    private static JSONObject getLocationObject(String assignedLocationUuid) throws JSONException {
        JSONObject location = new JSONObject();
        location.put("uuid", assignedLocationUuid);
        return location;
    }

    private static String getDateTimeString(DateTime dateTime) {
        return BIRTHDATE_FORMATTER.print(dateTime);
    }

    public static Builder builder() {
    	return new Builder();
    }

    public static final class Builder {

    	private String id;
        private String uuid;
        private String givenName;
        private String familyName;
        private int gender;
        private Duration age;
        private DateTime admissionDateTime;
        private String locationUuid;

        private Builder() {}

        public Builder setId(String id) {
        	this.id = id;
        	return this;
        }
    	public Builder setUuid(String uuid) {
    		this.uuid = uuid;
    		return this;
    	}
    	public Builder setGivenName(String givenName) {
    		this.givenName = givenName;
    		return this;
    	}
    	public Builder setFamilyName(String familyName) {
    		this.familyName = familyName;
    		return this;
    	}
        public Builder setGender(int gender) {
            this.gender = gender;
            return this;
        }
    	public Builder setAge(Duration age) {
    		this.age = age;
    		return this;
    	}
    	public Builder setAdmissionDateTime(DateTime admissionDateTime) {
    		this.admissionDateTime = admissionDateTime;
    		return this;
    	}
    	public Builder setLocationUuid(String locationUuid) {
    		this.locationUuid = locationUuid;
    		return this;
    	}
    	public AppPatient build() {
    		return new AppPatient(this);
    	}
    }
}
