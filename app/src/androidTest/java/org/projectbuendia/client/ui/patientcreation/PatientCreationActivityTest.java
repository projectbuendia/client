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

package org.projectbuendia.client.ui.patientcreation;

import org.projectbuendia.client.R;
import org.projectbuendia.client.ui.FunctionalTestCase;

import java.util.Date;

import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.Espresso.pressBack;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.click;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.scrollTo;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.typeText;
import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.matches;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isDescendantOfA;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isDisplayed;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withId;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.AllOf.allOf;

/** Tests for {@link PatientCreationActivity}. */
public class PatientCreationActivityTest extends FunctionalTestCase {

    /** Populates all the fields on the New Patient screen, except location. */
    private void populateNewPatientFieldsExceptLocation(String id) {
        screenshot("Before Patient Populated");
        String given = "Given" + id;
        String family = "Family" + id;
        onView(withId(R.id.patient_creation_text_patient_id)).perform(typeText(id));
        onView(withId(R.id.patient_creation_text_patient_given_name)).perform(typeText(given));
        onView(withId(R.id.patient_creation_text_patient_family_name)).perform(typeText(family));
        onView(withId(R.id.patient_creation_text_age))
                .perform(typeText(id.substring(id.length() - 2)));
        onView(withId(R.id.patient_creation_radiogroup_age_units_years)).perform(click());
        onView(withId(R.id.patient_creation_radiogroup_age_units_months)).perform(click());
        onView(withId(R.id.patient_creation_radiogroup_age_sex_male)).perform(click());
        onView(withId(R.id.patient_creation_radiogroup_age_sex_female)).perform(click());
        screenshot("After Patient Populated");
    }

    /** Tests adding a new patient with a location. */
    public void testNewPatientWithLocation() {
        inUserLoginGoToPatientCreation();
        screenshot("Test Start");
        String id = Long.toString(new Date().getTime() % 100000);
        populateNewPatientFieldsExceptLocation(id);
        onView(withId(R.id.patient_creation_button_change_location)).perform(scrollTo(), click());
        screenshot("After Location Dialog Shown");
        onView(withText("S1")).perform(click());
        screenshot("After Location Selected");
        onView(withText("Create")).perform(click());
        screenshot("After Create Pressed");

        waitForProgressFragment();

        // The new patient should be visible in the list for tent S1
        onView(withText("S1")).perform(click());
        screenshot("In S1");
        inPatientListClickPatientWithId(id);
        screenshot("After Patient Clicked");
    }

    /** Tests adding a new patient with no location. */
    public void testNewPatientWithoutLocation() {
        inUserLoginGoToPatientCreation();
        screenshot("Test Start");
        String id = Long.toString(new Date().getTime() % 100000);
        populateNewPatientFieldsExceptLocation(id);
        screenshot("After Patient Populated");
        onView(withText("Create")).perform(click());
        screenshot("After Create Pressed");

        waitForProgressFragment();

        // The new patient should be visible in the list for Triage zone.
        onView(withText("Triage")).perform(click());
        screenshot("In Triage");
        inPatientListClickPatientWithId(id);
        screenshot("After Patient Clicked");

        // The admission date should be visible right after adding a patient.
        // Flaky because of potential periodic syncs.
        checkViewDisplayedWithin(allOf(
                isDescendantOfA(withId(R.id.attribute_admission_days)),
                withText("Day 1")), 90000);

        // The symptom onset date should not be assigned a default value.
        onView(allOf(
                isDescendantOfA(withId(R.id.attribute_symptoms_onset_days)),
                withText("–")))
                .check(matches(isDisplayed()));
    }

    /** Tests that a confirmation prompt appears upon cancelling the form. */
    public void testNewPatientCancel() {
        inUserLoginGoToPatientCreation();
        screenshot("Test Start");
        onView(withId(R.id.patient_creation_text_patient_id)).perform(typeText("xyz"));
        screenshot("After Id Added");
        pressBack(); // close the keyboard
        screenshot("After Keyboard Closed");

        // Attempting to back out of the activity should trigger a prompt
        pressBack();
        onView(withText(containsString("Discard"))).check(matches(isDisplayed()));
        screenshot("Discard Prompt");

        // Dismiss the prompt
        onView(withText("Yes")).perform(click());
        screenshot("Discard Prompt Dismissed");
    }
}
