package org.msf.records.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.msf.records.R;
import org.msf.records.ui.patientlist.PatientListActivity;

/**
 * A fragment representing the main screen, allowing patients and rounds to be added.
 * This fragment is contained in a {@link PatientListActivity} in two-pane mode (on tablets).
 */
public class MainScreenFragment extends Fragment {
    public MainScreenFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_main_screen, container, false);
        SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        final boolean keepFormInstancesLocally =
                preferences.getBoolean("keep_form_instances_locally", false);
        if (keepFormInstancesLocally) {
            view.findViewById(R.id.view_xform_button).setVisibility(View.VISIBLE);
        }

        return view;


    }
}
