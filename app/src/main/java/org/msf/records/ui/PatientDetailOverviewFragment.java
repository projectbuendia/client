package org.msf.records.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.msf.records.R;

/**
 * Created by Gil on 03/10/2014.
 */

public class PatientDetailOverviewFragment extends Fragment {
    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */

    public PatientDetailOverviewFragment() {
    }

    public static PatientDetailOverviewFragment newInstance(String patientId){
        PatientDetailOverviewFragment patientDetailOverviewFragment = new PatientDetailOverviewFragment();
        Bundle bundle = new Bundle();
        bundle.putString(PatientDetailFragment.PATIENT_ID_KEY, patientId);
        patientDetailOverviewFragment.setArguments(bundle);
        return patientDetailOverviewFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_patient_detail_overview, container, false);
    }
}
