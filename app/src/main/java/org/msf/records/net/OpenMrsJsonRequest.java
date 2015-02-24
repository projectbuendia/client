package org.msf.records.net;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Base class for OpenMRS authenticated JSON requests.
 */
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

    public OpenMrsJsonRequest(OpenMrsConnectionDetails connectionDetails,
                              String urlSuffix,
                              JSONObject jsonRequest,
                              Response.Listener<JSONObject> listener,
                              Response.ErrorListener errorListener) {
        this(connectionDetails.getUserName(), connectionDetails.getPassword(),
                connectionDetails.getBuendiaApiUrl() + urlSuffix,
                jsonRequest, listener, errorListener);
    }

    public OpenMrsJsonRequest(OpenMrsConnectionDetails connectionDetails,
                              int method, String url, JSONObject jsonRequest,
                              Response.Listener<JSONObject> listener,
                              Response.ErrorListener errorListener) {
        super(method, url, jsonRequest, listener, errorListener);
        this.mUsername = connectionDetails.getUserName();
        this.mPassword = connectionDetails.getPassword();
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        // TODO(nfortescue): work out how to do Auth properly
        HashMap<String, String> params = new HashMap<>();
        OpenMrsConnectionDetails.addAuthHeader(mUsername, mPassword, params);
        params.put("Connection-Type", "application/json");
        return params;
    }
}
