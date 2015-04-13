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

import android.util.Base64;

import org.msf.records.AppSettings;

import java.util.Map;

/** Provides the URL and credentials for connecting to OpenMRS. */
public class OpenMrsConnectionDetails {

    private final VolleySingleton mVolley;
    private final AppSettings mSettings;

    /** Gets the Volley instance to use for network connections. */
    public VolleySingleton getVolley() {
        return mVolley;
    }

    /** Gets the URL to the API served by the Buendia module in OpenMRS. */
    public String getBuendiaApiUrl() {
        // The default value is set by setDefaultValues, not specified here.
        return mSettings.getOpenmrsUrl("/ws/rest/v1/projectbuendia");
    }

    /** Gets the OpenMRS username to use. */
    public String getUser() {
        return mSettings.getOpenmrsUser();
    }

    /** Gets the OpenMRS password to use. */
    public String getPassword() {
        return mSettings.getOpenmrsPassword();
    }

    /**
     * Constructs an {@link OpenMrsConnectionDetails} object.
     * @param volley the {@link VolleySingleton} for making requests
     * @param settings the application settings
     */
    public OpenMrsConnectionDetails(VolleySingleton volley, AppSettings settings) {
        mVolley = volley;
        mSettings = settings;
    }

    /**
     * Adds an authentication header to an existing map of HTTP headers.
     * @param params the header map to be modified
     * @return the modified header map, for method chaining
     */
    public Map<String, String> addAuthHeader(Map<String, String> params) {
        return addAuthHeader(getUser(), getPassword(), params);
    }

    /**
     * Adds an authentication header to an existing map of HTTP headers.
     * @param username the username
     * @param password the password
     * @param params the header map to be modified
     * @return the modified header map, for method chaining
     */
    public static Map<String, String> addAuthHeader(
            String username, String password, Map<String, String> params) {
        String creds = String.format("%s:%s", username, password);
        String encoded = Base64.encodeToString(creds.getBytes(), Base64.DEFAULT);
        params.put("Authorization", "Basic " + encoded);
        return params;
    }
}
