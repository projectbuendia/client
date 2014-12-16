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
import org.msf.records.net.Server;
import org.msf.records.net.model.Patient;
import org.msf.records.sync.PatientProjection;
import org.msf.records.sync.PatientProviderContract;

/**
 * An object that represents the data to write to a new patient or the data to update on a patient.
 */
public class AppPatientDelta {

    private static final String TAG = AppPatientDelta.class.getSimpleName();

    private static final DateTimeFormatter BIRTHDATE_FORMATTER =
            DateTimeFormat.forPattern("yyyy-MM-dd");

    public Optional<String> id = Optional.absent();
    public Optional<String> givenName = Optional.absent();
    public Optional<String> familyName = Optional.absent();
    public Optional<Integer> gender = Optional.absent();
    public Optional<DateTime> birthdate = Optional.absent();

    public Optional<DateTime> admissionDate = Optional.absent();
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
                        Server.PATIENT_GENDER_KEY, gender.get() == Patient.GENDER_MALE ? "M" : "F");
            }
            if (birthdate.isPresent()) {
                json.put(Server.PATIENT_BIRTHDATE_KEY, getDateTimeString(birthdate.get()));
            }
            if (admissionDate.isPresent()) {
                json.put(Server.PATIENT_ADMISSION_TIMESTAMP, getTimestamp(admissionDate.get()));
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
                        period.getYears()
                );
            } else {
                contentValues.put(
                        PatientProviderContract.PatientColumns.COLUMN_NAME_AGE_MONTHS,
                        period.getYears() * 12 + period.getMonths());
            }
        }
        if (admissionDate.isPresent()) {
            contentValues.put(
                    PatientProviderContract.PatientColumns.COLUMN_NAME_ADMISSION_TIMESTAMP,
                    getTimestamp(admissionDate.get()));
        }
        if (assignedLocationUuid.isPresent()) {
            contentValues.put(
                    PatientProviderContract.PatientColumns.COLUMN_NAME_LOCATION_UUID,
                    assignedLocationUuid.get());
        }

        return contentValues;
    }

    private JSONObject getLocationObject(String assignedLocationUuid) throws JSONException {
        JSONObject location = new JSONObject();
        location.put("uuid", assignedLocationUuid);
        return location;
    }

    private static String getDateTimeString(DateTime dateTime) {
        return BIRTHDATE_FORMATTER.print(dateTime);
    }

    private static long getTimestamp(DateTime dateTime) {
        return dateTime.toInstant().getMillis() / 1000;
    }
}
