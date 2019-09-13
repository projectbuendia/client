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
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.google.common.collect.ImmutableList;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.projectbuendia.client.App;
import org.projectbuendia.client.R;
import org.projectbuendia.client.json.ConceptType;
import org.projectbuendia.client.json.JsonPatient;
import org.projectbuendia.client.models.ConceptUuids;
import org.projectbuendia.client.models.Obs;
import org.projectbuendia.client.models.Patient;
import org.projectbuendia.client.models.Sex;
import org.projectbuendia.client.ui.BigToast;
import org.projectbuendia.client.ui.TextChangedWatcher;
import org.projectbuendia.client.utils.Utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.InjectView;

/** A {@link DialogFragment} for adding or editing a patient. */
public class PatientDialogFragment extends BaseDialogFragment<PatientDialogFragment> {
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

    private Patient mPatient;
    private ToggleRadioGroup<Sex> mSex;
    private boolean mAgeChanged;

    public static PatientDialogFragment create(Patient patient) {
        return new PatientDialogFragment().withArgs(Utils.bundle("patient", patient));
    }

    @Override public AlertDialog onCreateDialog(Bundle state) {
        return createAlertDialog(R.layout.patient_dialog_fragment);
    }

    @Override public void onOpen(Bundle args) {
        mPatient = (Patient) args.getSerializable("patient");

        dialog.setTitle(mPatient == null ?
            R.string.title_activity_patient_add : R.string.action_edit_patient);
        mSexFemale.setTag(Sex.FEMALE);
        mSexMale.setTag(Sex.MALE);
        mSexOther.setTag(Sex.OTHER);
        mSex = new ToggleRadioGroup<>(mSexRadioGroup);

        if (mPatient != null) {
            String idPrefix = "";
            String id = Utils.toNonnull(mPatient.id);
            Matcher matcher = ID_PATTERN.matcher(id);
            if (matcher.matches()) {
                idPrefix = matcher.group(1);
                id = matcher.group(2);
            }
            if (idPrefix.isEmpty() && id.isEmpty()) {
                idPrefix = App.getSettings().getLastIdPrefix();
            }
            mIdPrefix.setText(idPrefix);
            mId.setText(id);
            mGivenName.setText(Utils.toNonnull(mPatient.givenName));
            mFamilyName.setText(Utils.toNonnull(mPatient.familyName));
            if (mPatient.birthdate != null) {
                Period age = new Period(mPatient.birthdate, LocalDate.now());
                mAgeYears.setText(String.valueOf(age.getYears()));
                mAgeMonths.setText(String.valueOf(age.getMonths()));
            }

            mSex.setSelection(mPatient.sex);
        }

        mAgeYears.addTextChangedListener(new TextChangedWatcher(() -> mAgeChanged = true));
        mAgeMonths.addTextChangedListener(new TextChangedWatcher(() -> mAgeChanged = true));
        focusFirstEmptyField(dialog);
    }

    public void focusFirstEmptyField(AlertDialog dialog) {
        showKeyboard();

        EditText[] fields = {mIdPrefix, mId, mGivenName, mFamilyName, mAgeYears, mAgeMonths};
        for (EditText field : fields) {
            if (field.isEnabled() && field.getText().toString().isEmpty()) {
                field.requestFocus();
                return;
            }
        }

        // If all fields are populated, default to the given name field.
        mGivenName.requestFocus();
        mGivenName.setSelection(mGivenName.getText().length());
    }

    @Override public void onSubmit() {
        String idPrefix = mIdPrefix.getText().toString().trim();
        String id = Utils.toNonemptyOrNull(mId.getText().toString().trim());
        String givenName = Utils.toNonemptyOrNull(mGivenName.getText().toString().trim());
        String familyName = Utils.toNonemptyOrNull(mFamilyName.getText().toString().trim());
        Sex sex = mSex.getSelection();
        String ageYears = mAgeYears.getText().toString().trim();
        String ageMonths = mAgeMonths.getText().toString().trim();
        LocalDate birthdate = mPatient != null ? mPatient.birthdate : null;
        boolean valid = true;

        if (id == null) {
            setError(mId, R.string.patient_validation_missing_id);
            valid = false;
        }
        // Recalculate the birthdate only when the age fields have been edited;
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
            App.getSettings().setLastIdPrefix(idPrefix);
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

        if (mPatient == null) {
            BigToast.show(R.string.adding_new_patient_please_wait);
            DateTime now = DateTime.now(); // not actually used by PatientDelta
            App.getModel().addPatient(App.getCrudEventBus(), patient, ImmutableList.of(
                new Obs(null, null, now, ConceptUuids.ADMISSION_DATE_UUID,
                    ConceptType.DATE, LocalDate.now().toString(), ""),
                new Obs(null, null, now, ConceptUuids.PLACEMENT_UUID,
                    ConceptType.TEXT, App.getModel().getDefaultLocation().uuid, "")
            ));
        } else {
            BigToast.show(R.string.updating_patient_please_wait);
            patient.uuid = mPatient.uuid;
            App.getModel().updatePatient(App.getCrudEventBus(), patient);
        }
        // TODO: While the network request is in progress, show a spinner and/or keep the
        // dialog open but greyed out -- keep the UI blocked to make it clear that there is a
        // modal operation going on, while providing a way for the user to abort the operation.
    }
}
