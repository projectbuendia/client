package org.msf.records.ui;



import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.msf.records.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link PatientDetailNotesFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class PatientDetailNotesFragment extends Fragment {


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @return A new instance of fragment PatientDetailNotesFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static PatientDetailNotesFragment newInstance() {
        PatientDetailNotesFragment fragment = new PatientDetailNotesFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }
    public PatientDetailNotesFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_patient_detail_notes, container, false);
    }


}
