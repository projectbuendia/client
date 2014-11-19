package org.msf.records.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.msf.records.R;

/**
 * Displays a chart for a given patient. For now, this is actually
 * just a demo image.
 */
public class PatientDetailViewChartFragment extends Fragment {
    public static PatientDetailViewChartFragment newInstance(String patientId) {
        // TODO(akalachman): Use patient id once this fragment is more than a demo.
        return new PatientDetailViewChartFragment();
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
