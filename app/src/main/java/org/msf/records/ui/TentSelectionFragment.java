package org.msf.records.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import org.msf.records.R;
import org.msf.records.filter.FilterGroup;
import org.msf.records.filter.FilterManager;
import org.msf.records.filter.SimpleSelectionFilter;
import org.msf.records.filter.TentFilter;
import org.msf.records.model.LocationTree;
import org.msf.records.model.LocationTreeFactory;
import org.msf.records.model.Zone;
import org.msf.records.utils.PatientCountDisplay;
import org.msf.records.view.SubtitledButtonView;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class TentSelectionFragment extends Fragment {
    @InjectView(R.id.tent_selection_tents) GridView mTentGrid;
    @InjectView(R.id.tent_selection_all_patients) SubtitledButtonView mAllPatientsButton;
    @InjectView(R.id.tent_selection_triage) SubtitledButtonView mTriageButton;
    @InjectView(R.id.tent_selection_discharged) SubtitledButtonView mDischargedButton;

    public static TentSelectionFragment newInstance() {
        TentSelectionFragment fragment = new TentSelectionFragment();
        return fragment;
    }

    public TentSelectionFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_tent_selection, container, false);

        ButterKnife.inject(this, view);

        LocationTree tree = new LocationTreeFactory(getActivity()).build();

        TentListAdapter adapter = new TentListAdapter(
                getActivity(), LocationTree.getTents(getActivity(), tree));
        mTentGrid.setAdapter(adapter);

        mTentGrid.setOnItemClickListener(new GridView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                LocationTree selectedItem = (LocationTree)parent.getItemAtPosition(position);
                Intent roundIntent = new Intent(getActivity(), RoundActivity.class);
                roundIntent.putExtra(
                        RoundActivity.TENT_NAME_KEY, selectedItem.toString());
                roundIntent.putExtra(
                        RoundActivity.TENT_UUID_KEY, selectedItem.getLocation().uuid);
                roundIntent.putExtra(
                        RoundActivity.TENT_PATIENT_COUNT_KEY, selectedItem.getPatientCount());
                startActivity(roundIntent);
            }
        });

        if (tree != null) {
            mAllPatientsButton.setSubtitle(
                    PatientCountDisplay.getPatientCountSubtitle(
                            getActivity(), tree.getPatientCount(), true));
        }

        for (LocationTree zone : LocationTree.getZones(getActivity(), tree)) {
            switch (zone.getLocation().uuid) {
                case Zone.TRIAGE_ZONE_UUID:
                    mTriageButton.setSubtitle(
                            PatientCountDisplay.getPatientCountSubtitle(
                                    getActivity(), zone.getPatientCount()));
                    break;
                case Zone.DISCHARGED_ZONE_UUID:
                    mDischargedButton.setSubtitle(
                            PatientCountDisplay.getPatientCountSubtitle(
                                    getActivity(), zone.getPatientCount()));
                    break;
            }
        }


        mAllPatientsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent listIntent = new Intent(getActivity(), PatientListActivity.class);
                startActivity(listIntent);
            }
        });

        mDischargedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO(akalachman): Implement.
            }
        });

        mTriageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO(akalachman): Implement.
            }
        });

        return view;
    }
}
