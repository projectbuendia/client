package org.msf.records.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.msf.records.R;

public class TentSelectionFragment extends Fragment {
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
}
