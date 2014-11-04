package org.msf.records.net;

import android.support.annotation.Nullable;

import com.android.volley.Request;
import com.android.volley.Response;

import org.msf.records.App;
import org.msf.records.model.Patient;

import java.util.List;
import java.util.Map;

/**
 * Implementation of the Server interface which connects to the custom Buendia RPC server.
 * Created by nfortescue on 11/3/14.
 */
public class BuendiaServer implements Server {

    @Override
    public void addPatient(
            Map<String, String> patientArguments,
            Response.Listener<Patient> patientListener,
            Response.ErrorListener errorListener,
            String logTag) {
        App.getInstance().addToRequestQueue(new GsonRequest<Patient>(Request.Method.POST,
                patientArguments, App.API_ROOT_URL + "patients/", Patient.class, false, null,
                patientListener, errorListener), logTag);

    }

    @Override
    public void getPatient(
            String patientId,
            Response.Listener<Patient> patientListener,
            Response.ErrorListener errorListener,
            String logTag) {
        App.getInstance().addToRequestQueue(new GsonRequest<Patient>(
                App.API_ROOT_URL + "patients/" + patientId, Patient.class, false, null,
                patientListener, errorListener) {}, logTag);
    }

    @Override
    public void updatePatient(
            String patientId,
            Map<String, String> patientArguments,
            Response.Listener<Patient> patientListener,
            Response.ErrorListener errorListener, String logTag) {
        App.getInstance().addToRequestQueue(new GsonRequest<Patient>(Request.Method.PUT,
                        patientArguments, App.API_ROOT_URL + "patients/" + patientId, Patient.class, false,
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
        App.getInstance().addToRequestQueue(
                new GsonRequest<List<Patient>>(App.API_ROOT_URL + "patients/" + vars,
                        Patient.class, true, null, patientListener, errorListener),
                logTag);

    }
}
