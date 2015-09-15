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

package org.projectbuendia.client.ui.newpatient;

import org.projectbuendia.client.R;
import org.projectbuendia.client.ui.FunctionalTestCase;

import java.util.Date;

import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

/** Tests for {@link NewPatientActivity}. */
public class NewPatientActivityTest extends FunctionalTestCase {

    /** Tests adding a new patient with a location. */
    public void testNewPatientWithLocation() {
        inUserLoginGoToPatientCreation();
        screenshot("Test Start");
        String id = Long.toString(new Date().getTime()%100000);
        populateNewPatientFieldsExceptLocation(id);
        scrollToAndClick(viewWithId(R.id.patient_creation_button_change_location));
        screenshot("After Location Dialog Shown");
        click(viewWithText("S1"));
        screenshot("After Location Selected");
        click(viewWithText("Create"));
        screenshot("After Create Pressed");

        waitForProgressFragment();

        // The new patient should be visible in the list for tent S1
        click(viewWithText("S1"));
        screenshot("In S1");
        inPatientListClickPatientWithId(id);
        screenshot("After Patient Clicked");
    }

    /** Populates all the fields on the New Patient screen, except location. */
    private void populateNewPatientFieldsExceptLocation(String id) {
        screenshot("Before Patient Populated");
        String given = "Given" + id;
        String family = "Family" + id;
        type(id, viewWithId(R.id.patient_creation_text_patient_id));
        type(given, viewWithId(R.id.patient_creation_text_patient_given_name));
        type(family, viewWithId(R.id.patient_creation_text_patient_family_name));
        type(id.substring(id.length() - 2), viewWithId(R.id.patient_creation_age_years));
        type(id.substring(id.length() - 2), viewWithId(R.id.patient_creation_age_months));
        click(viewWithId(R.id.patient_creation_radiogroup_age_sex_male));
        click(viewWithId(R.id.patient_creation_radiogroup_age_sex_female));
        screenshot("After Patient Populated");
    }

    /** Tests adding a new patient with no location. */
    public void testNewPatientWithoutLocation() {
        inUserLoginGoToPatientCreation();
        screenshot("Test Start");
        String id = Long.toString(new Date().getTime()%100000);
        populateNewPatientFieldsExceptLocation(id);
        screenshot("After Patient Populated");
        click(viewWithText("Create"));
        screenshot("After Create Pressed");

        waitForProgressFragment();

        // The new patient should be visible in the list for Triage zone.
        click(viewWithText("Triage"));
        screenshot("In Triage");
        inPatientListClickPatientWithId(id);
        screenshot("After Patient Clicked");

        // The admission date should be visible right after adding a patient.
        // Flaky because of potential periodic syncs.
        expectVisibleWithin(90000, viewThat(
            hasAncestorThat(withId(R.id.attribute_admission_days)),
            hasText("Day 1")));

        // The symptom onset date should not be assigned a default value.
        expectVisible(viewThat(
            hasAncestorThat(withId(R.id.attribute_symptoms_onset_days)),
            hasText("–")));
    }

    /** Tests that a confirmation prompt appears upon cancelling the form. */
    public void testNewPatientCancel() {
        inUserLoginGoToPatientCreation();
        screenshot("Test Start");
        type("xyz", viewWithId(R.id.patient_creation_text_patient_id));
        screenshot("After Id Added");
        pressBack(); // close the keyboard
        screenshot("After Keyboard Closed");

        // Attempting to back out of the activity should trigger a prompt
        pressBack();
        expectVisible(viewThat(hasTextContaining("Discard")));
        screenshot("Discard Prompt");

        // Dismiss the prompt
        click(viewWithText("Yes"));
        screenshot("Discard Prompt Dismissed");
    }
}
