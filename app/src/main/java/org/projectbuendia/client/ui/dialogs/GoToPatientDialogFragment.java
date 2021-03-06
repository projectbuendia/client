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
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.widget.EditText;
import android.widget.TextView;

import org.projectbuendia.client.App;
import org.projectbuendia.client.R;
import org.projectbuendia.client.events.actions.PatientChartRequestedEvent;
import org.projectbuendia.client.events.data.ItemLoadFailedEvent;
import org.projectbuendia.client.events.data.ItemLoadedEvent;
import org.projectbuendia.models.Patient;
import org.projectbuendia.client.providers.Contracts.Patients;
import org.projectbuendia.client.ui.EditTextWatcher;
import org.projectbuendia.client.utils.Utils;

import java.io.Serializable;

import de.greenrobot.event.EventBus;

import static org.projectbuendia.client.utils.ContextUtils.FormatStyle.LONG;
import static org.projectbuendia.client.utils.ContextUtils.FormatStyle.SHORT;

/** A dialog for jumping to a patient by ID. */
public class GoToPatientDialogFragment extends BaseDialogFragment<GoToPatientDialogFragment, Serializable> {
    class Views {
        EditText patientId = u.findView(R.id.go_to_patient_id);
        TextView searchResult = u.findView(R.id.go_to_patient_result);
    }

    private Views v;
    String patientUuid = null;

    @Override public AlertDialog onCreateDialog(Bundle state) {
        return createAlertDialog(R.layout.go_to_patient_dialog_fragment);
    }

    @Override protected void onOpen() {
        v = new Views();
        App.getCrudEventBus().register(this);

        dialog.setTitle(R.string.go_to_patient_title);
        new EditTextWatcher(v.patientId).onChange(this::search);
        v.searchResult.setOnClickListener(view -> onSubmit());
        dialog.getButton(BUTTON_POSITIVE).setText(R.string.go_to_chart);
    }

    @Override protected void onSubmit() {
        Utils.logUserAction("go_to_patient_submitted",
            "query_id", v.patientId.getText().toString(),
            "patient_uuid", patientUuid);
        if (patientUuid != null) {
            dialog.dismiss();

            // NOTE(ping): I don't fully understand why, but posting this on a
            // Handler is necessary to get the numeric keypad to close.  If we
            // post the event to the EventBus immediately, the numeric keypad
            // stays up even as the new activity launches underneath it!
            new Handler().postDelayed(() -> EventBus.getDefault().post(
                new PatientChartRequestedEvent(patientUuid)), 100);
        }
    }

    public void onEventMainThread(ItemLoadedEvent<?> event) {
        if (event.item instanceof Patient) {
            String id = v.patientId.getText().toString().trim();
            Patient patient = (Patient) event.item;
            if (id.equals(patient.id)) {  // server returned the patient we were looking for
                patientUuid = patient.uuid;
                v.searchResult.setText(formatSearchResult(patient));
            }
        }
    }

    public void onEventMainThread(ItemLoadFailedEvent event) {
        String id = v.patientId.getText().toString().trim();
        if (id.equals(event.id)) {  // server returned empty results for the ID we sought
            patientUuid = null;
            v.searchResult.setText(u.str(R.string.patient_not_found, event.id));
        }
    }

    private String formatSearchResult(Patient patient) {
        String details = u.formatPatientDetails(patient, SHORT, LONG, LONG);
        return Utils.format("%s (%s)", u.formatPatientName(patient),
            Utils.nonemptyOrDefault(details, u.str(R.string.unknown)));
    }

    private void search() {
        String id = v.patientId.getText().toString().trim();
        if (id.isEmpty()) {
            patientUuid = null;
            v.searchResult.setText("");
        } else {
            if (id.matches("^\\d+$")) {
                String prefix = Utils.nonemptyOrDefault(App.getSettings().getLastIdPrefix(), "BN"); // TODO(ping): Remove this Bunia-specific hack.
                id = prefix + "/" + id;
            }
            try (Cursor cursor = getActivity().getContentResolver().query(
                Patients.URI, null, Patients.ID + " = ?", new String[] {id}, null)) {
                if (cursor.moveToNext()) {  // found locally
                    Patient patient = Patient.load(cursor);
                    patientUuid = patient.uuid;
                    v.searchResult.setText(formatSearchResult(patient));
                } else {  // not found locally; check server
                    patientUuid = null;
                    v.searchResult.setText(R.string.searching_ellipsis);
                    App.getModel().fetchPatient(App.getCrudEventBus(), id);
                }
            }
        }
        dialog.getButton(BUTTON_POSITIVE).setEnabled(patientUuid != null);
    }
}
