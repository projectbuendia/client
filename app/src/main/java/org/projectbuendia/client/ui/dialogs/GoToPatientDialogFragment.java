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
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.projectbuendia.client.App;
import org.projectbuendia.client.R;
import org.projectbuendia.client.events.CrudEventBus;
import org.projectbuendia.client.events.actions.PatientChartRequestedEvent;
import org.projectbuendia.client.events.data.ItemFetchFailedEvent;
import org.projectbuendia.client.events.data.ItemFetchedEvent;
import org.projectbuendia.client.models.AppModel;
import org.projectbuendia.client.models.Patient;
import org.projectbuendia.client.sync.SyncAccountService;
import org.projectbuendia.client.providers.Contracts.Patients;
import org.projectbuendia.client.utils.RelativeDateTimeFormatter;
import org.projectbuendia.client.utils.Utils;

import javax.inject.Inject;
import javax.inject.Provider;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;

/** A dialog for jumping to a patient by ID. */
public class GoToPatientDialogFragment extends DialogFragment {
    @Inject AppModel mAppModel;
    @Inject Provider<CrudEventBus> mCrudEventBusProvider;
    @InjectView(R.id.go_to_patient_id) EditText mPatientId;
    @InjectView(R.id.go_to_patient_result) TextView mPatientSearchResult;
    private LayoutInflater mInflater;
    String mPatientUuid;
    CrudEventBus mBus;

    public static GoToPatientDialogFragment newInstance() {
        return new GoToPatientDialogFragment();
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.getInstance().inject(this);
        mInflater = LayoutInflater.from(getActivity());
        mBus = mCrudEventBusProvider.get();
        mBus.register(this);

    }

    @Override public @NonNull Dialog onCreateDialog(Bundle savedInstanceState) {
        View fragment = mInflater.inflate(R.layout.go_to_patient_dialog_fragment, null);
        ButterKnife.inject(this, fragment);
        mPatientId.addTextChangedListener(new IdWatcher());
        mPatientSearchResult.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                onSubmit();
            }
        });
        return new AlertDialog.Builder(getActivity())
            .setTitle(R.string.go_to_patient_title)
            .setPositiveButton(R.string.go_to_patient_go, new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface dialogInterface, int i) {
                    onSubmit();
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .setView(fragment)
            .create();
    }

    public void onSubmit() {
        Utils.logUserAction("go_to_patient_submitted",
            "patient_id", mPatientId.getText().toString(),
            "patient_uuid", mPatientUuid);
        if (mPatientUuid != null) {
            EventBus.getDefault().post(new PatientChartRequestedEvent(mPatientUuid));
            getDialog().dismiss();
        }
    }

    public void onEventMainThread(ItemFetchedEvent<Patient> event) {
        String id = mPatientId.getText().toString().trim();
        Patient patient = event.item;
        if (id.equals(patient.id)) {  // server returned the patient we were looking for
            mPatientUuid = patient.uuid;
            mPatientSearchResult.setText(patient.givenName + " " + patient.familyName +
                " (" + patient.gender + ", " + Utils.birthdateToAge(
                patient.birthdate, getResources()) + ")");

            // Perform incremental observation sync to get the patient's admission date
            // and any other recent observations.
            SyncAccountService.startObservationsAndOrdersSync();
        }
    }

    public void onEventMainThread(ItemFetchFailedEvent event) {
        String id = mPatientId.getText().toString().trim();
        if (id.equals(event.id)) {  // server returned empty results for the ID we sought
            mPatientUuid = null;
            mPatientSearchResult.setText(getResources().getString(R.string.go_to_patient_not_found));
        }
    }

    class IdWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence c, int x, int y, int z) {
        }

        @Override public void onTextChanged(CharSequence c, int x, int y, int z) {
        }

        @Override public void afterTextChanged(Editable editable) {
            String id = mPatientId.getText().toString().trim();
            if (id.isEmpty()) {
                mPatientUuid = null;
                mPatientSearchResult.setText("");
            } else {
                try (Cursor cursor = getActivity().getContentResolver().query(Patients.CONTENT_URI,
                    null, Patients.ID + " LIKE ?", new String[] {"%" + id + "%"}, null)) {
                    if (cursor.moveToNext()) {
                        String uuid = Utils.getString(cursor, Patients.UUID, null);
                        String givenName = Utils.getString(cursor, Patients.GIVEN_NAME, "");
                        String familyName = Utils.getString(cursor, Patients.FAMILY_NAME, "");
                        LocalDate birthdate = Utils.getLocalDate(cursor, Patients.BIRTHDATE);
                        String age = birthdate == null
                                ? getResources().getString(R.string.age_unknown)
                                : Utils.birthdateToAge(birthdate, getResources());
                        String gender = Utils.getString(cursor, Patients.GENDER, "");
                        mPatientUuid = uuid;
                        mPatientSearchResult.setText(givenName + " " + familyName +
                            " (" + gender + ", " + age + ")");
                    } else {
                        String message = getResources().getString(R.string.go_to_patient_no_data);
                        DateTime lastSyncTime = mAppModel.getLastFullSyncTime();
                        if (lastSyncTime != null) {
                            message = getResources().getString(
                                R.string.go_to_patient_not_found_as_of_time,
                                new RelativeDateTimeFormatter().format(lastSyncTime));
                        }
                        mPatientUuid = null;
                        mPatientSearchResult.setText(message);

                        // Immediately check for this patient on the server.
                        mAppModel.downloadSinglePatient(mBus, id);
                    }
                }
            }
            ((AlertDialog) getDialog()).getButton(DialogInterface.BUTTON_POSITIVE)
                .setEnabled(mPatientUuid != null);
        }
    }
}
