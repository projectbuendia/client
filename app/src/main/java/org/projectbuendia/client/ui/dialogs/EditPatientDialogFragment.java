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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.google.common.base.Optional;

import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.projectbuendia.client.App;
import org.projectbuendia.client.AppSettings;
import org.projectbuendia.client.R;
import org.projectbuendia.client.events.CrudEventBus;
import org.projectbuendia.client.models.AppModel;
import org.projectbuendia.client.models.LocationForest;
import org.projectbuendia.client.models.Patient;
import org.projectbuendia.client.models.PatientDelta;
import org.projectbuendia.client.utils.Utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;

/** A {@link DialogFragment} for adding or editing a patient. */
public class EditPatientDialogFragment extends DialogFragment {
    @Inject AppModel mModel;
    @Inject AppSettings mSettings;
    @Inject CrudEventBus mCrudEventBus;

    @InjectView(R.id.patient_id_prefix) EditText mIdPrefix;
    @InjectView(R.id.patient_id) EditText mId;
    @InjectView(R.id.patient_given_name) EditText mGivenName;
    @InjectView(R.id.patient_family_name) EditText mFamilyName;
    @InjectView(R.id.patient_age_years) EditText mAgeYears;
    @InjectView(R.id.patient_age_months) EditText mAgeMonths;
    @InjectView(R.id.patient_sex) RadioGroup mSex;
    @InjectView(R.id.patient_sex_female) RadioButton mSexFemale;
    @InjectView(R.id.patient_sex_male) RadioButton mSexMale;

    private static final Pattern ID_PATTERN = Pattern.compile("([a-zA-Z]+)/?([0-9]+)*");

    private LayoutInflater mInflater;

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
            args.putString("birthdate", Utils.formatDate(patient.birthdate));
            args.putInt("gender", patient.gender);
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.getInstance().inject(this);
        mInflater = LayoutInflater.from(getActivity());
    }

    private void populateFields(Bundle args) {
        String idPrefix = "";
        String id = Utils.toNonnull(args.getString("id"));
        Matcher matcher = ID_PATTERN.matcher(id);
        if (matcher.matches()) {
            idPrefix = matcher.group(1);
            id = matcher.group(2);
        }
        if (idPrefix.isEmpty() && id.isEmpty()) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
            idPrefix = pref.getString("last_id_prefix", "");
        }
        mIdPrefix.setText(idPrefix);
        mId.setText(id);
        mGivenName.setText(Utils.toNonnull(args.getString("givenName")));
        mFamilyName.setText(Utils.toNonnull(args.getString("familyName")));
        LocalDate birthdate = Utils.toLocalDate(args.getString("birthdate"));
        if (birthdate != null) {
            Period age = new Period(birthdate, LocalDate.now());
            mAgeYears.setText(String.valueOf(age.getYears()));
            mAgeMonths.setText(String.valueOf(age.getMonths()));
        }
        switch (args.getInt("gender", Patient.GENDER_UNKNOWN)) {
            case Patient.GENDER_FEMALE:
                mSexFemale.setChecked(true);
                break;
            case Patient.GENDER_MALE:
                mSexMale.setChecked(true);
                break;
        }
    }

    public void onSubmit() {
        String idPrefix = mIdPrefix.getText().toString().trim();
        String id = mId.getText().toString().trim();
        String givenName = Utils.toNonemptyOrNull(mGivenName.getText().toString().trim());
        String familyName = Utils.toNonemptyOrNull(mFamilyName.getText().toString().trim());
        String ageYears = mAgeYears.getText().toString().trim();
        String ageMonths = mAgeMonths.getText().toString().trim();
        LocalDate birthdate = null;
        LocalDate admissionDate = LocalDate.now();

        if (!ageYears.isEmpty() || !ageMonths.isEmpty()) {
            birthdate = LocalDate.now().minusYears(Integer.parseInt("0" + ageYears))
                .minusMonths(Integer.parseInt("0" + ageMonths));
        }
        // TODO: This should start out as "Sex sex = null" and then get set to female, male,
        // other, or unknown if any button is selected (there should be four buttons) -- so
        // that we can distinguish "no change to sex" (null) from "change sex to unknown" ("U").
        int sex = Patient.GENDER_UNKNOWN;
        switch (mSex.getCheckedRadioButtonId()) {
            case R.id.patient_sex_female:
                sex = Patient.GENDER_FEMALE;
                break;
            case R.id.patient_sex_male:
                sex = Patient.GENDER_MALE;
                break;
        }

        id = idPrefix.isEmpty() ? id : idPrefix + "/" + id;
        Utils.logUserAction("patient_submitted",
            "id", id,
            "given_name", givenName,
            "family_name", familyName,
            "age_years", ageYears,
            "age_months", ageMonths,
            "sex", "" + sex);

        PatientDelta delta = new PatientDelta();
        delta.id = Optional.of(id);
        delta.givenName = Optional.fromNullable(givenName);
        delta.familyName = Optional.fromNullable(familyName);
        delta.birthdate = Optional.fromNullable(birthdate);
        delta.gender = Optional.of(sex);
        delta.admissionDate = Optional.of(admissionDate);

        if (!idPrefix.isEmpty()) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
            pref.edit().putString("last_id_prefix", idPrefix).commit();
        }

        Bundle args = getArguments();
        if (args.getBoolean("new")) {
            if (id != null || givenName != null || familyName != null || birthdate != null
                || sex != Patient.GENDER_UNKNOWN) {
                LocationForest forest = mModel.getForest(mSettings.getLocaleTag());
                delta.assignedLocationUuid = Optional.of(forest.getDefaultLocation().uuid);
                mModel.addPatient(mCrudEventBus, delta);
            }
        } else {
            mModel.updatePatient(mCrudEventBus, args.getString("uuid"), delta);
        }
        // TODO: While the network request is in progress, show a spinner and/or keep the
        // dialog open but greyed out -- keep the UI blocked to make it clear that there is a
        // modal operation going on, while providing a way for the user to abort the operation.
    }

    public void focusFirstEmptyField(Dialog dialog) {
        // Open the keyboard.
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        // Set focus.
        EditText[] fields = {mIdPrefix, mId, mGivenName, mFamilyName, mAgeYears, mAgeMonths};
        for (EditText field : fields) {
            if (field.getText().toString().isEmpty()) {
                field.requestFocus();
                break;
            }
        }

        // Default to focusing on the given name field.
        mGivenName.requestFocus();
    }

    @Override public @NonNull Dialog onCreateDialog(Bundle savedInstanceState) {
        View fragment = mInflater.inflate(R.layout.patient_dialog_fragment, null);
        ButterKnife.inject(this, fragment);

        Bundle args = getArguments();
        String title = args.getBoolean("new") ? getString(R.string.title_activity_patient_add) : getString(R.string.action_edit_patient);
        populateFields(args);

        AlertDialog dialog = new AlertDialog.Builder(getActivity())
            .setCancelable(false) // Disable auto-cancel.
            .setTitle(title)
            .setPositiveButton(getResources().getString(R.string.ok), (dialogInterface, i) -> onSubmit())
            .setNegativeButton(getResources().getString(R.string.cancel), null)
            .setView(fragment)
            .create();

        focusFirstEmptyField(dialog);
        return dialog;
    }
}
