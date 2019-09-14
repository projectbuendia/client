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

import com.android.volley.Response;

import org.projectbuendia.client.json.JsonEncounter;
import org.projectbuendia.client.json.JsonForm;
import org.projectbuendia.client.json.JsonLocation;
import org.projectbuendia.client.json.JsonNewUser;
import org.projectbuendia.client.json.JsonOrder;
import org.projectbuendia.client.json.JsonPatient;
import org.projectbuendia.client.json.JsonUser;
import org.projectbuendia.client.models.Encounter;
import org.projectbuendia.client.models.Order;

import java.util.List;

/** An interface abstracting the idea of an RPC to a server. */
public interface Server {
    /**
     * Logs an event by sending a dummy request to the server.  (The server logs
     * can then be scanned later to produce analytics for the client app.)
     * @param pairs An even number of arguments providing key-value pairs of
     *              arbitrary data to record with the event.
     */
    void logToServer(List<String> pairs);

    /** Adds a patient. */
    void addPatient(
        JsonPatient patient,
        Response.Listener<JsonPatient> successListener,
        Response.ErrorListener errorListener);

    /** Updates a patient. */
    public void updatePatient(
        JsonPatient patient,
        Response.Listener<JsonPatient> successListener,
        Response.ErrorListener errorListener);

    /**
     * Creates a new user.
     * @param user the JsonNewUser to add
     */
    public void addUser(
        JsonNewUser user,
        Response.Listener<JsonUser> successListener,
        Response.ErrorListener errorListener);

    /** Creates a new encounter. */
    void addEncounter(
        Encounter encounter,
        Response.Listener<JsonEncounter> successListener,
        Response.ErrorListener errorListener);

    /** Deletes (voids) an observation by its UUID. */
    void deleteObservation(
        String uuid,
        Response.Listener<Void> successListener,
        Response.ErrorListener errorListener);

    /**
     * Gets the patient record for an existing patient. Currently we are just using a String-String
     * map for parameters, but this is a bit close in implementation details to the old Buendia UI
     * so it will probably need to be generalized in future.
     * @param patientId the unique patient id representing the patients
     */
    public void getPatient(
        String patientId,
        Response.Listener<JsonPatient> successListener,
        Response.ErrorListener errorListener);

    /**
     * Updates the location of a patient.
     * @param patientId     the id of the patient to update
     * @param newLocationId the id of the new location that the patient is assigned to
     */
    public void updatePatientLocation(String patientId, String newLocationId);

    /** Lists all existing users. */
    public void listUsers(@Nullable String filterQueryTerm,
                          Response.Listener<List<JsonUser>> successListener,
                          Response.ErrorListener errorListener);

    /** Lists all published forms. */
    void listForms(Response.Listener<List<JsonForm>> successListener,
                   Response.ErrorListener errorListener);

    /**
     * Adds a new location to the server.
     * @param location        uuid must not be set, parent_uuid must be set, and the names map must have a
     *                        name for at least one locale.
     * @param successListener the listener to be informed of the newly added location
     * @param errorListener   listener to be informed of any errors
     */
    public void addLocation(JsonLocation location,
                            final Response.Listener<JsonLocation> successListener,
                            final Response.ErrorListener errorListener);

    /**
     * Updates the names for a location on the server.
     * @param location        the location, only uuid and new locale names for the location will be used,
     *                        but ideally the other arguments should be correct
     * @param successListener the listener to be informed of the newly added location
     * @param errorListener   listener to be informed of any errors
     */
    public void updateLocation(JsonLocation location,
                               final Response.Listener<JsonLocation> successListener,
                               final Response.ErrorListener errorListener);

    /**
     * Deletes a given location from the server. The location should not be the EMC location or
     * one of the zones - just a client added location, tent or bed.
     */
    public void deleteLocation(String locationUuid,
                               final Response.ErrorListener errorListener);

    /** Lists all locations. */
    public void listLocations(Response.Listener<List<JsonLocation>> successListener,
                              Response.ErrorListener errorListener);

    /** Adds or updates an order. */
    void saveOrder(Order order,
                   Response.Listener<JsonOrder> successListener,
                   Response.ErrorListener errorListener);

    /** Deletes an order. */
    void deleteOrder(String orderUuid,
                     Response.Listener<Void> successListener,
                     Response.ErrorListener errorListener);

    /** Cancels all pending requests. */
    public void cancelPendingRequests();
}
