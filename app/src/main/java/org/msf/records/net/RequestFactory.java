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

package org.msf.records.net;

import com.android.volley.Response;

import org.json.JSONObject;

/** A factory that creates Volley requests configured with a {@link RequestConfigurator}. */
public class RequestFactory {

    private final RequestConfigurator mConfigurator;

    RequestFactory(RequestConfigurator configurator) {
        mConfigurator = configurator;
    }

    /**
     * Returns an {@link OpenMrsJsonRequest} for a GET request to an API URL.
     * @param connectionDetails the {@link OpenMrsConnectionDetails} used to communicate with the
     *                          OpenMRS server
     * @param urlSuffix the API url to request, relative to the API root
     * @param jsonRequest a {@link JSONObject} containing the request body
     * @param listener a {@link Response.Listener} for handling a successful request
     * @param errorListener a {@link Response.ErrorListener} for handling a failed request
     * @return the configured {@link OpenMrsJsonRequest}
     */
    public OpenMrsJsonRequest newOpenMrsJsonRequest(
            OpenMrsConnectionDetails connectionDetails,
            String urlSuffix,
            JSONObject jsonRequest,
            Response.Listener<JSONObject> listener,
            Response.ErrorListener errorListener) {
        return mConfigurator.configure(
                new OpenMrsJsonRequest(
                        connectionDetails, urlSuffix, jsonRequest, listener,errorListener));
    }

    /**
     * Returns an {@link OpenMrsJsonRequest} for an arbitrary request.
     * @param connectionDetails the {@link OpenMrsConnectionDetails} used to communicate with the
     *                          OpenMRS server
     * @param method the HTTP method
     * @param url the absolute URL to request
     * @param jsonRequest a {@link JSONObject} containing the request body
     * @param listener a {@link Response.Listener} for handling a successful request
     * @param errorListener a {@link Response.ErrorListener} for handling a failed request
     * @return the configured {@link OpenMrsJsonRequest}
     */
    public OpenMrsJsonRequest newOpenMrsJsonRequest(
            OpenMrsConnectionDetails connectionDetails,
            int method,
            String url,
            JSONObject jsonRequest,
            Response.Listener<JSONObject> listener,
            Response.ErrorListener errorListener) {
        return mConfigurator.configure(
                new OpenMrsJsonRequest(
                        connectionDetails, method, url, jsonRequest, listener,errorListener));
    }
}
