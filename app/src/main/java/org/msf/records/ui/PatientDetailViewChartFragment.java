package org.msf.records.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.msf.records.App;
import org.msf.records.R;
import org.msf.records.model.PatientChart;
import org.msf.records.net.OpenMrsChartServer;

import java.util.Arrays;

/**
 * Displays a chart for a given patient. For now, this is actually
 * just a demo image.
 */
public class PatientDetailViewChartFragment extends Fragment {
    private static final String TAG = "PatientDetailViewChartFragment";

    public static PatientDetailViewChartFragment newInstance(String patientUuid) {
        PatientDetailViewChartFragment fragment = new PatientDetailViewChartFragment();
        OpenMrsChartServer server = new OpenMrsChartServer(App.getConnectionDetails());
        // TODO(nfortescue): get proper caching, and the dictionary working.
        server.getChart(patientUuid, new Response.Listener<PatientChart>() {
            @Override
            public void onResponse(PatientChart response) {
                Log.i(TAG, response.uuid + " " + Arrays.asList(response.encounters));
            }
        },
        new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, error.toString());
            }
        });
        return fragment;
    }

    public PatientDetailViewChartFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_patient_detail_view_chart, container, false);
    }
}
