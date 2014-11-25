package org.msf.records.net;

import android.content.Context;
import android.util.Base64;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * A little bean to encapsulate the arguments for connecting to Open MRS, as they are shared
 * between various classes. This might be a sign that those classes should be combined.
 */
public class OpenMrsConnectionDetails {
    public final String rootUrl;
    public final String userName;
    public final String password;
    public final VolleySingleton volley;

    public OpenMrsConnectionDetails(@Nullable String rootUrl,
                                    @Nullable String userName,
                                    @Nullable String password,
                                    VolleySingleton volley) {
        this.rootUrl = (rootUrl == null) ? Constants.API_URL : rootUrl;
        this.userName = (userName == null) ? Constants.LOCAL_ADMIN_USERNAME : userName;
        this.password = (password == null) ? Constants.LOCAL_ADMIN_PASSWORD : password;
        this.volley = volley;
    }

    public OpenMrsConnectionDetails(@Nullable String rootUrl,
                                    @Nullable String userName,
                                    @Nullable String password,
                                    Context context) {
        this(rootUrl, userName, password,
                VolleySingleton.getInstance(context.getApplicationContext()));
    }

    public Map<String, String> addAuthHeader(HashMap<String, String> params) {
        return addAuthHeader(userName, password, params);
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
