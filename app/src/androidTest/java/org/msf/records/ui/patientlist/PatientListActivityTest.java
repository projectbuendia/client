package org.msf.records.ui.patientlist;

import org.msf.records.R;
import org.msf.records.data.app.AppPatient;
import org.msf.records.ui.FunctionalTestCase;
import org.msf.records.ui.userlogin.UserLoginActivity;

import static com.google.android.apps.common.testing.ui.espresso.Espresso.onData;
import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.click;
import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.matches;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isDisplayed;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withId;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.is;
import static org.msf.records.ui.matchers.StringMatchers.matchesRegex;

public class PatientListActivityTest extends FunctionalTestCase {

    public void setUp() throws Exception {
        super.setUp();
        onView(withText("Guest User")).perform(click());
        onView(withText("ALL PATIENTS")).perform(click());
    }

    /** Looks for the filter menu. */
    public void testFilterMenu() {
        onView(withText("All Patients")).perform(click());
        onView(withText("Triage")).check(matches(isDisplayed()));
        onView(withText("Pregnant")).check(matches(isDisplayed()));
    }

    /** Looks for a zone heading and at least one patient. */
    public void testZoneAndPatientDisplayed() {
        onView(withText(matchesRegex("(Triage|S1) \\((No|[0-9]+) patients?\\)")))
                .check(matches(isDisplayed()));

        // Click the first patient
        onData(is(AppPatient.class))
                .inAdapterView(withId(R.id.fragment_patient_list))
                .atPosition(0)
                .perform(click());
    }
}
