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

import org.msf.records.prefs.StringPreference;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides up-to-date preferences for connecting to OpenMRS, as they are shared between various
 * classes.
 */
public class OpenMrsConnectionDetails {

    private final VolleySingleton mVolley;
    private final StringPreference mOpenMrsRootUrl;
    private final StringPreference mOpenMrsUser;
    private final StringPreference mOpenMrsPassword;

    public VolleySingleton getVolley() {
        return mVolley;
    }

    public String getBuendiaApiUrl() {
        return mOpenMrsRootUrl.get() + "/ws/rest/v1/projectbuendia";
    }

    public String getUserName() {
        return mOpenMrsUser.get();
    }

    public String getPassword() {
        return mOpenMrsPassword.get();
    }

    /**
     * Constructs an {@link OpenMrsConnectionDetails} object with the connection details for a
     * particular server.
     * @param volley a {@link VolleySingleton} used to make requests
     * @param openMrsRootUrl the URL of the OpenMRS server
     * @param openMrsUser the username used to authenticate OpenMRS requests
     * @param openMrsPassword the password used to authenticate OpenMRS requests
     */
    public OpenMrsConnectionDetails(
            VolleySingleton volley,
            StringPreference openMrsRootUrl,
            StringPreference openMrsUser,
            StringPreference openMrsPassword) {
        mVolley = volley;
        mOpenMrsRootUrl = openMrsRootUrl;
        mOpenMrsUser = openMrsUser;
        mOpenMrsPassword = openMrsPassword;
    }

    /**
     * Adds an authentication header to an existing map of header->header value.
     * @param params the header map
     * @return the header map, including an authentication header
     */
    public Map<String, String> addAuthHeader(HashMap<String, String> params) {
        return addAuthHeader(getUserName(), getPassword(), params);
    }

    /**
     * Adds an authentication header to an existing map of header->header value using a given
     * username and password.
     * @param username the OpenMRS username
     * @param password the OpenMRS password
     * @param params the header map
     * @return the header map, including an authentication header
     */
    public static Map<String, String> addAuthHeader(String username, String password,
                                                    Map<String, String> params) {
        String auth = OpenMrsConnectionDetails.makeBasicAuth(username, password);
        params.put("Authorization", auth);
        return params;
    }

    private static String makeBasicAuth(String username, String password) {
        String creds = String.format("%s:%s", username, password);
        return "Basic " + Base64.encodeToString(creds.getBytes(), Base64.DEFAULT);
    }
}
