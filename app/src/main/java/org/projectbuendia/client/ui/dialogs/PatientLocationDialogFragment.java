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
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.WindowManager;
import android.widget.EditText;

import org.joda.time.DateTime;
import org.projectbuendia.client.App;
import org.projectbuendia.client.AppSettings;
import org.projectbuendia.client.R;
import org.projectbuendia.client.events.CrudEventBus;
import org.projectbuendia.client.json.ConceptType;
import org.projectbuendia.client.models.AppModel;
import org.projectbuendia.client.models.ConceptUuids;
import org.projectbuendia.client.models.Location;
import org.projectbuendia.client.models.LocationForest;
import org.projectbuendia.client.models.Obs;
import org.projectbuendia.client.models.Patient;
import org.projectbuendia.client.ui.chart.PatientChartActivity;
import org.projectbuendia.client.ui.lists.LocationOptionList;
import org.projectbuendia.client.utils.ContextUtils;
import org.projectbuendia.client.utils.Utils;

import javax.inject.Inject;

import static org.projectbuendia.client.utils.Utils.eq;

/** A {@link DialogFragment} for updating a patient's location and bed number. */
public class PatientLocationDialogFragment extends DialogFragment {
    @Inject AppModel mModel;
    @Inject AppSettings mSettings;
    @Inject CrudEventBus mCrudEventBus;
    private ContextUtils c;

    private LocationOptionList list;
    private String patientUuid;
    private EditText bedNumber;
    private String initialLocationUuid;
    private String initialBedNumber;

    /** Creates a new instance and registers the given UI, if specified. */
    public static PatientLocationDialogFragment newInstance(Patient patient) {
        PatientLocationDialogFragment fragment = new PatientLocationDialogFragment();
        Bundle args = new Bundle();
        args.putString("patientUuid", patient.uuid);
        args.putString("locationUuid", patient.locationUuid);
        args.putString("bedNumber", patient.bedNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.inject(this);
        c = ContextUtils.from(getActivity());
    }

    @Override public @NonNull Dialog onCreateDialog(Bundle savedInstanceState) {
        patientUuid = getArguments().getString("patientUuid");

        AlertDialog dialog = c.buildDialog(R.layout.patient_location_dialog_fragment)
            .setCancelable(false) // Disable auto-cancel.
            .setTitle(R.string.action_assign_location)
            .setPositiveButton(getString(R.string.ok), (dialogInterface, i) -> onSubmit())
            .setNegativeButton(getString(R.string.cancel), null)
            .create();
        dialog.getWindow().setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        initialLocationUuid = getArguments().getString("locationUuid");
        initialBedNumber = getArguments().getString("bedNumber");

        LocationForest forest = mModel.getForest();
        list = new LocationOptionList(c.findView(R.id.list_container), true);
        list.setLocations(forest, forest.getLeaves());
        list.setSelectedLocation(forest.get(initialLocationUuid));

        bedNumber = c.findView(R.id.bed_number);
        bedNumber.setFilters(new InputFilter[] {new InputFilter.AllCaps()});
        bedNumber.setText(initialBedNumber);
        bedNumber.setSelection(bedNumber.getText().length());
        bedNumber.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) {
                if (eq(list.getSelectedLocation().uuid, initialLocationUuid)) {
                    initialBedNumber = bedNumber.getText().toString();
                }
            }
        });
        list.setOnLocationSelectedListener(location -> {
            if (eq(location.uuid, initialLocationUuid)) {
                bedNumber.setText(initialBedNumber);
                bedNumber.setSelection(bedNumber.getText().length());
            } else {
                bedNumber.setText("");
            }
        });

        return dialog;
    }

    public void onSubmit() {
        Utils.logUserAction("location_assigned");
        ((PatientChartActivity) getActivity()).getUi().showWaitDialog(R.string.title_updating_patient);

        Location location = list.getSelectedLocation();
        String bedNumber = this.bedNumber.getText().toString().toUpperCase();
        String placement = location != null ? location.uuid + "/" + bedNumber : null;
        mModel.addObservationEncounter(mCrudEventBus, patientUuid, new Obs(
            null, patientUuid, DateTime.now(), ConceptUuids.PLACEMENT_UUID,
            ConceptType.TEXT, placement, null
        ));
        dismiss();
    }
}
