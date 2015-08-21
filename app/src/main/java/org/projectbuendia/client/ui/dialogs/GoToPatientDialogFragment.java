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

import org.joda.time.LocalDate;
import org.projectbuendia.client.R;
import org.projectbuendia.client.events.actions.PatientChartRequestedEvent;
import org.projectbuendia.client.sync.providers.Contracts.Patients;
import org.projectbuendia.client.utils.Utils;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;

/** A dialog for jumping to a patient by ID. */
public class GoToPatientDialogFragment extends DialogFragment {
    public static GoToPatientDialogFragment newInstance() {
        return new GoToPatientDialogFragment();
    }

    @InjectView(R.id.go_to_patient_id) EditText mPatientId;
    @InjectView(R.id.go_to_patient_result) TextView mPatientSearchResult;

    private LayoutInflater mInflater;
    String mPatientUuid;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mInflater = LayoutInflater.from(getActivity());
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

    @Override
    public @NonNull Dialog onCreateDialog(Bundle savedInstanceState) {
        View fragment = mInflater.inflate(R.layout.go_to_patient_dialog_fragment, null);
        ButterKnife.inject(this, fragment);
        mPatientId.addTextChangedListener(new IdWatcher());
        mPatientSearchResult.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onSubmit();
            }
        });
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.go_to_patient_title)
                .setPositiveButton(R.string.go_to_patient_go, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        onSubmit();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .setView(fragment)
                .create();
    }

    class IdWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence c, int x, int y, int z) {}
        @Override public void onTextChanged(CharSequence c, int x, int y, int z) {}

        @Override
        public void afterTextChanged(Editable editable) {
            String id = mPatientId.getText().toString().trim();
            if (id.isEmpty()) {
                mPatientUuid = null;
                mPatientSearchResult.setText("");
            } else {
                Cursor cursor = getActivity().getContentResolver().query(
                        Patients.CONTENT_URI, null, "_id = ?", new String[]{id}, null);
                try {
                    if (cursor.moveToNext()) {
                        String uuid = Utils.getString(cursor, Patients.UUID, null);
                        String givenName = Utils.getString(cursor, Patients.GIVEN_NAME, "");
                        String familyName = Utils.getString(cursor, Patients.FAMILY_NAME, "");
                        LocalDate birthdate = Utils.getLocalDate(cursor, Patients.BIRTHDATE);
                        String gender = Utils.getString(cursor, Patients.GENDER, "");
                        mPatientUuid = uuid;
                        mPatientSearchResult.setText(givenName + " " + familyName +
                                " (" + gender + ", " + Utils.birthdateToAge(birthdate) + ")");
                    } else {
                        mPatientUuid = null;
                        mPatientSearchResult.setText(getResources().getString(R.string.go_to_patient_not_found));
                    }
                } finally {
                    cursor.close();
                }
            }
            ((AlertDialog) getDialog()).getButton(DialogInterface.BUTTON_POSITIVE)
                    .setEnabled(mPatientUuid != null);
        }
    }
}