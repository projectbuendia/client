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
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;

import org.projectbuendia.client.App;
import org.projectbuendia.client.R;
import java.util.ArrayList;
import butterknife.ButterKnife;
import org.projectbuendia.client.models.ObsRow;
import org.projectbuendia.client.ui.lists.ObsRowAdapter;

public class ViewObservationsDialogFragment extends DialogFragment {

    private LayoutInflater mInflater;

    public static ViewObservationsDialogFragment newInstance(ArrayList<ObsRow> observations) {
        Bundle args = new Bundle();
        args.putParcelableArrayList("obsrows", observations);
        ViewObservationsDialogFragment f = new ViewObservationsDialogFragment();
        f.setArguments(args);
        return f;
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mInflater = LayoutInflater.from(getActivity());
    }

    @Override public @NonNull Dialog onCreateDialog(Bundle savedInstanceState) {
        View fragment = mInflater.inflate(R.layout.view_observations_dialog_fragment, null);
        ButterKnife.inject(this, fragment);

        final ArrayList<ObsRow> obsrows = getArguments().getParcelableArrayList("obsrows");
        ObsRowAdapter adapter = new ObsRowAdapter(App.getInstance().getApplicationContext(), obsrows);
        ListView listView = (ListView) fragment.findViewById(R.id.lvObs);
        listView.setAdapter(adapter);
        LinearLayout listFooterView = (LinearLayout)mInflater.inflate(R.layout.void_observations_switch, null);
        listView.addFooterView(listFooterView);
        final Switch swVoid = (Switch) fragment.findViewById(R.id.swVoid);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (swVoid.isChecked()){
                            VoidObservationsDialogFragment.newInstance(obsrows)
                                    .show(getActivity().getSupportFragmentManager(), null);
                        }
                        dialogInterface.dismiss();
                    }
                })
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

