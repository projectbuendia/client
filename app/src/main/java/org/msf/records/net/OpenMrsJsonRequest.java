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

    private String mUsername;
    private String mPassword;

    public OpenMrsJsonRequest(String username, String password, String url, JSONObject jsonRequest,
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
        this(connectionDetails.userName, connectionDetails.password,
                connectionDetails.rootUrl + urlSuffix,
                jsonRequest, listener, errorListener);
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        // TODO(nfortescue): work out how to do Auth properly
        HashMap<String, String> params = new HashMap<>();
        OpenMrsConnectionDetails.addAuthHeader(mUsername, mPassword, params);
        return params;
    }
}
