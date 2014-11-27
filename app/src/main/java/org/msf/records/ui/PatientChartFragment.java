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
import org.msf.records.model.ChartStructure;
import org.msf.records.model.ConceptList;
import org.msf.records.model.PatientChart;
import org.msf.records.net.OpenMrsChartServer;
import org.msf.records.view.VitalView;

import java.util.Arrays;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * A {@link Fragment} that displays a patient's vitals and charts.
 */
public class PatientChartFragment extends Fragment {

    private static final String TAG = PatientChartFragment.class.getName();

    public static PatientChartFragment newInstance(String patientUuid) {
        PatientChartFragment fragment = new PatientChartFragment();
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
                Log.e(TAG, "Unexpected error on fetching chart", error);
            }
        });
        server.getConcepts(
                new Response.Listener<ConceptList>() {
                    @Override
                    public void onResponse(ConceptList response) {
                        Log.i(TAG, Integer.toString(response.results.length));
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Unexpected error fetching concepts", error);
                    }
                });
        server.getChartStructure("ea43f213-66fb-4af6-8a49-70fd6b9ce5d4",
                new Response.Listener<ChartStructure>() {
                    @Override
                    public void onResponse(ChartStructure response) {
                        Log.i(TAG, Arrays.asList(response.groups).toString());
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Unexpected error fetching concepts", error);
                    }
                });
        return fragment;
    }

    @InjectView(R.id.vital_heart) VitalView mHeart;
    @InjectView(R.id.vital_blood_pressure) VitalView mBloodPressure;
    @InjectView(R.id.vital_temperature) VitalView mTemperature;
    @InjectView(R.id.vital_respirations) VitalView mRespirations;
    @InjectView(R.id.vital_pcr) VitalView mPcr;

    public PatientChartFragment() {}

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_patient_chart, container, false);
        ButterKnife.inject(this, view);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        // TODO(dxchen,nfortescue): This is where the code to hook up the views to data should go.
    }
}
