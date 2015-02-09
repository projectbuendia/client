package org.msf.records.net;

import android.support.annotation.Nullable;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.msf.records.data.app.AppPatientDelta;
import org.msf.records.net.model.Location;
import org.msf.records.net.model.NewUser;
import org.msf.records.net.model.Patient;
import org.msf.records.net.model.User;
import org.msf.records.utils.Logger;
import org.msf.records.utils.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of Server RPCs that will talk to OpenMRS.
 * Currently no other implementations.
 */
public class OpenMrsServer implements Server {

    private static final Logger LOG = Logger.create();

    private final OpenMrsConnectionDetails mConnectionDetails;
    private final RequestFactory mRequestFactory;
    private final Gson mGson;

    public OpenMrsServer(
            OpenMrsConnectionDetails connectionDetails,
            RequestFactory requestFactory,
            Gson gson) {
        mConnectionDetails = connectionDetails;
        mRequestFactory = requestFactory;
        // TODO(kpy): Inject a Gson instance here.
        mGson = gson;
    }

    /**
     * Wraps an ErrorListener so as to extract an error message from the JSON
     * content of a response, if possible.
     * @param errorListener An error listener.
     * @return A new error listener that tries to pass a more meaningful message
     * to the original errorListener.
     */
    private Response.ErrorListener wrapErrorListener(
            final Response.ErrorListener errorListener) {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                String message = error.getMessage();
                try {
                    if (error.networkResponse != null &&
                        error.networkResponse.data != null) {
                        String text = new String(error.networkResponse.data);
                        JsonObject result = new JsonParser().parse(text).getAsJsonObject();
                        if (result.has("error")) {
                            JsonObject errorObject = result.getAsJsonObject("error");
                            JsonElement element = errorObject.get("message");
                            if (element == null || element.isJsonNull()) {
                                element = errorObject.get("code");
                            }
                            if (element != null && element.isJsonPrimitive()) {
                                message = element.getAsString();
                            }
                        }
                    }
                } catch (JsonParseException
                        | IllegalStateException
                        | UnsupportedOperationException e) {
                    e.printStackTrace();
                }
                errorListener.onErrorResponse(new VolleyError(message, error));
            }
        };
    }

    @Override
    public void addPatient(
            AppPatientDelta patientDelta,
            final Response.Listener<Patient> patientListener,
            final Response.ErrorListener errorListener) {
        JSONObject json = new JSONObject();
        if (!patientDelta.toJson(json)) {
            throw new IllegalArgumentException("Unable to serialize the patient delta to JSON.");
        }

        LOG.v("Adding patient from JSON: %s", json.toString());

        OpenMrsJsonRequest request = mRequestFactory.newOpenMrsJsonRequest(
                mConnectionDetails,
                "/patient",
                json,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            patientListener.onResponse(patientFromJson(response));
                        } catch (JSONException e) {
                            LOG.e(e, "Failed to parse response");
                            errorListener.onErrorResponse(
                                    new VolleyError("Failed to parse response", e));
                        }
                    }
                },
                wrapErrorListener(errorListener));
        mConnectionDetails.getVolley().addToRequestQueue(request);
    }

    @Override
    public void updatePatient(
            String patientUuid,
            AppPatientDelta patientDelta,
            final Response.Listener<Patient> patientListener,
            final Response.ErrorListener errorListener) {
        JSONObject json = new JSONObject();
        if (!patientDelta.toJson(json)) {
            throw new IllegalArgumentException("Unable to serialize the patient delta to JSON.");
        }

        OpenMrsJsonRequest request = mRequestFactory.newOpenMrsJsonRequest(
                mConnectionDetails,
                "/patient/" + patientUuid,
                json,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            patientListener.onResponse(patientFromJson(response));
                        } catch (JSONException e) {
                            LOG.e(e, "Failed to parse response");
                            errorListener.onErrorResponse(
                                    new VolleyError("Failed to parse response", e));
                        }
                    }
                },
                wrapErrorListener(errorListener)
        );
        mConnectionDetails.getVolley().addToRequestQueue(request);
    }

    @Override
    public void addUser(
            final NewUser user,
            final Response.Listener<User> userListener,
            final Response.ErrorListener errorListener) {
        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("user_name", user.username);
            requestBody.put("given_name", user.givenName);
            requestBody.put("family_name", user.familyName);
            requestBody.put("password", user.password);

        } catch (JSONException e) {
            // This is almost never recoverable, and should not happen in correctly functioning code
            // So treat like NPE and rethrow.
            throw new RuntimeException(e);
        }

        OpenMrsJsonRequest request = mRequestFactory.newOpenMrsJsonRequest(
                mConnectionDetails,
                "/user",
                requestBody,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            userListener.onResponse(userFromJson(response));
                        } catch (JSONException e) {
                            LOG.e(e, "Failed to parse response");
                            errorListener.onErrorResponse(
                                    new VolleyError("Failed to parse response", e));
                        }
                    }
                },
                wrapErrorListener(errorListener)
        );
        request.setRetryPolicy(new DefaultRetryPolicy(100000, 1, 1f));
        mConnectionDetails.getVolley().addToRequestQueue(request);
    }

    @Override
    public void getPatient(String patientId,
                           final Response.Listener<Patient> patientListener,
                           final Response.ErrorListener errorListener) {
        OpenMrsJsonRequest request = mRequestFactory.newOpenMrsJsonRequest(
                mConnectionDetails,
                "/patient/" + patientId,
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            patientListener.onResponse(patientFromJson(response));
                        } catch (JSONException e) {
                            LOG.e(e, "Failed to parse response");
                            errorListener.onErrorResponse(
                                    new VolleyError("Failed to parse response", e));
                        }
                    }
                },
                wrapErrorListener(errorListener)
        );
        mConnectionDetails.getVolley().addToRequestQueue(request);
    }

    @Override
    public void updatePatientLocation(String patientId, String newLocationId) {
        // TODO(sdoerner): Implement.
    }

    @Override
    public void listPatients(@Nullable String filterState, @Nullable String filterLocation,
                             @Nullable String filterQueryTerm,
                             final Response.Listener<List<Patient>> patientListener,
                             Response.ErrorListener errorListener) {
        String query = filterQueryTerm != null ? filterQueryTerm : "";
        OpenMrsJsonRequest request = mRequestFactory.newOpenMrsJsonRequest(
                mConnectionDetails,
                "/patient?q=" + Utils.urlEncode(query),
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        ArrayList<Patient> patients = new ArrayList<>();
                        try {
                            JSONArray results = response.getJSONArray("results");
                            for (int i=0; i<results.length(); i++) {
                                patients.add(patientFromJson(results.getJSONObject(i)));
                            }
                        } catch (JSONException e) {
                            LOG.e("Failed to convert JSON response");
                        }
                        patientListener.onResponse(patients);
                    }
                },
                wrapErrorListener(errorListener)
        );
        mConnectionDetails.getVolley().addToRequestQueue(request);
    }

    private Patient patientFromJson(JSONObject object) throws JSONException {
        Patient patient = mGson.fromJson(object.toString(), Patient.class);

        if (!"M".equals(patient.gender) && !"F".equals(patient.gender)) {
            LOG.e("Invalid gender from server: " + patient.gender);
            patient.gender = "F";
        }
        return patient;
    }

    @Override
    public void listUsers(@Nullable String filterQueryTerm,
                          final Response.Listener<List<User>> userListener,
                          Response.ErrorListener errorListener) {
        String query = filterQueryTerm != null ? filterQueryTerm : "";
        OpenMrsJsonRequest request = mRequestFactory.newOpenMrsJsonRequest(
                mConnectionDetails,
                "/user?q=" + Utils.urlEncode(query),
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                        public void onResponse(JSONObject response) {
                        ArrayList<User> users = new ArrayList<>();
                        try {
                            JSONArray results = response.getJSONArray("results");
                            for (int i=0; i<results.length(); i++) {
                                users.add(userFromJson(results.getJSONObject(i)));
                            }
                        } catch (JSONException e) {
                            LOG.e(e, "Failed to parse response");
                        }
                        userListener.onResponse(users);
                    }
                },
                wrapErrorListener(errorListener)
        );
        mConnectionDetails.getVolley().addToRequestQueue(request);
    }

    private User userFromJson(JSONObject object) throws JSONException {
        return new User(object.getString("user_id"), object.getString("full_name"));
    }

    @Override
    public void addLocation(Location location,
                            final Response.Listener<Location> locationListener,
                            final Response.ErrorListener errorListener) {
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
            requestBody = new JSONObject(mGson.toJson(location));
        } catch (JSONException e) {
            // This is almost never recoverable, and should not happen in correctly functioning code
            // So treat like NPE and rethrow.
            throw new RuntimeException(e);
        }

        OpenMrsJsonRequest request = mRequestFactory.newOpenMrsJsonRequest(
                mConnectionDetails,
                "/location",
                requestBody,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        locationListener.onResponse(parseLocationJson(response));
                    }
                },
                errorListener);
        mConnectionDetails.getVolley().addToRequestQueue(request);
    }

    @Override
    public void updateLocation(Location location,
                               final Response.Listener<Location> locationListener,
                               final Response.ErrorListener errorListener) {

        if (location.uuid == null) {
            throw new IllegalArgumentException("Location must be set for update " + location);
        }
        if (location.names == null || location.names.isEmpty()) {
            throw new IllegalArgumentException("New names must be set for update " + location);
        }
        JSONObject requestBody;
        try {
            requestBody = new JSONObject(mGson.toJson(location));
        } catch (JSONException e) {
            String msg = "Failed to write patient changes to Gson: " + location.toString();
            LOG.e(e, msg);
            errorListener.onErrorResponse(new VolleyError(msg));
            return;
        }

        OpenMrsJsonRequest request = mRequestFactory.newOpenMrsJsonRequest(
                mConnectionDetails,
                "/location/" + location.uuid,
                requestBody,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        locationListener.onResponse(parseLocationJson(response));
                    }
                },
                wrapErrorListener(errorListener)
        );
        mConnectionDetails.getVolley().addToRequestQueue(request);
    }

    @Override
    public void deleteLocation(String locationUuid,
                               final Response.ErrorListener errorListener) {
        OpenMrsJsonRequest request = mRequestFactory.newOpenMrsJsonRequest(
                mConnectionDetails,
                Request.Method.DELETE, "/location/" + locationUuid,
                null,
                null,
                wrapErrorListener(errorListener)
        );
        mConnectionDetails.getVolley().addToRequestQueue(request);
    }

    @Override
    public void listLocations(final Response.Listener<List<Location>> locationListener,
                              Response.ErrorListener errorListener) {


        OpenMrsJsonRequest request = mRequestFactory.newOpenMrsJsonRequest(
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
                            LOG.e(e, "Failed to parse response");
                        }
                        locationListener.onResponse(result);
                    }
                },
                wrapErrorListener(errorListener)
        );
        mConnectionDetails.getVolley().addToRequestQueue(request);
    }

    private Location parseLocationJson(JSONObject object) {
        return mGson.fromJson(object.toString(), Location.class);
    }

    @Override
    public void cancelPendingRequests() {
        // TODO(dxchen): Implement or deprecate. The way this was implemented before, where a string
        // was the tag, is not safe. Only the class that initiated a request (and its delegates)
        // should be able to cancel that request.
    }
}
