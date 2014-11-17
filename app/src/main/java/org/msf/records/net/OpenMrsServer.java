package org.msf.records.net;

import android.content.Context;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.msf.records.model.Patient;
import org.msf.records.model.PatientAge;
import org.msf.records.model.PatientLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implementation of Server RPCs that will talk
 * Created by nfortescue on 11/3/14.
 */
public class OpenMrsServer implements Server {
    private static final String USERNAME = "buendiatest1";
    private static final String PASSWORD = "Buendia123";
    private static final String DEFAULT_ROOT_URL = "http://104.155.15.141:8080/openmrs/ws/rest/v1/";

    private final Gson gson = new Gson();
    private final String ROOT_URL;
    private final VolleySingleton mVolley;

    public OpenMrsServer(Context context, @Nullable String rootUrl) {
        this.mVolley = VolleySingleton.getInstance(context.getApplicationContext());
        ROOT_URL = TextUtils.isEmpty(rootUrl) ? DEFAULT_ROOT_URL : rootUrl;
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


        OpenMrsJsonRequest request = new OpenMrsJsonRequest(
                Constants.LOCAL_ADMIN_USERNAME, Constants.LOCAL_ADMIN_PASSWORD,
                "http://" + Constants.LOCALHOST_EMULATOR + ":8080" + Constants.API_BASE +
                        "/patient",
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
        mVolley.addToRequestQueue(request, logTag);
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
        OpenMrsJsonRequest request = new OpenMrsJsonRequest(
                Constants.LOCAL_ADMIN_USERNAME, Constants.LOCAL_ADMIN_PASSWORD,
                "http://" + Constants.LOCALHOST_EMULATOR + ":8080" + Constants.API_BASE +
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
        mVolley.addToRequestQueue(request, logTag);
    }

    @Override
    public void updatePatient(String patientId, Map<String, String> patientArguments,
                              Response.Listener<Patient> patientListener,
                              Response.ErrorListener errorListener, String logTag) {
        errorListener.onErrorResponse(new VolleyError("Not yet implemented"));
    }

    @Override
    public void listPatients(@Nullable String filterState, @Nullable String filterLocation,
                             @Nullable String filterQueryTerm,
                             final Response.Listener<List<Patient>> patientListener,
                             Response.ErrorListener errorListener, final String logTag) {
        OpenMrsJsonRequest request = new OpenMrsJsonRequest(
                Constants.LOCAL_ADMIN_USERNAME, Constants.LOCAL_ADMIN_PASSWORD,
                "http://" + Constants.LOCALHOST_EMULATOR + ":8080" + Constants.API_BASE +
                        "/patient",
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
        mVolley.addToRequestQueue(request, logTag);
    }

    private Patient parsePatientJson(JSONObject object) throws JSONException {
        Patient patient = gson.fromJson(object.toString(),
                Patient.class);

        // TODO(nfortescue): fill these in properly
        patient.assigned_location = new PatientLocation();
        patient.assigned_location.zone = 1;
        patient.assigned_location.bed = 2;
        patient.assigned_location.tent = 3;

        patient.age = new PatientAge();
        patient.age.type = "years";
        patient.age.years = 24;

        patient.first_showed_symptoms_timestamp_utc = 0L;
        if (patient.created_timestamp_utc != null) {
            patient.created_timestamp_utc /= 1000; // UI wants it in seconds, not millis
        }
        return patient;
    }

    @Override
    public void cancelPendingRequests(String logTag) {
        mVolley.cancelPendingRequests(logTag);
    }
}
