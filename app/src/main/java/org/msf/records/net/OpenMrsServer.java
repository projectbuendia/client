package org.msf.records.net;

import android.support.annotation.Nullable;

import com.android.volley.Response;

import org.msf.records.model.Patient;

import java.util.List;
import java.util.Map;

/**
 * Implementation of Server RPCs that will talk
 * Created by nfortescue on 11/3/14.
 */
public class OpenMrsServer implements Server {

    @Override
    public void addPatient(Map<String, String> patientArguments,
                           Response.Listener<Patient> patientListener,
                           Response.ErrorListener errorListener, String logTag) {

    }

    @Override
    public void getPatient(String patientId, Response.Listener<Patient> patientListener,
                           Response.ErrorListener errorListener, String logTag) {

    }

    @Override
    public void updatePatient(String patientId, Map<String, String> patientArguments,
                              Response.Listener<Patient> patientListener,
                              Response.ErrorListener errorListener, String logTag) {

    }

    @Override
    public void listPatients(@Nullable String filterState, @Nullable String filterLocation,
                             @Nullable String filterQueryTerm,
                             Response.Listener<List<Patient>> patientListener,
                             Response.ErrorListener errorListener, String logTag) {

    }
}
