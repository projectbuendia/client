package org.msf.records.ui.patientlist;

import android.test.ActivityInstrumentationTestCase2;

import org.msf.records.R;

import java.util.Date;

import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.click;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.typeText;
import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.matches;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isDisplayed;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withId;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;

public class PatientListActivityTest extends
        ActivityInstrumentationTestCase2<PatientListActivity> {

    public PatientListActivityTest() {
        super(PatientListActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        getActivity();
    }

    /** Looks for the filter menu and the first zone heading. */
    public void testBasicDisplay() {
        onView(allOf(isDisplayed(), withText("All Patients")));
        onView(allOf(isDisplayed(), withText(
                allOf(startsWith("Triage ("), endsWith("patients)")))));
    }
}
