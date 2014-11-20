package org.msf.records.net;

import android.content.Context;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.android.volley.Request;
import com.android.volley.Response;

import org.msf.records.model.Patient;

import java.util.List;
import java.util.Map;

/**
 * Implementation of the Server interface which connects to the custom Buendia RPC server.
 * Created by nfortescue on 11/3/14.
 */
public class BuendiaServer implements Server {

    private static final String DEFAULT_ROOT_URL = "http://buendia.whitespell.com:8080/";
    private final String ROOT_URL;
    private final VolleySingleton mVolley;

    /**
     *
     * @param context the context to enable singleton construction to be done
     * @param rootUrl the root URL for API calls, or null to use the default
     */
    public BuendiaServer(Context context, @Nullable String rootUrl) {
        this.mVolley = VolleySingleton.getInstance(context.getApplicationContext());
        ROOT_URL = TextUtils.isEmpty(rootUrl) ? DEFAULT_ROOT_URL : rootUrl;
    }

    @Override
    public void addPatient(
            Map<String, String> patientArguments,
            Response.Listener<Patient> patientListener,
            Response.ErrorListener errorListener,
            String logTag) {
        mVolley.addToRequestQueue(new GsonRequest<Patient>(Request.Method.POST,
                patientArguments, ROOT_URL + "patients/", Patient.class, false, null,
                patientListener, errorListener), logTag);
    }

    @Override
    public void getPatient(
            String patientId,
            Response.Listener<Patient> patientListener,
            Response.ErrorListener errorListener,
            String logTag) {
        mVolley.addToRequestQueue(new GsonRequest<Patient>(
                ROOT_URL + "patients/" + patientId, Patient.class, false, null,
                patientListener, errorListener) {
        }, logTag);
    }

    @Override
    public void updatePatient(
            String patientId,
            Patient patientArguments,
            Response.Listener<Patient> patientListener,
            Response.ErrorListener errorListener, String logTag) {
        // TODO(akalachman): Fix compatibility with old server by making map from patientArguments.
        mVolley.addToRequestQueue(new GsonRequest<Patient>(Request.Method.PUT,
                        null, ROOT_URL + "patients/" + patientId, Patient.class, false,
                        null,
                        patientListener, errorListener),
                logTag);

    }

    @Override
    public void listPatients(@Nullable String filterState,
                             @Nullable String filterLocation,
                             @Nullable String filterQueryTerm,
                             Response.Listener<List<Patient>> patientListener,
                             Response.ErrorListener errorListener, String logTag) {
        String vars = "?";
        vars += filterState != null && !filterState.isEmpty() ? "state=" + filterState + "&" : "";
        vars += filterLocation != null ? "assigned_location_zone_id=" + filterLocation + "&" : "";
        vars += filterQueryTerm != null && !filterQueryTerm.isEmpty() ? "search=" + filterQueryTerm + "&" : "";
        mVolley.addToRequestQueue(
                new GsonRequest<List<Patient>>(ROOT_URL + "patients/" + vars,
                        Patient.class, true, null, patientListener, errorListener),
                logTag);

    }

    @Override
    public void cancelPendingRequests(String logTag) {
        mVolley.cancelPendingRequests(logTag);
    }
}
