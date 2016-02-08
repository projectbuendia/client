package org.projectbuendia.client.net;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;

import org.projectbuendia.client.App;

import java.util.HashMap;
import java.util.Map;

/**
 * A request that targets the OpenMRS REST API (as opposed to the Buendia API) and returns a string.
 */
public class OpenMrsStringRequest extends StringRequest {

    public OpenMrsStringRequest(
            int method, String urlSuffix, Response.Listener<String> listener,
            Response.ErrorListener errorListener) {
        super(
                method,
                App.getConnectionDetails().getRestApiUrl() + urlSuffix,
                listener,
                errorListener);
    }

    public OpenMrsStringRequest(
            String urlSuffix, Response.Listener<String> listener,
            Response.ErrorListener errorListener) {
        super(App.getConnectionDetails().getRestApiUrl() + urlSuffix, listener, errorListener);
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        HashMap<String, String> params = new HashMap<>();
        OpenMrsConnectionDetails.addAuthHeader(
                App.getConnectionDetails().getUser(),
                App.getConnectionDetails().getPassword(),
                params);
        return params;
    }
}
