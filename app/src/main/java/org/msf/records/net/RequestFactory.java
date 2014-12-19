package org.msf.records.net;

import com.android.volley.Response;

import org.json.JSONObject;

/**
 * A factory that creates Volley requests.
 */
public class RequestFactory {

    private final RequestConfigurator mConfigurator;

    RequestFactory(RequestConfigurator configurator) {
        mConfigurator = configurator;
    }

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
