package org.msf.records.net;

import android.support.annotation.Nullable;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.msf.records.model.NewUser;
import org.msf.records.model.Patient;
import org.msf.records.model.PatientAge;
import org.msf.records.model.PatientLocation;
import org.msf.records.model.User;
import org.msf.records.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implementation of Server RPCs that will talk to OpenMRS.
 * Currently no other implementations.
 */
public class OpenMrsServer implements Server {

    private static final String TAG = "OpenMrsServer";
    private final Gson gson = new Gson();
    private final OpenMrsConnectionDetails mConnectionDetails;

    public OpenMrsServer(OpenMrsConnectionDetails connectionDetails) {
        this.mConnectionDetails = connectionDetails;
    }

    @Override
    public void addPatient(final Map<String, String> patientArguments,
                           final Response.Listener<Patient> patientListener,
                           final Response.ErrorListener errorListener,
                           final String logTag) {

        JSONObject requestBody = new JSONObject();
        try {
            putIfSet(patientArguments, Server.PATIENT_ID_KEY, requestBody,
                    Server.PATIENT_ID_KEY);
            putIfSet(patientArguments, Server.PATIENT_GIVEN_NAME_KEY, requestBody,
                    Server.PATIENT_GIVEN_NAME_KEY);
            putIfSet(patientArguments, Server.PATIENT_FAMILY_NAME_KEY, requestBody,
                    Server.PATIENT_FAMILY_NAME_KEY);
            putIfSet(patientArguments, Server.PATIENT_GENDER_KEY, requestBody,
                    Server.PATIENT_GENDER_KEY);
        } catch (JSONException e) {
            // This is almost never recoverable, and should not happen in correctly functioning code
            // So treat like NPE and rethrow.
            throw new RuntimeException(e);
        }

        OpenMrsJsonRequest request = new OpenMrsJsonRequest(mConnectionDetails, "/patient",
                requestBody,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            patientListener.onResponse(parsePatientJson(response));
                        } catch (JSONException e) {
                            Log.e(logTag, "Failed to parse response", e);
                            errorListener.onErrorResponse(
                                    new VolleyError("Failed to parse response", e));
                        }
                    }
                },
                errorListener);
        mConnectionDetails.volley.addToRequestQueue(request, logTag);
    }

    @Override
    public void addUser(
            final NewUser user,
            final Response.Listener<User> userListener,
            final Response.ErrorListener errorListener,
            final String logTag) {
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("user_name", user.getUsername());
            requestBody.put("given_name", user.getGivenName());
            requestBody.put("family_name", user.getFamilyName());
            requestBody.put("password", user.getPassword());

        } catch (JSONException e) {
            // This is almost never recoverable, and should not happen in correctly functioning code
            // So treat like NPE and rethrow.
            throw new RuntimeException(e);
        }

        // TODO(akalachman): Remove.
        Log.e(TAG, requestBody.toString());

        OpenMrsJsonRequest request = new OpenMrsJsonRequest(mConnectionDetails, "/user",
                requestBody,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            userListener.onResponse(parseUserJson(response));
                        } catch (JSONException e) {
                            Log.e(logTag, "Failed to parse response", e);
                            errorListener.onErrorResponse(
                                    new VolleyError("Failed to parse response", e));
                        }
                    }
                },
                errorListener);
        mConnectionDetails.volley.addToRequestQueue(request, logTag);
    }

    private void putIfSet(Map<String, String> patientArguments, String key, JSONObject name, String param) throws JSONException {
        String value = patientArguments.get(key);
        if (value != null) {
            name.put(param, value);
        }
    }

    @Override
    public void getPatient(String patientId,
                           final Response.Listener<Patient> patientListener,
                           final Response.ErrorListener errorListener,
                           final String logTag) {
        OpenMrsJsonRequest request = new OpenMrsJsonRequest(mConnectionDetails,
                "/patient/" + patientId,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {

                        try {
                            patientListener.onResponse(parsePatientJson(response));
                        } catch (JSONException e) {
                            Log.e(logTag, "Failed to parse response", e);
                            errorListener.onErrorResponse(
                                    new VolleyError("Failed to parse response", e));
                        }
                    }
                },
                errorListener);
        mConnectionDetails.volley.addToRequestQueue(request, logTag);
    }

    @Override
    public void updatePatient(String patientUuid, Patient patientChanges,
                              final Response.Listener<Patient> patientListener,
                              final Response.ErrorListener errorListener,
                              final String logTag) {
        JSONObject requestBody;
        try {
            requestBody = new JSONObject(gson.toJson(patientChanges));
        } catch (JSONException e) {
            String msg = "Failed to write patient changes to Gson: " + patientChanges;
            Log.e(logTag, msg);
            errorListener.onErrorResponse(new VolleyError(msg));
            return;
        }

        OpenMrsJsonRequest request = new OpenMrsJsonRequest(mConnectionDetails,
                "/patient/"+patientUuid,
                requestBody,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            patientListener.onResponse(parsePatientJson(response));
                        } catch (JSONException e) {
                            Log.e(logTag, "Failed to parse response", e);
                            errorListener.onErrorResponse(
                                    new VolleyError("Failed to parse response", e));
                        }
                    }
                },
                errorListener);
        mConnectionDetails.volley.addToRequestQueue(request, logTag);
    }

    @Override
    public void listPatients(@Nullable String filterState, @Nullable String filterLocation,
                             @Nullable String filterQueryTerm,
                             final Response.Listener<List<Patient>> patientListener,
                             Response.ErrorListener errorListener, final String logTag) {
        String query = filterQueryTerm != null ? filterQueryTerm : "";
        OpenMrsJsonRequest request = new OpenMrsJsonRequest(mConnectionDetails,
                "/patient?q=" + Utils.urlEncode(query),
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        ArrayList<Patient> result = new ArrayList<>();
                        try {
                            JSONArray results = response.getJSONArray("results");
                            for (int i=0; i<results.length(); i++) {
                                Patient patient = parsePatientJson(results.getJSONObject(i));
                                result.add(patient);
                            }
                        } catch (JSONException e) {
                            Log.e(logTag, "Failed to parse response", e);
                        }
                        patientListener.onResponse(result);
                    }
                },
                errorListener);
        mConnectionDetails.volley.addToRequestQueue(request, logTag);
    }

    private Patient parsePatientJson(JSONObject object) throws JSONException {
        Patient patient = gson.fromJson(object.toString(),
                Patient.class);

        // TODO(akalachman): After the demo, replace with obvious sentinels to avoid confusion.
        if (patient.assigned_location == null) {
            patient.assigned_location = new PatientLocation();
            patient.assigned_location.zone = "Suspect Zone";
            patient.assigned_location.bed = "Bed 5";
            patient.assigned_location.tent = "Tent 4";
        }

        if (patient.age == null) {
            // TODO(akalachman): After the demo, replace with obvious sentinel to avoid confusion.
            patient.age = new PatientAge();
            patient.age.type = "years";
            patient.age.years = 24;
        }

        patient.first_showed_symptoms_timestamp = 0L;
        if (patient.created_timestamp != null) {
            patient.created_timestamp /= 1000; // UI wants it in seconds, not millis
        }

        if (patient.gender == null) {
            // If not sent by server (should never happen)
            Log.e(TAG, "gender was not sent from server");
            patient.gender = "F";
        }
        return patient;
    }

    private User parseUserJson(JSONObject object) throws JSONException {
        return User.create(object.getString("user_id"), object.getString("full_name"));
    }

    @Override
    public void listUsers(@Nullable String filterQueryTerm,
                          final Response.Listener<List<User>> userListener,
                          Response.ErrorListener errorListener,
                          final String logTag) {
        String query = filterQueryTerm != null ? filterQueryTerm : "";
        OpenMrsJsonRequest request = new OpenMrsJsonRequest(
                mConnectionDetails, "/user?q=" + Utils.urlEncode(query),
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        ArrayList<User> result = new ArrayList<>();
                        try {
                            JSONArray results = response.getJSONArray("results");
                            for (int i=0; i<results.length(); i++) {
                                User user = parseUserJson(results.getJSONObject(i));
                                result.add(user);
                            }
                        } catch (JSONException e) {
                            Log.e(logTag, "Failed to parse response", e);
                        }
                        userListener.onResponse(result);
                    }
                },
                errorListener);
        mConnectionDetails.volley.addToRequestQueue(request, logTag);
    }

    @Override
    public void cancelPendingRequests(String logTag) {
        mConnectionDetails.volley.cancelPendingRequests(logTag);
    }
}
