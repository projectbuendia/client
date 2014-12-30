package org.msf.records.net;

import android.util.Base64;

import org.msf.records.prefs.StringPreference;

import java.util.HashMap;
import java.util.Map;

/**
 * OpenMrsConnectionDetails provides up-to-date preferences for connecting to Open MRS, as they are
 * shared between various classes. This might be a sign that those classes should be combined.
 */
public class OpenMrsConnectionDetails {

    private final VolleySingleton mVolley;
    private final StringPreference mOpenMrsRootUrl;
    private final StringPreference mOpenMrsUser;
    private final StringPreference mOpenMrsPassword;

    public VolleySingleton getVolley() {
        return mVolley;
    }

    public String getRootUrl() {
        return mOpenMrsRootUrl.get();
    }

    public String getUserName() {
        return mOpenMrsUser.get();
    }

    public String getPassword() {
        return mOpenMrsPassword.get();
    }

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

    public Map<String, String> addAuthHeader(HashMap<String, String> params) {
        return addAuthHeader(getUserName(), getPassword(), params);
    }

    public static String makeBasicAuth(String username, String password) {
        String creds = String.format("%s:%s", username, password);
        return "Basic " + Base64.encodeToString(creds.getBytes(), Base64.DEFAULT);
    }

    public static Map<String, String> addAuthHeader(String username, String password,
                                     Map<String, String> params) {
        String auth = OpenMrsConnectionDetails.makeBasicAuth(username, password);
        params.put("Authorization", auth);
        return params;
    }
}
