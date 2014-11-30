package org.msf.records.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import org.msf.records.R;

public class TentSelectionFragment extends Fragment implements
        AdapterView.OnItemClickListener{
    private GridView mTentGrid;

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
        return inflater.inflate(R.layout.fragment_tent_selection, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mTentGrid = (GridView) view.findViewById(R.id.tent_selection_tents);
        mTentGrid.setOnItemClickListener(this);

        // TODO(akalachman): Remove and allow adapter to retrieve its own list.
        String[] values = new String[] {
                "S1", "S2", "P1", "P2", "C1", "C2", "C3", "C4", "C5", "C6", "Morgue"
        };
        TentListAdapter adapter = new TentListAdapter(getActivity(), values);
        mTentGrid.setAdapter(adapter);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // TODO(akalachman): Implement.
    }
}
