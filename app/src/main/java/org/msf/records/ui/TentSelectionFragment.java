package org.msf.records.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import org.msf.records.R;
import org.msf.records.model.LocationTree;
import org.msf.records.model.Zone;
import org.msf.records.utils.PatientCountDisplay;
import org.msf.records.view.SubtitledButtonView;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class TentSelectionFragment extends Fragment {
    @InjectView(R.id.tent_selection_tents) GridView mTentGrid;
    @InjectView(R.id.tent_selection_all_patients) SubtitledButtonView mAllPatientsButton;
    @InjectView(R.id.tent_selection_triage) SubtitledButtonView mTriageButton;
    @InjectView(R.id.tent_selection_discharged) SubtitledButtonView mDischargedButton;

    private LocationTree mDischargedZone = null;
    private LocationTree mTriageZone = null;

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

        LocationTree tree = LocationTree.getRootLocation(getActivity());

        TentListAdapter adapter = new TentListAdapter(
                getActivity(), LocationTree.getTents(getActivity(), tree));
        mTentGrid.setAdapter(adapter);

        mTentGrid.setOnItemClickListener(new GridView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                launchActivityForLocation((LocationTree) parent.getItemAtPosition(position));
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
                    mTriageZone = zone;
                    break;
                // TODO(akalachman): Revisit if discharged should be treated differently.
                case Zone.DISCHARGED_ZONE_UUID:
                    mDischargedButton.setSubtitle(
                            PatientCountDisplay.getPatientCountSubtitle(
                                    getActivity(), zone.getPatientCount()));
                    mDischargedZone = zone;
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
                if (mDischargedZone != null) {
                    launchActivityForLocation(mDischargedZone);
                }
            }
        });

        mTriageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTriageZone != null) {
                    launchActivityForLocation(mTriageZone);
                }
            }
        });

        return view;
    }

    private void launchActivityForLocation(LocationTree locationTree) {
        Intent roundIntent = new Intent(getActivity(), RoundActivity.class);
        roundIntent.putExtra(
                RoundActivity.LOCATION_NAME_KEY, locationTree.toString());
        roundIntent.putExtra(
                RoundActivity.LOCATION_UUID_KEY, locationTree.getLocation().uuid);
        roundIntent.putExtra(
                RoundActivity.LOCATION_PATIENT_COUNT_KEY, locationTree.getPatientCount());
        startActivity(roundIntent);
    }
}
