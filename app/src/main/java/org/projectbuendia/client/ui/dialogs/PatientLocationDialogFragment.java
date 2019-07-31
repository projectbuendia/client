// Copyright 2015 The Project Buendia Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at: http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distrib-
// uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
// specific language governing permissions and limitations under the License.

package org.projectbuendia.client.ui.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import com.google.android.flexbox.FlexboxLayout;
import com.google.common.collect.ImmutableList;

import org.projectbuendia.client.App;
import org.projectbuendia.client.R;
import org.projectbuendia.client.events.CrudEventBus;
import org.projectbuendia.client.models.AppModel;
import org.projectbuendia.client.models.CursorLoader;
import org.projectbuendia.client.models.Patient;
import org.projectbuendia.client.models.TypedCursorWithLoader;
import org.projectbuendia.client.providers.Contracts;
import org.projectbuendia.client.ui.lists.LocationOption;
import org.projectbuendia.client.ui.lists.LocationOptionList;
import org.projectbuendia.client.utils.LocaleSelector;
import org.projectbuendia.client.utils.Utils;

import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;

/** A {@link DialogFragment} for updating a patient's location and bed number. */
public class PatientLocationDialogFragment extends DialogFragment {
    @Inject AppModel mModel;
    @Inject CrudEventBus mCrudEventBus;
    private LayoutInflater mInflater;

    @InjectView(R.id.list_container) FlexboxLayout mContainer;
    private LocationOptionList mList;

    /** Creates a new instance and registers the given UI, if specified. */
    public static PatientLocationDialogFragment newInstance(Patient patient) {
        PatientLocationDialogFragment fragment = new PatientLocationDialogFragment();
        Bundle args = new Bundle();
        args.putString("uuid", patient.uuid);
        args.putString("locationUuid", patient.locationUuid);
        fragment.setArguments(args);
        return fragment;
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.getInstance().inject(this);
        mInflater = LayoutInflater.from(getActivity());
    }

    @Override public @NonNull Dialog onCreateDialog(Bundle savedInstanceState) {
        View fragment = mInflater.inflate(R.layout.patient_location_dialog_fragment, null);
        ButterKnife.inject(this, fragment);
        populateDialog(getArguments());

        AlertDialog dialog = new AlertDialog.Builder(getActivity())
            .setCancelable(false) // Disable auto-cancel.
            .setTitle(R.string.action_assign_location)
            .setPositiveButton(getResources().getString(R.string.ok), (dialogInterface, i) -> onSubmit())
            .setNegativeButton(getResources().getString(R.string.cancel), null)
            .setView(fragment)
            .create();
        dialog.getWindow().setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        return dialog;
    }

    private void populateDialog(Bundle args) {
        String locale = LocaleSelector.getCurrentLocaleTag();
        CursorLoader<LocationOption> loader = cursor -> new LocationOption(
            Utils.getString(cursor, Contracts.LocalizedLocations.UUID),
            Utils.getString(cursor, Contracts.LocalizedLocations.NAME),
            Utils.getLong(cursor, Contracts.LocalizedLocations.PATIENT_COUNT),
            0,
            0
        );

        try (Cursor cursor = getActivity().getContentResolver().query(
            Contracts.getLocalizedLocationsUri(locale), null, null, null, null)) {
            List<LocationOption> options =
                ImmutableList.copyOf(new TypedCursorWithLoader<>(cursor, loader));
            mList = new LocationOptionList(getActivity(), mContainer);
            mList.setOptions(options);
            mList.setSelectedUuid(args.getString("locationUuid"));
        }
    }

    public void onSubmit() {
    }
}
