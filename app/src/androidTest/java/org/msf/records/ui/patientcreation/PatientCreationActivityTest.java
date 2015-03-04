package org.msf.records.ui.patientcreation;

import org.msf.records.R;
import org.msf.records.ui.FunctionalTestCase;

import java.util.Date;

import static com.google.android.apps.common.testing.ui.espresso.Espresso.onData;
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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.AllOf.allOf;

import static org.msf.records.ui.matchers.AppPatientMatchers.isPatientWithId;

public class PatientCreationActivityTest extends FunctionalTestCase {

    public void setUp() throws Exception {
        super.setUp();
        // Go to PatientCreationActivity
        onView(withText("Guest User")).perform(click());

        // NOTE: Requires additional setup to enter add patient screen. See enterAddPatientScreen().
    }

    private void enterAddPatientScreen() {
        waitForProgressFragment(); // Cannot be run in setUp
        onView(withId(R.id.action_add)).perform(click());
        onView(withText("New Patient")).check(matches(isDisplayed()));
    }

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

    /** Checks that a given patient appears in the patient list, and clicks it. */
    private void clickPatientWithIdInPatientList(String id) {
        screenshot("Before Patient Selected");
        onData(isPatientWithId(equalTo(id)))
                .inAdapterView(withId(R.id.fragment_patient_list))
                .atPosition(0)
                .perform(click());
        screenshot("After Patient Selected");
    }

    /** Tests adding a new patient with a location. */
    public void testNewPatientWithLocation() {
        enterAddPatientScreen();
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
        clickPatientWithIdInPatientList(id);
        screenshot("After Patient Clicked");
    }

    /** Tests adding a new patient with no location. */
    public void testNewPatientWithoutLocation() {
        enterAddPatientScreen();
        screenshot("Test Start");
        String id = Long.toString(new Date().getTime() % 100000);
        populateNewPatientFieldsExceptLocation(id);
        screenshot("After Patient Populated");
        onView(withText("Create")).perform(click());
        screenshot("After Create Pressed");

        waitForProgressFragment();

        // The new patient should be visible in the list for Triage zone
        onView(withText("Triage")).perform(click());
        screenshot("In Triage");
        clickPatientWithIdInPatientList(id);
        screenshot("After Patient Clicked");
    }

    /** Tests that the admission date is visible right after adding a patient. */
    public void testNewPatientHasDefaultAdmissionDate() {
        testNewPatientWithoutLocation();
        checkViewDisplayedWithin(allOf(
                isDescendantOfA(withId(R.id.attribute_admission_days)),
                withText("Day 1")), 60000);
    }

    /** Tests that symptoms onset is optional and not assigned a default value. */
    public void testNewPatientDoesNotHaveDefaultSymptomsOnsetDate() {
        testNewPatientWithoutLocation();
        onView(allOf(
                isDescendantOfA(withId(R.id.attribute_symptoms_onset_days)),
                withText("–")))
                .check(matches(isDisplayed()));
    }

    /** Tests that a confirmation prompt appears upon cancelling the form. */
    public void testNewPatientCancel() {
        enterAddPatientScreen();
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
