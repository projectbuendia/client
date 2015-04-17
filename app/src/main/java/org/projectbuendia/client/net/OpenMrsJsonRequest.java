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

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/** Base class for authenticated OpenMRS JSON requests. */
public class OpenMrsJsonRequest extends JsonObjectRequest {

    private final String mUsername;
    private final String mPassword;

    private OpenMrsJsonRequest(String username, String password, String url, JSONObject jsonRequest,
                              Response.Listener<JSONObject> listener,
                              Response.ErrorListener errorListener) {
        super(url, jsonRequest, listener, errorListener);
        this.mUsername = username;
        this.mPassword = password;
    }

    /**
     * Constructs a GET request to OpenMRS.
     * @param connectionDetails an {@link OpenMrsConnectionDetails} for communicating with OpenMRS
     * @param urlSuffix the API URL being requested, relative to the API root
     * @param jsonRequest a {@link JSONObject} containing the request body
     * @param listener a {@link Response.Listener} that handles successful requests
     * @param errorListener a {@link Response.ErrorListener} that handles failed requests
     */
    public OpenMrsJsonRequest(OpenMrsConnectionDetails connectionDetails,
                              String urlSuffix,
                              JSONObject jsonRequest,
                              Response.Listener<JSONObject> listener,
                              Response.ErrorListener errorListener) {
        this(connectionDetails.getUser(), connectionDetails.getPassword(),
                connectionDetails.getBuendiaApiUrl() + urlSuffix,
                jsonRequest, listener, errorListener);
    }

    /**
     * Constructs a request to OpenMRS using an arbitrary HTTP method.
     * @param connectionDetails an {@link OpenMrsConnectionDetails} for communicating with OpenMRS
     * @param method the HTTP method
     * @param url the absolute URL being requested
     * @param jsonRequest a {@link JSONObject} containing the request body
     * @param listener a {@link Response.Listener} that handles successful requests
     * @param errorListener a {@link Response.ErrorListener} that handles failed requests
     */
    public OpenMrsJsonRequest(OpenMrsConnectionDetails connectionDetails,
                              int method, String url, JSONObject jsonRequest,
                              Response.Listener<JSONObject> listener,
                              Response.ErrorListener errorListener) {
        super(method, url, jsonRequest, listener, errorListener);
        this.mUsername = connectionDetails.getUser();
        this.mPassword = connectionDetails.getPassword();
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        // TODO: work out how to do Auth properly
        HashMap<String, String> params = new HashMap<>();
        OpenMrsConnectionDetails.addAuthHeader(mUsername, mPassword, params);
        params.put("Connection-Type", "application/json");
        return params;
    }
}
