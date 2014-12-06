package org.msf.records.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import org.msf.records.R;
import org.msf.records.events.location.LocationsLoadFailedEvent;
import org.msf.records.events.location.LocationsLoadedEvent;
import org.msf.records.location.LocationManager;
import org.msf.records.location.LocationTree;
import org.msf.records.model.Zone;
import org.msf.records.utils.PatientCountDisplay;
import org.msf.records.view.SubtitledButtonView;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;

public class TentSelectionFragment extends Fragment {
    @InjectView(R.id.tent_selection_tents) GridView mTentGrid;
    @InjectView(R.id.tent_selection_all_patients) SubtitledButtonView mAllPatientsButton;
    @InjectView(R.id.tent_selection_triage) SubtitledButtonView mTriageButton;
    @InjectView(R.id.tent_selection_discharged) SubtitledButtonView mDischargedButton;

    private static LocationTree mRoot = null;
    private LocationTree mDischargedZone = null;
    private LocationTree mTriageZone = null;

    public static TentSelectionFragment newInstance() {
        TentSelectionFragment fragment = new TentSelectionFragment();
        return fragment;
    }

    public TentSelectionFragment() {
        // Required empty public constructor
    }

    public synchronized void onEventMainThread(LocationsLoadFailedEvent event) {
        Toast.makeText(getActivity(), R.string.location_load_error, Toast.LENGTH_SHORT).show();
    }

    public synchronized void onEventMainThread(LocationsLoadedEvent event) {
        mRoot = event.mLocationTree;
        TentListAdapter adapter = new TentListAdapter(
                getActivity(), LocationTree.getTents(getActivity(), mRoot));
        mTentGrid.setAdapter(adapter);

        mTentGrid.setOnItemClickListener(new GridView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                launchActivityForLocation((LocationTree) parent.getItemAtPosition(position));
            }
        });

        if (mRoot != null) {
            mAllPatientsButton.setSubtitle(
                    PatientCountDisplay.getPatientCountSubtitle(
                            getActivity(), mRoot.getPatientCount(), true));
        }

        for (LocationTree zone : LocationTree.getZones(getActivity(), mRoot)) {
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

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        EventBus.getDefault().register(this);
        new LocationManager().loadLocations();
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);

        super.onPause();
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
