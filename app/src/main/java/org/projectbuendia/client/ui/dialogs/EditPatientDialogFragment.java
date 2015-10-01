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
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.google.common.base.Optional;

import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.projectbuendia.client.App;
import org.projectbuendia.client.R;
import org.projectbuendia.client.events.CrudEventBus;
import org.projectbuendia.client.models.AppModel;
import org.projectbuendia.client.models.Patient;
import org.projectbuendia.client.models.PatientDelta;
import org.projectbuendia.client.utils.Utils;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;

/** A {@link DialogFragment} for adding or editing a patient. */
public class EditPatientDialogFragment extends DialogFragment {
    @Inject AppModel mModel;
    @Inject CrudEventBus mCrudEventBus;

    @InjectView(R.id.patient_id) EditText mId;
    @InjectView(R.id.patient_given_name) EditText mGivenName;
    @InjectView(R.id.patient_family_name) EditText mFamilyName;
    @InjectView(R.id.patient_age_years) EditText mAgeYears;
    @InjectView(R.id.patient_age_months) EditText mAgeMonths;
    @InjectView(R.id.patient_sex) RadioGroup mSex;
    @InjectView(R.id.patient_sex_female) RadioButton mSexFemale;
    @InjectView(R.id.patient_sex_male) RadioButton mSexMale;

    private LayoutInflater mInflater;
    @Nullable private ActivityUi mActivityUi;  // optional UI for showing a spinner

    /** An interface to show a spinner while the new user is being saved. */
    public interface ActivityUi {
        void showSpinner(boolean show);
    }

    /** Creates a new instance and registers the given UI, if specified. */
    public static EditPatientDialogFragment newInstance(Patient patient) {
        EditPatientDialogFragment fragment = new EditPatientDialogFragment();
        Bundle args = new Bundle();
        args.putBoolean("new", patient == null);
        if (patient != null) {
            args.putString("uuid", patient.uuid);
            args.putString("id", patient.id);
            args.putString("givenName", patient.givenName);
            args.putString("familyName", patient.familyName);
            args.putString("birthdate", Utils.toString(patient.birthdate));
            args.putInt("gender", patient.gender);
        }
        fragment.setArguments(args);
        return fragment;
    }

    public void setUi(ActivityUi activityUi) {
        mActivityUi = activityUi;
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.getInstance().inject(this);
        mInflater = LayoutInflater.from(getActivity());
    }

    private void populateFields(Bundle args) {
        mId.setText(Utils.valueOrDefault(args.getString("id"), ""));
        mGivenName.setText(Utils.valueOrDefault(args.getString("givenName"), ""));
        mFamilyName.setText(Utils.valueOrDefault(args.getString("familyName"), ""));
        LocalDate birthdate = Utils.toLocalDate(args.getString("birthdate"));
        if (birthdate != null) {
            Period age = new Period(birthdate, LocalDate.now());
            mAgeYears.setText("" + age.getYears());
            mAgeMonths.setText("" + age.getMonths());
        }
        Integer gender = args.getInt("gender");
        if (gender != null) {
            if (gender == Patient.GENDER_FEMALE) {
                mSexFemale.setChecked(true);
            } else if (gender == Patient.GENDER_MALE) {
                mSexMale.setChecked(true);
            }
        }
    }

    public void onSubmit() throws ValidationException {
        String id = Utils.toNonemptyOrNull(mId.getText().toString().trim());
        String givenName = Utils.toNonemptyOrNull(mGivenName.getText().toString().trim());
        String familyName = Utils.toNonemptyOrNull(mFamilyName.getText().toString().trim());
        String ageYears = mAgeYears.getText().toString().trim();
        String ageMonths = mAgeMonths.getText().toString().trim();
        LocalDate birthdate = null;
        LocalDate admissionDate = LocalDate.now();
        boolean valid = true;

        if (!ageYears.isEmpty() || !ageMonths.isEmpty()) {
            birthdate = LocalDate.now().minusYears(Integer.parseInt("0" + ageYears))
                .minusMonths(Integer.parseInt("0" + ageMonths));
        }

        clearErrors();
        // VALIDATION
        //OpenMRS Age limit is 120 years.
        if ((birthdate != null) && (birthdate.isBefore(LocalDate.now().minusYears(119)
            .minusMonths(11).minusDays(29)))) {
            setError(mAgeYears, R.string.age_limit);
            setError(mAgeMonths, R.string.age_limit);
            valid = false;
        }

        int sex = mSex.getCheckedRadioButtonId();
        if (sex == -1) {
            mSexFemale.setError(getResources().getString(R.string.sex_cannot_be_null));
            mSexMale.setError(getResources().getString(R.string.sex_cannot_be_null));
            showErrorMessage(getResources().getString(R.string.sex_cannot_be_null));
            valid = false;
        } else {
            switch (mSex.getCheckedRadioButtonId()) {
                case R.id.patient_sex_female:
                    sex = Patient.GENDER_FEMALE;
                    break;
                case R.id.patient_sex_male:
                    sex = Patient.GENDER_MALE;
                    break;
            }
        }

        if (!valid) {
            throw new ValidationException("Validation Error");
        }

        Utils.logUserAction("patient_submitted",
            "id", id,
            "given_name", givenName,
            "family_name", familyName,
            "age_years", ageYears,
            "age_months", ageMonths,
            "sex", "" + sex);

        PatientDelta delta = new PatientDelta();
        delta.id = Optional.fromNullable(id);
        delta.givenName = Optional.fromNullable(givenName);
        delta.familyName = Optional.fromNullable(familyName);
        delta.birthdate = Optional.fromNullable(birthdate);
        delta.gender = Optional.of(sex);
        delta.admissionDate = Optional.of(admissionDate);

        Bundle args = getArguments();
        if (args.getBoolean("new")) {
            if (id != null || givenName != null || familyName != null || birthdate != null
                || sex != Patient.GENDER_UNKNOWN) {
                mModel.addPatient(mCrudEventBus, delta);
            }
        } else {
            mModel.updatePatient(mCrudEventBus, args.getString("uuid"), delta);
        }
        // TODO: While the network request is in progress, show a spinner and/or keep the
        // dialog open but greyed out -- keep the UI blocked to make it clear that there is a
        // modal operation going on, while providing a way for the user to abort the operation.
    }

    @Override public @NonNull Dialog onCreateDialog(Bundle savedInstanceState) {
        View fragment = mInflater.inflate(R.layout.patient_dialog_fragment, null);
        ButterKnife.inject(this, fragment);

        Bundle args = getArguments();
        String title = args.getBoolean("new") ? "New patient" : "Edit patient";
        populateFields(args);

        final AlertDialog dialog = new AlertDialog.Builder(getActivity())
            .setCancelable(false) // Disable auto-cancel.
            .setTitle(title)
            .setPositiveButton(getResources().getString(R.string.ok), new DialogInterface
                .OnClickListener() {
                @Override public void onClick(DialogInterface dialogInterface, int i) {
                }
            })
            .setNegativeButton(getResources().getString(R.string.cancel), null)
            .setView(fragment)
            .create();

        //TODO: Create an encapsulated version of this
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
            public void onShow(DialogInterface arg0) {

                Button okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                okButton.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View arg0) {
                        try {
                            onSubmit();
                            dialog.dismiss();
                        } catch (ValidationException e) {
                            Utils.logEvent("add_patient_failed", "reason", "" + e.getMessage());
                        }
                    }
                });
            }
        });

        // Open the keyboard, ready to type into the given name field.
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        return dialog;
    }

    private void setError(EditText field, int resourceId) {
        field.setError(getResources().getString(resourceId));
        field.invalidate();
        field.requestFocus();
    }

    private void clearErrors(){
        mId.setError(null);
        mGivenName.setError(null);
        mFamilyName.setError(null);
        mAgeYears.setError(null);
        mAgeMonths.setError(null);
        mSexFemale.setError(null);
        mSexMale.setError(null);
    }

    private void showErrorMessage(String Message) {
        new AlertDialog.Builder(getActivity())
            .setTitle(getResources().getString(R.string.invalid_input))
            .setMessage(Message)
            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    // continue with delete
                }
            })
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show();
    }
}