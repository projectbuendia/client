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
import android.os.Handler;
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
import org.projectbuendia.client.events.data.ItemLoadFailedEvent;
import org.projectbuendia.client.events.data.ItemLoadedEvent;
import org.projectbuendia.client.models.AppModel;
import org.projectbuendia.client.models.Patient;
import org.projectbuendia.client.providers.Contracts.Patients;
import org.projectbuendia.client.sync.SyncManager;
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
    @Inject SyncManager mSyncManager;
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
        final View fragment = mInflater.inflate(R.layout.go_to_patient_dialog_fragment, null);
        ButterKnife.inject(this, fragment);
        mPatientId.addTextChangedListener(new IdWatcher());
        mPatientSearchResult.setOnClickListener(view -> onSubmit());
        return new AlertDialog.Builder(getActivity())
            .setTitle(R.string.go_to_patient_title)
            .setPositiveButton(R.string.go_to_patient_go, (dialogInterface, i) -> onSubmit())
            .setNegativeButton(R.string.cancel, null)
            .setView(fragment)
            .create();
    }

    public void onSubmit() {
        Utils.logUserAction("go_to_patient_submitted",
            "patient_id", mPatientId.getText().toString(),
            "patient_uuid", mPatientUuid);
        if (mPatientUuid != null) {
            getDialog().dismiss();

            // NOTE(ping): I don't fully understand why, but posting this on a
            // Handler is necessary to get the numeric keypad to close.  If we
            // post the event to the EventBus immediately, the numeric keypad
            // stays up even as the new activity launches underneath it!
            new Handler().postDelayed(() -> EventBus.getDefault().post(
                new PatientChartRequestedEvent(mPatientUuid)), 100);
        }
    }

    public void onEventMainThread(ItemLoadedEvent<?> event) {
        if (event.item instanceof Patient) {
            String id = mPatientId.getText().toString().trim();
            Patient patient = (Patient) event.item;
            if (id.equals(patient.id)) {  // server returned the patient we were looking for
                mPatientUuid = patient.uuid;
                mPatientSearchResult.setText(patient.givenName + " " + patient.familyName +
                    " (" + patient.sex + ", " + Utils.birthdateToAge(
                    patient.birthdate, App.getInstance().getResources()) + ")");
            }
        }
    }

    public void onEventMainThread(ItemLoadFailedEvent event) {
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
                try (Cursor cursor = getActivity().getContentResolver().query(
                    Patients.URI, null, Patients.ID + " = ?", new String[] {id}, null)) {
                    if (cursor.moveToNext()) {
                        String uuid = Utils.getString(cursor, Patients.UUID, null);
                        String givenName = Utils.getString(cursor, Patients.GIVEN_NAME, "");
                        String familyName = Utils.getString(cursor, Patients.FAMILY_NAME, "");
                        LocalDate birthdate = Utils.getLocalDate(cursor, Patients.BIRTHDATE);
                        String age = birthdate != null ?
                            Utils.birthdateToAge(birthdate, getResources()) : "age unknown";
                        String sex = Utils.getString(cursor, Patients.SEX, "");
                        mPatientUuid = uuid;
                        mPatientSearchResult.setText(givenName + " " + familyName +
                            " (" + sex + ", " + age + ")");
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
                        mAppModel.fetchSinglePatient(mBus, id);
                    }
                }
            }
            ((AlertDialog) getDialog()).getButton(DialogInterface.BUTTON_POSITIVE)
                .setEnabled(mPatientUuid != null);
        }
    }
}
