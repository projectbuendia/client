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
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.InputFilter;
import android.view.WindowManager;
import android.widget.EditText;

import org.joda.time.DateTime;
import org.projectbuendia.client.App;
import org.projectbuendia.client.R;
import org.projectbuendia.client.json.Datatype;
import org.projectbuendia.models.ConceptUuids;
import org.projectbuendia.models.Location;
import org.projectbuendia.models.LocationForest;
import org.projectbuendia.models.Obs;
import org.projectbuendia.models.Patient;
import org.projectbuendia.client.ui.EditTextWatcher;
import org.projectbuendia.client.ui.chart.PatientChartActivity;
import org.projectbuendia.client.ui.lists.LocationOptionList;
import org.projectbuendia.client.utils.Utils;

import static org.projectbuendia.client.utils.Utils.eq;

/** A {@link DialogFragment} for updating a patient's location and bed number. */
public class PatientLocationDialogFragment extends BaseDialogFragment<PatientLocationDialogFragment, Patient> {
    private Patient patient;
    private String initialLocationUuid;
    private String initialBedNumber;

    private LocationOptionList list;
    private EditText bedNumber;

    public static PatientLocationDialogFragment create(Patient patient) {
        return new PatientLocationDialogFragment().withArgs(patient);
    }

    @Override public AlertDialog onCreateDialog(Bundle state) {
        return createAlertDialog(R.layout.patient_location_dialog_fragment);
    }

    @Override protected void onOpen() {
        patient = args;
        initialLocationUuid = patient.locationUuid;
        initialBedNumber = patient.bedNumber;

        dialog.setTitle(R.string.action_assign_location);
        dialog.getWindow().setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        LocationForest forest = App.getModel().getForest();
        list = new LocationOptionList(u.findView(R.id.list_container), true);
        list.setLocations(forest, forest.getLeaves());
        list.setSelectedLocation(forest.get(initialLocationUuid));

        bedNumber = u.findView(R.id.bed_number);
        bedNumber.setFilters(new InputFilter[] {new InputFilter.AllCaps()});
        bedNumber.setText(initialBedNumber);
        bedNumber.setSelection(bedNumber.getText().length());
        new EditTextWatcher(bedNumber).onChange(() -> {
            if (eq(list.getSelectedLocation().uuid, initialLocationUuid)) {
                initialBedNumber = bedNumber.getText().toString();
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
    }

    @Override protected void onSubmit() {
        Utils.logUserAction("location_assigned");
        ((PatientChartActivity) getActivity()).getUi().showWaitDialog(R.string.title_updating_patient);

        Location location = list.getSelectedLocation();
        String bedNumber = this.bedNumber.getText().toString().toUpperCase();
        String placement = location != null ? location.uuid + "/" + bedNumber : null;
        App.getModel().addObservationEncounter(App.getCrudEventBus(), patient.uuid, new Obs(
            null, null, patient.uuid, Utils.getProviderUuid(),
            ConceptUuids.PLACEMENT_UUID, Datatype.TEXT, DateTime.now(), null, placement, null
        ));
        dismiss();
    }
}
