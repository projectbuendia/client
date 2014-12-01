package org.msf.records.ui;

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
import org.msf.records.view.SubtitledButtonView;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class TentSelectionFragment extends Fragment implements
        AdapterView.OnItemClickListener{
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

        mTentGrid.setOnItemClickListener(this);

        // TODO(akalachman): Remove and allow adapter to retrieve its own list.
        String[] values = new String[] {
                "S1", "S2", "P1", "P2", "C1", "C2", "C3", "C4", "C5", "C6", "Morgue"
        };
        TentListAdapter adapter = new TentListAdapter(getActivity(), values);
        mTentGrid.setAdapter(adapter);

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

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // TODO(akalachman): Implement.
    }
}
