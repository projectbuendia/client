package org.msf.records.ui.dialogs;

import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.msf.records.R;
import org.msf.records.view.InstantAutoCompleteTextView;

import butterknife.InjectView;

/**
 * A {@link DialogFragment} for editing a user's assigned location.
 */
public class EditAssignedLocationDialogFragment extends DialogFragment {

    @InjectView(R.id.zone)
    InstantAutoCompleteTextView zone;

    @InjectView(R.id.tent)
    InstantAutoCompleteTextView tent;

    @InjectView(R.id.bed)
    InstantAutoCompleteTextView bed;

    @Nullable
    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View fragment = inflater.inflate(R.layout.dialog_fragment_edit_assigned_location, null);

        return fragment;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }
}
