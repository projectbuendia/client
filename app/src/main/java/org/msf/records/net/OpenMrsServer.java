package org.msf.records.net;

import android.content.Context;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.msf.records.model.Patient;

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
    private final String ROOT_URL;
    private final VolleySingleton mVolley;

    public OpenMrsServer(Context context, @Nullable String rootUrl) {
        this.mVolley = VolleySingleton.getInstance(context.getApplicationContext());
        ROOT_URL = TextUtils.isEmpty(rootUrl) ? DEFAULT_ROOT_URL : rootUrl;
    }

    @Override
    public void addPatient(final Map<String, String> patientArguments,
                           final Response.Listener<Patient> patientListener,
                           Response.ErrorListener errorListener, String logTag) {

        // Temporary implementation. Ideally we'd want this logic at the server side, but we are
        // doing it client side for a proof of concept.
        JsonObjectRequest request;
        JSONObject requestBody = new JSONObject();
        JSONObject name = new JSONObject();
        try {
            putIfSet(patientArguments, Server.PATIENT_GIVEN_NAME_KEY, name, "givenName");
            putIfSet(patientArguments, Server.PATIENT_FAMILY_NAME_KEY, name, "familyName");
            JSONArray namesArray = new JSONArray();
            namesArray.put(0, name);
            putIfSet(patientArguments, Server.PATIENT_GENDER_KEY, requestBody, "gender");
            requestBody.put("names", namesArray);
        } catch (JSONException e) {
            // This is almost never recoverable, and should not happen in correctly functioning code
            // So treat like NPE and rethrow.
            throw new RuntimeException(e);
        }
        request = new OpenMrsJsonRequest(
                USERNAME, PASSWORD,
                ROOT_URL + "person",
                requestBody,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i("org.msf.records", response.toString());
                        // The response from OpenMSR is rubbish, and requires you to follow a link
                        // to get a name. So we will assume it just worked, and use what was sent.
                        Patient patient = new Patient();
                        patient.id = response.optString("uuid");
                        if (patientArguments.containsKey(Server.PATIENT_GIVEN_NAME_KEY)) {
                            patient.given_name = patientArguments.get(Server.PATIENT_GIVEN_NAME_KEY);
                        }
                        if (patientArguments.containsKey(Server.PATIENT_FAMILY_NAME_KEY)) {
                            patient.family_name = patientArguments.get(Server.PATIENT_FAMILY_NAME_KEY);
                        }
                        if (patientArguments.containsKey(Server.PATIENT_GENDER_KEY)) {
                            patient.gender = patientArguments.get(Server.PATIENT_GENDER_KEY);
                        }
                        // TODO(nfortescue): work out if utc is best for timestamps, and re-specify
                        patient.created_timestamp_utc = System.currentTimeMillis() / 1000;
                        patientListener.onResponse(patient);
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
    public void getPatient(String patientId, Response.Listener<Patient> patientListener,
                           Response.ErrorListener errorListener, String logTag) {
        errorListener.onErrorResponse(new VolleyError("Not yet implemented"));
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
                             Response.Listener<List<Patient>> patientListener,
                             Response.ErrorListener errorListener, String logTag) {
        // TODO(nfortescue): actually get patients
        patientListener.onResponse(new ArrayList<Patient>());
    }

    @Override
    public void cancelPendingRequests(String logTag) {
        mVolley.cancelPendingRequests(logTag);
    }
}
