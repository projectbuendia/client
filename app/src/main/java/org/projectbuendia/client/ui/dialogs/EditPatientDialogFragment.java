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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.google.common.collect.ImmutableList;

import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.projectbuendia.client.App;
import org.projectbuendia.client.AppSettings;
import org.projectbuendia.client.R;
import org.projectbuendia.client.events.CrudEventBus;
import org.projectbuendia.client.json.ConceptType;
import org.projectbuendia.client.json.JsonPatient;
import org.projectbuendia.client.models.AppModel;
import org.projectbuendia.client.models.ConceptUuids;
import org.projectbuendia.client.models.Obs;
import org.projectbuendia.client.models.Patient;
import org.projectbuendia.client.models.Sex;
import org.projectbuendia.client.ui.BigToast;
import org.projectbuendia.client.ui.TextChangedWatcher;
import org.projectbuendia.client.utils.Utils;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
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
    @InjectView(R.id.patient_sex) RadioGroup mSexRadioGroup;
    @InjectView(R.id.patient_sex_female) RadioButton mSexFemale;
    @InjectView(R.id.patient_sex_male) RadioButton mSexMale;
    @InjectView(R.id.patient_sex_other) RadioButton mSexOther;

    private static final Pattern ID_PATTERN = Pattern.compile("([a-zA-Z]+)/?([0-9]+)*");

    private LayoutInflater mInflater;
    private ToggleRadioGroup<Sex> mSex;
    private boolean mAgeChanged;
    private LocalDate mBirthdate;

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
            args.putString("birthdate", Utils.format(patient.birthdate));
            args.putString("sex", Sex.nullableNameOf(patient.sex));
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.inject(this);
        mInflater = LayoutInflater.from(getActivity());
    }

    @Override public @Nonnull Dialog onCreateDialog(Bundle savedInstanceState) {
        View fragment = mInflater.inflate(R.layout.patient_dialog_fragment, null);
        ButterKnife.inject(this, fragment);

        Bundle args = getArguments();
        String title = args.getBoolean("new") ?
            getString(R.string.title_activity_patient_add) :
            getString(R.string.action_edit_patient);
        populateFields(args);

        AlertDialog dialog = new AlertDialog.Builder(getActivity())
            .setCancelable(false) // Disable auto-cancel.
            .setTitle(title)
            .setPositiveButton(getString(R.string.ok), null)
            .setNegativeButton(getString(R.string.cancel), null)
            .setView(fragment)
            .create();

        // To prevent the dialog from being automatically dismissed, we have to
        // override the listener instead of passing it in to setPositiveButton.
        dialog.setOnShowListener(di ->
            dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                .setOnClickListener(view -> onSubmit(dialog))
        );

        focusFirstEmptyField(dialog);
        return dialog;
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

        Sex sex = Sex.nullableValueOf(args.getString("sex"));
        mSexFemale.setTag(Sex.FEMALE);
        mSexMale.setTag(Sex.MALE);
        mSexOther.setTag(Sex.OTHER);
        mSex = new ToggleRadioGroup<>(mSexRadioGroup);
        mSex.setSelection(sex);

        mBirthdate = birthdate;
        mAgeYears.addTextChangedListener(new TextChangedWatcher(() -> mAgeChanged = true));
        mAgeMonths.addTextChangedListener(new TextChangedWatcher(() -> mAgeChanged = true));
    }

    public void onSubmit(DialogInterface dialog) {
        String idPrefix = mIdPrefix.getText().toString().trim();
        String id = Utils.toNonemptyOrNull(mId.getText().toString().trim());
        String givenName = Utils.toNonemptyOrNull(mGivenName.getText().toString().trim());
        String familyName = Utils.toNonemptyOrNull(mFamilyName.getText().toString().trim());
        Sex sex = mSex.getSelection();
        String ageYears = mAgeYears.getText().toString().trim();
        String ageMonths = mAgeMonths.getText().toString().trim();
        LocalDate birthdate = mBirthdate;
        boolean valid = true;

        if (id == null) {
            setError(mId, R.string.patient_validation_missing_id);
            valid = false;
        }
        // We should only save/update the age when the fields have been edited;
        // otherwise, simply opening and submitting the dialog would shift the
        // birthdate every time to make the age a whole number of months.
        if (mAgeChanged) {
            if (ageYears.isEmpty() && ageMonths.isEmpty()) {
                // The user can clear the birthdate information by clearing the fields.
                birthdate = null;
            } else {
                int years = Integer.parseInt("0" + ageYears);
                int months = Integer.parseInt("0" + ageMonths);
                birthdate = LocalDate.now().minusYears(years).minusMonths(months);

                // Pick a birthdate just one day earlier than necessary to put the age
                // at the desired number of years and months, so that the same age
                // appears on all tablets regardless of timezone.  Showing different
                // ages in different timezones isn't truly avoidable because ages are
                // stored as local dates (not timestamps), but the extra day will at
                // least prevent some initial confusion.  After all, the entered age
                // is only precise to the month; the confusion comes from emphasizing
                // precision that isn't there.
                birthdate = birthdate.minusDays(1);
            }
        }
        if (birthdate != null && new Period(
            birthdate, LocalDate.now().plusDays(1)).getYears() >= 120) {
            setError(mAgeYears, R.string.patient_validation_age_limit);
            valid = false;
        }
        if (!valid) return;

        if (!idPrefix.isEmpty()) {
            id = idPrefix + "/" + id;
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
            pref.edit().putString("last_id_prefix", idPrefix).commit();
        }

        Utils.logUserAction("patient_submitted",
            "id", id,
            "given_name", givenName,
            "family_name", familyName,
            "age_years", ageYears,
            "age_months", ageMonths,
            "sex", "" + sex);
        dialog.dismiss();

        JsonPatient patient = new JsonPatient();
        patient.id = id;
        patient.given_name = givenName;
        patient.family_name = familyName;
        patient.birthdate = birthdate;
        patient.sex = sex;

        List<Obs> observations = null;
        Bundle args = getArguments();
        if (args.getBoolean("new")) {
            BigToast.show(R.string.adding_new_patient_please_wait);

            long now = System.currentTimeMillis(); // not actually used by PatientDelta
            observations = ImmutableList.of(
                new Obs(null, now, ConceptUuids.ADMISSION_DATE_UUID,
                    ConceptType.DATE, LocalDate.now().toString(), ""),
                new Obs(null, now, ConceptUuids.PLACEMENT_UUID,
                    ConceptType.TEXT, mModel.getDefaultLocation().uuid, "")
            );
            mModel.addPatient(mCrudEventBus, patient, observations);
        } else {
            BigToast.show(R.string.updating_patient_please_wait);
            patient.uuid = args.getString("uuid");
            mModel.updatePatient(mCrudEventBus, patient);
        }
        // TODO: While the network request is in progress, show a spinner and/or keep the
        // dialog open but greyed out -- keep the UI blocked to make it clear that there is a
        // modal operation going on, while providing a way for the user to abort the operation.
    }

    private void setError(EditText field, int resourceId, Object... args) {
        String message = getString(resourceId, args);
        field.setError(message);
        field.invalidate();
        field.requestFocus();
    }

    public void focusFirstEmptyField(Dialog dialog) {
        // Open the keyboard.
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        // Set focus.
        EditText[] fields = {mIdPrefix, mId, mGivenName, mFamilyName, mAgeYears, mAgeMonths};
        for (EditText field : fields) {
            if (field.isEnabled() && field.getText().toString().isEmpty()) {
                field.requestFocus();
                return;
            }
        }

        // If all fields are populated, default to the end of the given name field.
        mGivenName.requestFocus();
        mGivenName.setSelection(mGivenName.getText().length());
    }
}
