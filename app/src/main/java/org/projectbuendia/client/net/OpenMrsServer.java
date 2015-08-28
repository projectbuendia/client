// Copyright 2015 The Project Buendia Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at: http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distrib-
// uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
// specific language governing permissions and limitations under the License.

package org.projectbuendia.client.net;

import android.support.annotation.Nullable;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.common.base.Joiner;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.projectbuendia.client.App;
import org.projectbuendia.client.data.app.AppEncounter;
import org.projectbuendia.client.data.app.AppOrder;
import org.projectbuendia.client.data.app.AppPatient;
import org.projectbuendia.client.data.app.AppPatientDelta;
import org.projectbuendia.client.model.Concepts;
import org.projectbuendia.client.net.model.Encounter;
import org.projectbuendia.client.net.model.Form;
import org.projectbuendia.client.net.model.Location;
import org.projectbuendia.client.net.model.NewUser;
import org.projectbuendia.client.net.model.Order;
import org.projectbuendia.client.net.model.Patient;
import org.projectbuendia.client.net.model.User;
import org.projectbuendia.client.utils.Logger;
import org.projectbuendia.client.utils.Utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** Implementation of {@link Server} that sends RPC's to OpenMRS. */
public class OpenMrsServer implements Server {
    private static final Logger LOG = Logger.create();

    private final OpenMrsConnectionDetails mConnectionDetails;
    private final RequestFactory mRequestFactory;
    private final Gson mGson;

    /**
     * Constructs an interface to the OpenMRS server.
     * @param connectionDetails an {@link OpenMrsConnectionDetails} instance for communicating with
     *                          the server
     * @param requestFactory a {@link RequestFactory} for generating requests to OpenMRS
     * @param gson a {@link Gson} instance for serialization/deserialization
     */
    public OpenMrsServer(
            OpenMrsConnectionDetails connectionDetails,
            RequestFactory requestFactory,
            Gson gson) {
        mConnectionDetails = connectionDetails;
        mRequestFactory = requestFactory;
        // TODO: Inject a Gson instance here.
        mGson = gson;
    }

    /**
     * Wraps an ErrorListener so as to extract an error message from the JSON
     * content of a response, if possible.
     * @param errorListener An error listener.
     * @return A new error listener that tries to pass a more meaningful message
     *     to the original errorListener.
     */
    private Response.ErrorListener wrapErrorListener(
            final Response.ErrorListener errorListener) {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                String message = error.getMessage();
                try {
                    if (error.networkResponse != null
                            && error.networkResponse.data != null) {
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
    public void logToServer(List<String> pairs) {
        // To avoid filling the server logs with big messy stack traces, let's make a dummy
        // request that succeeds.  We assume "Pulse" will always be present on the server.
        // Conveniently, extra data after ";" in the URL is included in request logs, but
        // ignored by the REST resource handler, which just returns the "Pulse" concept.
        final String urlPath = "/concept/" + Concepts.PULSE_UUID;
        List<String> params = new ArrayList<>();
        params.add("time=" + (new Date().getTime()));
        User user = App.getUserManager().getActiveUser();
        if (user != null) {
            params.add("user_id=" + user.id);
            if (user.isGuestUser()) {
                params.add("guest_user=1");
            }
        }
        for (int i = 0; i + 1 < pairs.size(); i += 2) {
            params.add(Utils.urlEncode(pairs.get(i)) + "=" + Utils.urlEncode(pairs.get(i + 1)));
        }

        LOG.i("Logging to server: %s", params);
        OpenMrsJsonRequest request = mRequestFactory.newOpenMrsJsonRequest(
                mConnectionDetails, urlPath + ";" + Joiner.on(";").join(params), null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) { }
                } , null);
        request.setRetryPolicy(new DefaultRetryPolicy(Common.REQUEST_TIMEOUT_MS_SHORT, 0, 1));
        mConnectionDetails.getVolley().addToRequestQueue(request);
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
        request.setRetryPolicy(new DefaultRetryPolicy(Common.REQUEST_TIMEOUT_MS_SHORT, 1, 1f));
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
        request.setRetryPolicy(new DefaultRetryPolicy(Common.REQUEST_TIMEOUT_MS_SHORT, 1, 1f));
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
        request.setRetryPolicy(new DefaultRetryPolicy(Common.REQUEST_TIMEOUT_MS_SHORT, 1, 1f));
        mConnectionDetails.getVolley().addToRequestQueue(request);
    }

    @Override
    public void addEncounter(AppPatient patient,
                             AppEncounter encounter,
                             final Response.Listener<Encounter> encounterListener,
                             final Response.ErrorListener errorListener) {
        JSONObject json;
        try {
            json = encounter.toJson();
        } catch (JSONException e) {
            throw new IllegalArgumentException("Unable to serialize the encounter to JSON.", e);
        }

        OpenMrsJsonRequest request = mRequestFactory.newOpenMrsJsonRequest(
                mConnectionDetails,
                "/patientencounters",
                json,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            encounterListener.onResponse(encounterFromJson(response));
                        } catch (JSONException e) {
                            LOG.e(e, "Failed to parse response");
                            errorListener.onErrorResponse(
                                    new VolleyError("Failed to parse response", e));
                        }
                    }
                },
                wrapErrorListener(errorListener));
        request.setRetryPolicy(new DefaultRetryPolicy(Common.REQUEST_TIMEOUT_MS_SHORT, 1, 1f));
        mConnectionDetails.getVolley().addToRequestQueue(request);
    }

    @Override
    public void addOrder(AppOrder order,
                         final Response.Listener<Order> successListener,
                         final Response.ErrorListener errorListener) {
        JSONObject json;
        try {
            json = order.toJson();
        } catch (JSONException e) {
            throw new IllegalArgumentException("Unable to serialize the order to JSON.", e);
        }

        LOG.v("Adding order with JSON: %s", json);

        OpenMrsJsonRequest request = mRequestFactory.newOpenMrsJsonRequest(
                mConnectionDetails,
                "/order",
                json,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            successListener.onResponse(
                                    mGson.fromJson(response.toString(), Order.class));
                        } catch (JsonSyntaxException e) {
                            LOG.e(e, "Failed to parse response");
                            errorListener.onErrorResponse(
                                    new VolleyError("Failed to parse response", e));
                        }
                    }
                },
                wrapErrorListener(errorListener));
        request.setRetryPolicy(new DefaultRetryPolicy(Common.REQUEST_TIMEOUT_MS_SHORT, 1, 1f));
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
        // TODO: Implement or remove (currently handled by updatePatient).
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
                            for (int i = 0; i < results.length(); i++) {
                                patients.add(patientFromJson(results.getJSONObject(i)));
                            }
                        } catch (JSONException e) {
                            LOG.e(e, "Failed to convert JSON response");
                        }
                        patientListener.onResponse(patients);
                    }
                },
                wrapErrorListener(errorListener)
        );
        request.setRetryPolicy(
                new DefaultRetryPolicy(Common.REQUEST_TIMEOUT_MS_VERY_LONG, 1, 1f));
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

    private Encounter encounterFromJson(JSONObject object) throws JSONException {
        return mGson.fromJson(object.toString(), Encounter.class);
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
                            for (int i = 0; i < results.length(); i++) {
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
        request.setRetryPolicy(new DefaultRetryPolicy(Common.REQUEST_TIMEOUT_MS_MEDIUM, 1, 1f));
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
        request.setRetryPolicy(new DefaultRetryPolicy(Common.REQUEST_TIMEOUT_MS_SHORT, 1, 1f));
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
        request.setRetryPolicy(new DefaultRetryPolicy(Common.REQUEST_TIMEOUT_MS_SHORT, 1, 1f));
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
        request.setRetryPolicy(new DefaultRetryPolicy(Common.REQUEST_TIMEOUT_MS_SHORT, 1, 1f));
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
                            for (int i = 0; i < results.length(); i++) {
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
        request.setRetryPolicy(new DefaultRetryPolicy(Common.REQUEST_TIMEOUT_MS_MEDIUM, 1, 1f));
        mConnectionDetails.getVolley().addToRequestQueue(request);
    }

    private Location parseLocationJson(JSONObject object) {
        return mGson.fromJson(object.toString(), Location.class);
    }

    @Override
    public void listOrders(final Response.Listener<List<Order>> successListener,
                           Response.ErrorListener errorListener) {
        OpenMrsJsonRequest request = mRequestFactory.newOpenMrsJsonRequest(
                mConnectionDetails, "/order",
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        ArrayList<Order> orders = new ArrayList<>();
                        try {
                            JSONArray results = response.getJSONArray("results");
                            for (int i = 0; i < results.length(); i++) {
                                JSONObject result = results.getJSONObject(i);
                                orders.add(mGson.fromJson(result.toString(), Order.class));
                            }
                        } catch (JSONException e) {
                            LOG.e(e, "Failed to parse response");
                        }
                        successListener.onResponse(orders);
                    }
                },
                wrapErrorListener(errorListener)
        );
        request.setRetryPolicy(new DefaultRetryPolicy(Common.REQUEST_TIMEOUT_MS_MEDIUM, 1, 1f));
        mConnectionDetails.getVolley().addToRequestQueue(request);
    }

    @Override
    public void listForms(final Response.Listener<List<Form>> successListener,
                          Response.ErrorListener errorListener) {
        OpenMrsJsonRequest request = mRequestFactory.newOpenMrsJsonRequest(
                mConnectionDetails,
                "/xform",
                null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        ArrayList<Form> forms = new ArrayList<>();
                        try {
                            JSONArray results = response.getJSONArray("results");
                            for (int i = 0; i < results.length(); i++) {
                                JSONObject result = results.getJSONObject(i);
                                forms.add(mGson.fromJson(result.toString(), Form.class));
                            }
                        } catch (JSONException e) {
                            LOG.e(e, "Failed to parse response");
                        }
                        successListener.onResponse(forms);
                    }
                },
                wrapErrorListener(errorListener)
        );
        request.setRetryPolicy(new DefaultRetryPolicy(Common.REQUEST_TIMEOUT_MS_MEDIUM, 1, 1f));
        mConnectionDetails.getVolley().addToRequestQueue(request);
    }

    @Override
    public void cancelPendingRequests() {
        // TODO: Implement or deprecate. The way this was implemented before, where a string
        // was the tag, is not safe. Only the class that initiated a request (and its delegates)
        // should be able to cancel that request.
    }
}
