package org.msf.records.net;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Base64;

import org.msf.records.App;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * OpenMrsConnectionDetails provides up-to-date preferences for connecting to Open MRS, as they are
 * shared between various classes. This might be a sign that those classes should be combined.
 */
public class OpenMrsConnectionDetails {
    private final VolleySingleton volley;
    private final SharedPreferences preferences;

    public VolleySingleton getVolley() {
        return volley;
    }

    public String getRootUrl() {
        return preferences.getString("openmrs_root_url", null);
    }

    public String getUserName() {
        return preferences.getString("openmrs_user", null);
    }

    public String getPassword() {
        return preferences.getString("openmrs_password", null);
    }

    public OpenMrsConnectionDetails(Context context, VolleySingleton volley) {
        this.volley = volley;
        this.preferences =
                PreferenceManager.getDefaultSharedPreferences(App.getInstance());
    }

    public OpenMrsConnectionDetails(Context context) {
        this(App.getInstance(), VolleySingleton.getInstance(context.getApplicationContext()));
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
