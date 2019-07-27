package org.projectbuendia.client.ui.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ExpandableListView;

import org.projectbuendia.client.App;
import org.projectbuendia.client.R;
import org.projectbuendia.client.events.actions.VoidObservationsRequestEvent;
import org.projectbuendia.client.models.ObsRow;
import org.projectbuendia.client.ui.lists.ExpandableVoidObsRowAdapter;
import org.projectbuendia.client.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;

public class VoidObservationsDialogFragment extends DialogFragment {

    private LayoutInflater mInflater;
    private ExpandableListView expListView;
    private List<String> listDataHeader;
    private HashMap<String, ArrayList<ObsRow>> listDataChild;

    public static VoidObservationsDialogFragment newInstance(ArrayList<ObsRow> observations) {
        Bundle args = new Bundle();
        args.putParcelableArrayList("obsrows", observations);
        VoidObservationsDialogFragment f = new VoidObservationsDialogFragment();
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

    private void prepareData(ArrayList<ObsRow> rows){

        ArrayList<ObsRow> child;
        String verifyTitle;
        String Title;

        listDataHeader = new ArrayList<>();
        listDataChild = new HashMap<>();

        for (ObsRow row: rows) {

            Title = row.conceptName + " " + Utils.formatShortDate(row.time);

            if(!isExistingHeader(Title)){
                listDataHeader.add(Title);
            }

        }

        for (String header: listDataHeader){

            child = new ArrayList<>();

            for (ObsRow row: rows){

                verifyTitle = row.conceptName + " " + Utils.formatShortDate(row.time);

                if (verifyTitle.equals(header)){
                    child.add(row);
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

        final ArrayList<ObsRow> obsrows = getArguments().getParcelableArrayList("obsrows");
        prepareData(obsrows);

        final ExpandableVoidObsRowAdapter listAdapter = new ExpandableVoidObsRowAdapter(App.getInstance().getApplicationContext(), listDataHeader, listDataChild);
        ExpandableListView listView = fragment.findViewById(R.id.obs_list);
        listView.setAdapter(listAdapter);

        for(int i=0; i < listAdapter.getGroupCount(); i++)
            listView.expandGroup(i);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setNegativeButton(R.string.cancel, (dialogInterface, i) -> dialogInterface.dismiss())
                .setPositiveButton(R.string.voiding, (dialogInterface, i) -> {
                    if ((listAdapter.mCheckedItems != null) && (!listAdapter.mCheckedItems.isEmpty())) {
                        EventBus.getDefault().post(new VoidObservationsRequestEvent(listAdapter.mCheckedItems));
                    }
                    dialogInterface.dismiss();
                }).setTitle(getResources().getString(R.string.void_observations))
                .setView(fragment);
        return builder.create();
    }

    @Override
    public void show(FragmentManager manager, String tag) {
        if (manager.findFragmentByTag(tag) == null) {
            super.show(manager, tag);
        }
    }
}
