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
import android.view.WindowManager;

import com.google.android.flexbox.FlexboxLayout;
import com.google.common.base.Optional;

import org.projectbuendia.client.App;
import org.projectbuendia.client.AppSettings;
import org.projectbuendia.client.R;
import org.projectbuendia.client.events.CrudEventBus;
import org.projectbuendia.client.models.AppModel;
import org.projectbuendia.client.models.Location;
import org.projectbuendia.client.models.LocationForest;
import org.projectbuendia.client.models.Patient;
import org.projectbuendia.client.models.PatientDelta;
import org.projectbuendia.client.models.Zones;
import org.projectbuendia.client.ui.chart.PatientChartActivity;
import org.projectbuendia.client.ui.lists.LocationOption;
import org.projectbuendia.client.ui.lists.LocationOptionList;
import org.projectbuendia.client.utils.ContextUtils;
import org.projectbuendia.client.utils.Utils;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/** A {@link DialogFragment} for updating a patient's location and bed number. */
public class PatientLocationDialogFragment extends DialogFragment {
    @Inject AppModel mModel;
    @Inject AppSettings mSettings;
    @Inject CrudEventBus mCrudEventBus;
    private ContextUtils c;

    private FlexboxLayout mContainer;
    private LocationOptionList mList;
    private String patientUuid;

    /** Creates a new instance and registers the given UI, if specified. */
    public static PatientLocationDialogFragment newInstance(Patient patient) {
        PatientLocationDialogFragment fragment = new PatientLocationDialogFragment();
        Bundle args = new Bundle();
        args.putString("patientUuid", patient.uuid);
        args.putString("locationUuid", patient.locationUuid);
        fragment.setArguments(args);
        return fragment;
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.getInstance().inject(this);
        c = ContextUtils.from(getActivity());
    }

    @Override public @NonNull Dialog onCreateDialog(Bundle savedInstanceState) {
        patientUuid = getArguments().getString("patientUuid");

        AlertDialog dialog = c.buildDialog(R.layout.patient_location_dialog_fragment)
            .setCancelable(false) // Disable auto-cancel.
            .setTitle(R.string.action_assign_location)
            .setPositiveButton(getResources().getString(R.string.ok), (dialogInterface, i) -> onSubmit())
            .setNegativeButton(getResources().getString(R.string.cancel), null)
            .create();
        dialog.getWindow().setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        LocationForest forest = mModel.getForest(mSettings.getLocaleTag());
        mList = new LocationOptionList(c.findView(R.id.list_container));
        mList.setOptions(getLocationOptions(forest));
        mList.setSelectedUuid(getArguments().getString("locationUuid"));
        return dialog;
    }

    private List<LocationOption> getLocationOptions(LocationForest forest) {
        Location discharged = forest.get(Zones.DISCHARGED_ZONE_UUID);
        int numDischarged = discharged != null ? forest.countPatientsIn(discharged) : 0;
        int numPatients = forest.countAllPatients();

        int fg = c.color(R.color.vital_fg_light);
        int bg = c.color(R.color.zone_confirmed);

        List<LocationOption> options = new ArrayList<>();
        options.add(new LocationOption(
            null, c.str(R.string.all_present_patients), numPatients - numDischarged, fg, bg, 1, false));
        for (Location location : forest.allNodes()) {
            if (forest.isLeaf(location)) {
                double size = location.depth > 1 ? 0.5 : 1;
                options.add(new LocationOption(
                    location.uuid, location.name, forest.countPatientsIn(location), fg, bg, size, false));
            }
        }
        return options;
    }

    public void onSubmit() {
        Utils.logUserAction("location_assigned");
        ((PatientChartActivity) getActivity()).getUi().showWaitDialog(R.string.title_updating_patient);
        PatientDelta delta = new PatientDelta();
        String locationUuid = mList.getSelectedUuid();
        delta.assignedLocationUuid = Optional.of(locationUuid);
        mModel.updatePatient(mCrudEventBus, patientUuid, delta);
        dismiss();
    }
}
