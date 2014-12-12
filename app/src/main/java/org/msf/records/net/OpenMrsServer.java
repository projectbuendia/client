package org.msf.records.net;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.msf.records.location.LocationTree;
import org.msf.records.location.LocationTree.LocationSubtree;
import org.msf.records.model.Zone;
import org.msf.records.net.model.Location;
import org.msf.records.net.model.NewUser;
import org.msf.records.net.model.Patient;
import org.msf.records.net.model.PatientAge;
import org.msf.records.net.model.User;
import org.msf.records.utils.Utils;

import android.support.annotation.Nullable;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.gson.Gson;

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
            putIfSet(patientArguments, Server.PATIENT_AGE_TYPE_KEY, requestBody,
                    Server.PATIENT_AGE_TYPE_KEY);
            putIfSet(patientArguments, Server.PATIENT_DOB_YEARS_KEY, requestBody,
                    Server.PATIENT_DOB_YEARS_KEY);
            putIfSet(patientArguments, Server.PATIENT_DOB_MONTHS_KEY, requestBody,
                    Server.PATIENT_DOB_MONTHS_KEY);
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
        mConnectionDetails.getVolley().addToRequestQueue(request, logTag);
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
        mConnectionDetails.getVolley().addToRequestQueue(request, logTag);
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
        mConnectionDetails.getVolley().addToRequestQueue(request, logTag);
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
        mConnectionDetails.getVolley().addToRequestQueue(request, logTag);
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
        mConnectionDetails.getVolley().addToRequestQueue(request, logTag);
    }

    private Patient parsePatientJson(JSONObject object) throws JSONException {
        Patient patient = gson.fromJson(object.toString(),
                Patient.class);

        // TODO(rjlothian): This shouldn't be done here.
        if (patient.assigned_location == null && LocationTree.SINGLETON_INSTANCE != null) {
            LocationSubtree subtree =
                    LocationTree.SINGLETON_INSTANCE.getLocationByUuid(Zone.TRIAGE_ZONE_UUID);
            if (subtree != null) {
                patient.assigned_location = subtree.getLocation();
            }
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
        mConnectionDetails.getVolley().addToRequestQueue(request, logTag);
    }

    private User parseUserJson(JSONObject object) throws JSONException {
        return User.create(object.getString("user_id"), object.getString("full_name"));
    }

    public void addLocation(Location location,
                            final Response.Listener<Location> locationListener,
                            final Response.ErrorListener errorListener,
                            final String logTag) {
        JSONObject requestBody;
        try {
            if (location.uuid != null) {
                throw new IllegalArgumentException("The server sets the uuids for new locations");
            }
            if (location.parent_uuid == null) {
                throw new IllegalArgumentException("You must set a parent_uuid for a new location");
            }
            if (location.names == null || location.names.isEmpty()) {
                throw new IllegalArgumentException(
                        "You must set a name in at least one locale for a new location");
            }
            requestBody = new JSONObject(gson.toJson(location));
        } catch (JSONException e) {
            // This is almost never recoverable, and should not happen in correctly functioning code
            // So treat like NPE and rethrow.
            throw new RuntimeException(e);
        }

        OpenMrsJsonRequest request = new OpenMrsJsonRequest(mConnectionDetails, "/location",
                requestBody,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        locationListener.onResponse(parseLocationJson(response));
                    }
                },
                errorListener);
        mConnectionDetails.getVolley().addToRequestQueue(request, logTag);
    }

    @Override
    public void updateLocation(Location location,
                               final Response.Listener<Location> locationListener,
                               final Response.ErrorListener errorListener,
                               final String logTag) {

        if (location.uuid == null) {
            throw new IllegalArgumentException("Location must be set for update " + location);
        }
        if (location.names == null || location.names.isEmpty()) {
            throw new IllegalArgumentException("New names must be set for update " + location);
        }
        JSONObject requestBody;
        try {
            requestBody = new JSONObject(gson.toJson(location));
        } catch (JSONException e) {
            String msg = "Failed to write patient changes to Gson: " + location.toString();
            Log.e(logTag, msg);
            errorListener.onErrorResponse(new VolleyError(msg));
            return;
        }

        OpenMrsJsonRequest request = new OpenMrsJsonRequest(mConnectionDetails,
                "/location/"+location.uuid,
                requestBody,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        locationListener.onResponse(parseLocationJson(response));
                    }
                },
                errorListener);
        mConnectionDetails.getVolley().addToRequestQueue(request, logTag);
    }

    public void deleteLocation(String locationUuid,
                               final Response.ErrorListener errorListener,
                               final String logTag) {
        OpenMrsJsonRequest request = new OpenMrsJsonRequest(mConnectionDetails,
                Request.Method.DELETE, "/location/" + locationUuid,
                null,
                null,
                errorListener);
        mConnectionDetails.getVolley().addToRequestQueue(request, logTag);
    }

    @Override
    public void listLocations(final Response.Listener<List<Location>> locationListener,
                              Response.ErrorListener errorListener,
                              final String logTag) {


        OpenMrsJsonRequest request = new OpenMrsJsonRequest(
                mConnectionDetails, "/location",
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        ArrayList<Location> result = new ArrayList<>();
                        try {
                            JSONArray results = response.getJSONArray("results");
                            for (int i=0; i<results.length(); i++) {
                                Location location =
                                        parseLocationJson(results.getJSONObject(i));
                                result.add(location);
                            }
                        } catch (JSONException e) {
                            Log.e(logTag, "Failed to parse response", e);
                        }
                        locationListener.onResponse(result);
                    }
                },
                errorListener);
        mConnectionDetails.getVolley().addToRequestQueue(request, logTag);
    }

    private Location parseLocationJson(JSONObject object) {
        return gson.fromJson(object.toString(), Location.class);
    }

    @Override
    public void cancelPendingRequests(String logTag) {
        mConnectionDetails.getVolley().cancelPendingRequests(logTag);
    }
}
