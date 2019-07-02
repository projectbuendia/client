package org.projectbuendia.client.ui.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;

import org.projectbuendia.client.App;
import org.projectbuendia.client.R;
import org.projectbuendia.client.models.ObsRow;
import org.projectbuendia.client.ui.lists.ExpandableObsRowAdapter;
import org.projectbuendia.client.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.ButterKnife;

public class ObsDetailDialogFragment extends DialogFragment {

    private static final String KEY_OBSROWS = "obsrows";
    private static final String KEY_CONCEPT_UUIDS = "conceptUuids";

    private LayoutInflater mInflater;
    private ExpandableListAdapter listAdapter;
    private ExpandableListView expListView;
    private List<String> listDataHeader;
    private HashMap<String, List<String>> listDataChild;

    public static ObsDetailDialogFragment newInstance(ArrayList<ObsRow> observations,
                                                      ArrayList<String> orderedConceptUuids) {
        Bundle args = new Bundle();
        args.putParcelableArrayList(KEY_OBSROWS, observations);
        args.putStringArrayList(KEY_CONCEPT_UUIDS, orderedConceptUuids);
        ObsDetailDialogFragment f = new ObsDetailDialogFragment();
        f.setArguments(args);
        return f;
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mInflater = LayoutInflater.from(getActivity());
    }

    private boolean isExistingHeader(String check){

        for (String header:listDataHeader)
            if (header.equals(check)) return true;

        return false;
    }

    private void prepareData(ArrayList<ObsRow> rows, final ArrayList<String> conceptUuids) {

        List<String> child;
        String verifyTitle;
        String Title;

        listDataHeader = new ArrayList<>();
        listDataChild = new HashMap<>();

        if (conceptUuids != null) {
            Utils.sortObsRows(rows, conceptUuids);
        }

        for (ObsRow row: rows) {

            Title = row.conceptName + " \u2022 " + row.day;

            if(!isExistingHeader(Title)){
                listDataHeader.add(Title);
            }

        }

        for (String header: listDataHeader){

            child = new ArrayList<>();

            for (ObsRow row : rows) {
                verifyTitle = row.conceptName + " \u2022 " + row.day;

                if (verifyTitle.equals(header)) {
                    child.add(row.time + " \u2014 " + row.valueName);
                }
            }

            if (!child.isEmpty()){
                listDataChild.put(header, child);
            }
        }
    }

    @Override public @NonNull Dialog onCreateDialog(Bundle savedInstanceState) {
        View fragment = mInflater.inflate(R.layout.obs_detail_dialog_fragment, null);
        ButterKnife.inject(this, fragment);

        final ArrayList<ObsRow> obsrows = getArguments().getParcelableArrayList(KEY_OBSROWS);
        final ArrayList<String> conceptUuids = getArguments().getStringArrayList(KEY_CONCEPT_UUIDS);
        prepareData(obsrows, conceptUuids);

        listAdapter = new ExpandableObsRowAdapter(App.getInstance().getApplicationContext(), listDataHeader, listDataChild);
        ExpandableListView listView = (ExpandableListView) fragment.findViewById(R.id.lvObs);
        listView.setAdapter(listAdapter);

        for(int i=0; i < listAdapter.getGroupCount(); i++)
            listView.expandGroup(i);

        // Omit the "void observations switch" for now.  (Ping, 2019-03-19)
        //
        // LinearLayout listFooterView = (LinearLayout)mInflater.inflate(R.layout.void_observations_switch, null);
        // listView.addFooterView(listFooterView);


        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .setView(fragment);

        final Dialog dialog = builder.create();

        // Omit the "void observations switch" for now.  (Ping, 2019-03-19)
        /*
        final Switch swVoid = (Switch) fragment.findViewById(R.id.swVoid);
        swVoid.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (swVoid.isChecked()){
                    VoidObservationsDialogFragment.newInstance(obsrows)
                            .show(getActivity().getSupportFragmentManager(), null);
                }
                dialog.dismiss();
            }
        });
        */

        return dialog;
    }

    @Override
    public void show(FragmentManager manager, String tag) {
        if (manager.findFragmentByTag(tag) == null) {
            super.show(manager, tag);
        }
    }
}

