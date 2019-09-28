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
import org.projectbuendia.client.json.Datatype;
import org.projectbuendia.client.json.JsonObservation;
import org.projectbuendia.client.json.JsonPatient;
import org.projectbuendia.client.models.ConceptUuids;
import org.projectbuendia.client.models.Obs;
import org.projectbuendia.client.models.Patient;
import org.projectbuendia.client.models.Sex;
import org.projectbuendia.client.ui.BigToast;
import org.projectbuendia.client.ui.EditTextWatcher;
import org.projectbuendia.client.utils.Utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A {@link DialogFragment} for adding or editing a patient. */
public class PatientDialogFragment extends BaseDialogFragment<PatientDialogFragment, Patient> {
    private static final Pattern ID_PATTERN = Pattern.compile("([a-zA-Z]+)/?([0-9]+)*");

    class Views {
        EditText idPrefix = u.findView(R.id.patient_id_prefix);
        EditText id = u.findView(R.id.patient_id);
        EditText givenName = u.findView(R.id.patient_given_name);
        EditText familyName = u.findView(R.id.patient_family_name);
        EditText ageYears = u.findView(R.id.patient_age_years);
        EditText ageMonths = u.findView(R.id.patient_age_months);
        RadioGroup sexRadioGroup = u.findView(R.id.patient_sex);
        RadioButton sexFemale = u.findView(R.id.patient_sex_female);
        RadioButton sexMale = u.findView(R.id.patient_sex_male);
        RadioButton sexOther = u.findView(R.id.patient_sex_other);
    }

    private Views v;
    private Patient patient;
    private ToggleRadioGroup<Sex> sexToggleGroup;
    private boolean ageChanged;

    public static PatientDialogFragment create(Patient patient) {
        return new PatientDialogFragment().withArgs(patient);
    }

    @Override public AlertDialog onCreateDialog(Bundle state) {
        return createAlertDialog(R.layout.patient_dialog_fragment);
    }

    @Override public void onOpen() {
        v = new Views();
        patient = args;

        dialog.setTitle(patient == null ?
            R.string.title_activity_patient_add : R.string.action_edit_patient);
        v.sexFemale.setTag(Sex.FEMALE);
        v.sexMale.setTag(Sex.MALE);
        v.sexOther.setTag(Sex.OTHER);
        sexToggleGroup = new ToggleRadioGroup<>(v.sexRadioGroup);

        if (patient != null) {
            String idPrefix = "";
            String id = Utils.toNonnull(patient.id);
            Matcher matcher = ID_PATTERN.matcher(id);
            if (matcher.matches()) {
                idPrefix = matcher.group(1);
                id = matcher.group(2);
            }
            if (idPrefix.isEmpty() && id.isEmpty()) {
                idPrefix = App.getSettings().getLastIdPrefix();
            }
            v.idPrefix.setText(idPrefix);
            v.id.setText(id);
            v.givenName.setText(Utils.toNonnull(patient.givenName));
            v.familyName.setText(Utils.toNonnull(patient.familyName));
            if (patient.birthdate != null) {
                Period age = new Period(patient.birthdate, LocalDate.now());
                v.ageYears.setText(String.valueOf(age.getYears()));
                v.ageMonths.setText(String.valueOf(age.getMonths()));
            }

            sexToggleGroup.setSelection(patient.sex);
        }

        new EditTextWatcher(v.ageYears, v.ageMonths).onChange(() -> ageChanged = true);
        // TODO(ping): Figure out why the keyboard doesn't appear and focus
        // isn't placed on the first field.
        // TODO(ping): Make the name fields auto-capitalize the first letter.
        Utils.showKeyboard(dialog.getWindow());
        Utils.focusFirstEmptyField(v.idPrefix, v.id, v.givenName,
            v.familyName, v.ageYears, v.ageMonths);
    }

    @Override public void onSubmit() {
        String idPrefix = v.idPrefix.getText().toString().trim();
        String id = Utils.toNonemptyOrNull(v.id.getText().toString().trim());
        String givenName = Utils.toNonemptyOrNull(v.givenName.getText().toString().trim());
        String familyName = Utils.toNonemptyOrNull(v.familyName.getText().toString().trim());
        Sex sex = sexToggleGroup.getSelection();
        String ageYears = v.ageYears.getText().toString().trim();
        String ageMonths = v.ageMonths.getText().toString().trim();
        LocalDate birthdate = patient != null ? patient.birthdate : null;
        boolean valid = true;

        if (id == null) {
            setError(v.id, R.string.patient_validation_missing_id);
            valid = false;
        }
        // Recalculate the birthdate only when the age fields have been edited;
        // otherwise, simply opening and submitting the dialog would shift the
        // birthdate every time to make the age a whole number of months.
        if (ageChanged) {
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
            setError(v.ageYears, R.string.patient_validation_age_limit);
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

        JsonPatient newPatient = new JsonPatient();
        newPatient.id = id;
        newPatient.given_name = givenName;
        newPatient.family_name = familyName;
        newPatient.birthdate = birthdate;
        newPatient.sex = sex;

        if (patient == null) {
            BigToast.show(R.string.adding_new_patient_please_wait);
            DateTime now = DateTime.now(); // not actually used by PatientDelta
            newPatient.observations = ImmutableList.of(
                new JsonObservation(new Obs(
                    null, null, null, Utils.getProviderUuid(),
                    ConceptUuids.ADMISSION_DATE_UUID, Datatype.DATE,
                    now, null, LocalDate.now().toString(), ""
                )),
                new JsonObservation(new Obs(
                    null, null, null, Utils.getProviderUuid(),
                    ConceptUuids.PLACEMENT_UUID, Datatype.TEXT,
                    now, null, App.getModel().getDefaultLocation().uuid, ""
                ))
            );
            App.getModel().addPatient(App.getCrudEventBus(), newPatient);
        } else {
            BigToast.show(R.string.updating_patient_please_wait);
            newPatient.uuid = patient.uuid;
            App.getModel().updatePatient(App.getCrudEventBus(), newPatient);
        }
        // TODO: While the network request is in progress, show a spinner and/or keep the
        // dialog open but greyed out -- keep the UI blocked to make it clear that there is a
        // modal operation going on, while providing a way for the user to abort the operation.
    }
}
