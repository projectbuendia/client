package org.projectbuendia.client.ui.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

import org.projectbuendia.client.R;
import org.projectbuendia.client.models.Obs;

import java.util.ArrayList;

public class ViewObservationsDialogFragment extends DialogFragment {

    public static ViewObservationsDialogFragment newInstance(ArrayList<Obs> observations) {
        Bundle args = new Bundle();
        ViewObservationsDialogFragment f = new ViewObservationsDialogFragment();
        f.setArguments(args);
        return f;
    }

    @Override public @NonNull Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .setView(null);

        return builder.create();
    }
}

