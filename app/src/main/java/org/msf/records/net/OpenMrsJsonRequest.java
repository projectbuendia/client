package org.msf.records.net;

import android.util.Base64;

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

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        // TODO(nfortescue): work out how to do Auth properly
        HashMap<String, String> params = new HashMap<String, String>();
        String creds = String.format("%s:%s", mUsername, mPassword);
        String auth = "Basic " + Base64.encodeToString(creds.getBytes(), Base64.DEFAULT);
        params.put("Authorization", auth);
        return params;
    }
}
