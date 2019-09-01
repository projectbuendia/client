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
import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;
import org.projectbuendia.client.BuildConfig;
import org.projectbuendia.client.utils.Logger;
import org.projectbuendia.client.utils.Utils;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import static org.projectbuendia.client.utils.Utils.eq;

/** Base class for authenticated OpenMRS JSON requests. */
public class OpenMrsJsonRequest extends JsonObjectRequest {
    private final String mUsername;
    private final String mPassword;

    private static Logger LOG = Logger.create();

    /**
     * Constructs a GET request to OpenMRS.
     * @param connectionDetails an {@link OpenMrsConnectionDetails} for communicating with OpenMRS
     * @param urlSuffix         the API URL being requested, relative to the API root
     * @param jsonRequest       a {@link JSONObject} containing the request body
     * @param listener          a {@link Response.Listener} that handles successful requests
     * @param errorListener     a {@link Response.ErrorListener} that handles failed requests
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

    private OpenMrsJsonRequest(String username, String password, String url, JSONObject jsonRequest,
                               Response.Listener<JSONObject> listener,
                               Response.ErrorListener errorListener) {
        super(url, jsonRequest, listener, errorListener);
        this.mUsername = username;
        this.mPassword = password;
    }

    /**
     * Constructs a request to OpenMRS using an arbitrary HTTP method.
     * @param connectionDetails an {@link OpenMrsConnectionDetails} for communicating with OpenMRS
     * @param method            the HTTP method
     * @param urlSuffix         the API URL being requested, relative to the API root
     * @param jsonRequest       a {@link JSONObject} containing the request body
     * @param listener          a {@link Response.Listener} that handles successful requests
     * @param errorListener     a {@link Response.ErrorListener} that handles failed requests
     */
    public OpenMrsJsonRequest(OpenMrsConnectionDetails connectionDetails,
                              int method, String urlSuffix, JSONObject jsonRequest,
                              Response.Listener<JSONObject> listener,
                              Response.ErrorListener errorListener) {
        super(method, connectionDetails.getBuendiaApiUrl() + urlSuffix,
            jsonRequest, listener, errorListener);
        this.mUsername = connectionDetails.getUser();
        this.mPassword = connectionDetails.getPassword();
    }

    @Override public Map<String, String> getHeaders() throws AuthFailureError {
        // TODO: work out how to do Auth properly
        HashMap<String, String> params = new HashMap<>();
        OpenMrsConnectionDetails.addAuthHeader(mUsername, mPassword, params);
        params.put("Connection-Type", "application/json");
        return params;
    }

    @Override protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
        LOG.finish("HTTP." + getSequence(), "Response to %s -> %s", Utils.repr(this), Utils.repr(response.data, 500));
        try {
            if (response.data.length == 0) {
                byte[] responseData = "{}".getBytes("UTF8");
                response = new NetworkResponse(
                    response.statusCode, responseData, response.headers, response.notModified);
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Should never occur");
        }

        // OkHttp deletes the Content-Type header, which breaks Unicode decoding! :(
        // We have to set it to ensure that the data will be interpreted as UTF-8.
        response.headers.put("Content-Type", "application/json; charset=utf-8");

        String requiredVersion = getMinimumVersionHeader(response);
        LOG.d("Client version is %s; server requires %s",
            BuildConfig.VERSION_NAME, requiredVersion);
        if (requiredVersion != null && isProductionVersion(BuildConfig.VERSION_NAME)) {
            if (Utils.ALPHANUMERIC_COMPARATOR.compare(
                BuildConfig.VERSION_NAME, requiredVersion) < 0) {
                LOG.e("Client version is %s, but server requires at least %s",
                    BuildConfig.VERSION_NAME, requiredVersion);
            }
        }
        return super.parseNetworkResponse(response);
    }

    private boolean isProductionVersion(String version) {
        return version.contains(".");
    }

    private String getMinimumVersionHeader(NetworkResponse response) {
        // HTTP headers are case-insensitive but NetworkResponse stores
        // them in a case-sensitive Map.  Sigh.
        for (String key : response.headers.keySet()) {
            if (eq(key.toLowerCase(), "buendia-client-minimum-version")) {
                return response.headers.get(key);
            }
        }
        return null;
    }
}
