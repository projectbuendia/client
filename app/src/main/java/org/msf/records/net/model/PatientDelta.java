package org.msf.records.net.model;

import android.util.Log;

import com.google.common.base.Optional;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONException;
import org.json.JSONObject;
import org.msf.records.net.Server;

/**
 * An object that represents the data to write to a new patient or the data to update on a patient.
 */
public class PatientDelta {

    private static final String TAG = PatientDelta.class.getSimpleName();

    private static final DateTimeFormatter BIRTHDATE_FORMATTER =
            DateTimeFormat.forPattern("yyyy-MM-dd");

    public Optional<String> id;
    public Optional<String> givenName;
    public Optional<String> familyName;
    public Optional<Integer> gender;
    public Optional<DateTime> birthdate;

    public Optional<String> assignedLocationUuid;

    /**
     * Serializes the fields changed in the delta to a {@link JSONObject}.
     *
     * @return whether serialization succeeded
     */
    public boolean serializeToJson(JSONObject json) {
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

    private JSONObject getLocationObject(String assignedLocationUuid) throws JSONException {
        JSONObject location = new JSONObject();
        location.put("uuid", assignedLocationUuid);
        return location;
    }

    private static String getDateTimeString(DateTime dateTime) {
        return BIRTHDATE_FORMATTER.print(dateTime);
    }
}
