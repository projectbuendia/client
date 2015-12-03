package org.projectbuendia.client.ui.dialogs;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ListView;

import org.projectbuendia.client.App;
import org.projectbuendia.client.R;
import org.projectbuendia.client.events.actions.OrderExecutionSaveRequestedEvent;
import org.projectbuendia.client.events.actions.VoidObservationRequestEvent;
import org.projectbuendia.client.events.actions.VoidObservationsRequestEvent;
import org.projectbuendia.client.models.ObsRow;
import org.projectbuendia.client.models.VoidObs;
import org.projectbuendia.client.ui.lists.ObsRowAdapter;
import org.projectbuendia.client.ui.lists.VoidObsRowAdapter;

import java.util.ArrayList;

import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;


public class VoidObservationsDialogFragment extends DialogFragment {

    private LayoutInflater mInflater;

    public static VoidObservationsDialogFragment newInstance(ArrayList<ObsRow> observations) {
        Bundle args = new Bundle();
        args.putParcelableArrayList("obsrows", observations);
        VoidObservationsDialogFragment f = new VoidObservationsDialogFragment();
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mInflater = LayoutInflater.from(getActivity());
    }

    @Override public @NonNull
    Dialog onCreateDialog(Bundle savedInstanceState) {
        final View fragment = mInflater.inflate(R.layout.void_observations_dialog_fragment, null);
        ButterKnife.inject(this, fragment);
        final ArrayList<ObsRow> obsrows = getArguments().getParcelableArrayList("obsrows");
        final VoidObsRowAdapter adapter = new VoidObsRowAdapter(App.getInstance().getApplicationContext(), obsrows);
        ListView listView = (ListView) fragment.findViewById(R.id.lvVoidObs);
        listView.setAdapter(adapter);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .setPositiveButton(R.string.voiding, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if ((adapter.mCheckedItems != null) && (!adapter.mCheckedItems.isEmpty())) {
                            //EventBus.getDefault().post(new VoidObservationsRequestEvent(adapter.mCheckedItems));
                        }
                        dialogInterface.dismiss();
                    }
                }).setTitle(getResources().getString(R.string.void_observations))
                .setView(fragment);
        return builder.create();
    }

}

