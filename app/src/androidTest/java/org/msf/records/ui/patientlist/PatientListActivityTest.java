package org.msf.records.ui.patientlist;

import org.msf.records.R;
import org.msf.records.data.app.AppLocation;
import org.msf.records.data.app.AppPatient;
import org.msf.records.ui.FunctionalTestCase;

import static com.google.android.apps.common.testing.ui.espresso.Espresso.onData;
import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.click;
import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.matches;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isDisplayed;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withId;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.text.StringStartsWith.startsWith;
import static org.msf.records.ui.matchers.StringMatchers.matchesRegex;

public class PatientListActivityTest extends FunctionalTestCase {

    public void setUp() throws Exception {
        super.setUp();
        onView(withText("Guest User")).perform(click());
    }

    /** Opens the patient list. */
    public void openPatientList() {
        waitForProgressFragment(); // Wait for tents.
        onView(withText("ALL PRESENT PATIENTS")).perform(click());
        waitForProgressFragment(); // Wait for patients.
    }

    /** Looks for the filter menu. */
    public void testFilterMenu() {
        openPatientList();
        screenshot("Test Start");
        onView(withText("All Present Patients")).perform(click());
        onView(withText("Triage")).check(matches(isDisplayed()));
        onView(withText("Pregnant")).check(matches(isDisplayed()));
        screenshot("In Filter Menu");
    }

    /** Looks for two zone headings and at least one patient. */
    public void testZoneAndPatientDisplayed() {
        openPatientList();
        screenshot("Test Start");
        // There should be patients in both Triage and S1.
        onView(withText(matchesRegex("Triage \\((No|[0-9]+) patients?\\)")))
                .check(matches(isDisplayed()));

        onData(allOf(is(AppLocation.class), hasToString(startsWith("S1"))))
                .inAdapterView(withId(R.id.fragment_patient_list))
                .check(matches(isDisplayed()));

        // Click the first patient
        onData(is(AppPatient.class))
                .inAdapterView(withId(R.id.fragment_patient_list))
                .atPosition(0)
                .perform(click());
        screenshot("After Patient Clicked");
    }
}
