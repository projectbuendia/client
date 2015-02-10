package org.msf.records.data.app;

import android.content.ContentValues;

import com.google.common.base.Optional;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.msf.records.model.Concept;
import org.msf.records.net.Server;
import org.msf.records.net.model.Patient;
import org.msf.records.sync.providers.Contracts;
import org.msf.records.utils.Logger;

/**
 * An object that represents the data to write to a new patient or the data to update on a patient.
 */
public class AppPatientDelta {

    private static final Logger LOG = Logger.create();

    private static final DateTimeFormatter BIRTHDATE_FORMATTER =
            DateTimeFormat.forPattern("yyyy-MM-dd");

    public Optional<String> id = Optional.absent();
    public Optional<String> givenName = Optional.absent();
    public Optional<String> familyName = Optional.absent();
    public Optional<Integer> gender = Optional.absent();
    public Optional<DateTime> birthdate = Optional.absent();

    public Optional<DateTime> admissionDate = Optional.absent();
    public Optional<DateTime> firstSymptomDate = Optional.absent();
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
            if (firstSymptomDate.isPresent()) {
                JSONObject observation = new JSONObject();
                observation.put(Server.PATIENT_QUESTION_UUID, Concept.FIRST_SYMPTOM_DATE_UUID);
                observation.put(Server.PATIENT_ANSWER_DATE,
                        getDateTimeString(firstSymptomDate.get()));
                JSONArray observations = new JSONArray();
                observations.put(observation);
                json.put(Server.PATIENT_OBSERVATIONS_KEY, observations);
            }
            if (assignedLocationUuid.isPresent()) {
                json.put(
                        Server.PATIENT_ASSIGNED_LOCATION,
                        getLocationObject(assignedLocationUuid.get()));
            }

            return true;
        } catch (JSONException e) {
            LOG.w(e, "Unable to serialize a patient delta to JSON.");

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
                    Contracts.Patients._ID,
                    id.get());
        }
        if (givenName.isPresent()) {
            contentValues.put(
                    Contracts.Patients.GIVEN_NAME,
                    givenName.get());
        }
        if (familyName.isPresent()) {
            contentValues.put(
                    Contracts.Patients.FAMILY_NAME,
                    familyName.get());
        }
        if (gender.isPresent()) {
            contentValues.put(
                    Contracts.Patients.GENDER,
                    gender.get() == Patient.GENDER_MALE ? "M" : "F");
        }
        if (birthdate.isPresent()) {
            contentValues.put(
                    Contracts.Patients.BIRTHDATE,
                    birthdate.toString());
        }
        if (admissionDate.isPresent()) {
            contentValues.put(
                    Contracts.Patients.ADMISSION_TIMESTAMP,
                    getTimestamp(admissionDate.get()));
        }
        if (assignedLocationUuid.isPresent()) {
            contentValues.put(
                    Contracts.Patients.LOCATION_UUID,
                    assignedLocationUuid.get());
        }
        return contentValues;
    }

    private static JSONObject getLocationObject(String assignedLocationUuid) throws JSONException {
        JSONObject location = new JSONObject();
        location.put("encounterUuid", assignedLocationUuid);
        return location;
    }

    private static String getDateTimeString(DateTime dateTime) {
        return BIRTHDATE_FORMATTER.print(dateTime);
    }

    private static long getTimestamp(DateTime dateTime) {
        return dateTime.toInstant().getMillis() / 1000;
    }
}
