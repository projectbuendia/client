package org.msf.records.ui.patientlist;

import org.msf.records.R;
import org.msf.records.data.app.AppPatient;
import org.msf.records.ui.FunctionalTestCase;

import static com.google.android.apps.common.testing.ui.espresso.Espresso.onData;
import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.click;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withId;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.is;

/**
 * Test case for {@link RoundActivity}.
 */
public class RoundActivityTest extends FunctionalTestCase {
    /**
     * Initializes the test by entering the Round view for Triage.
     * @throws Exception if anything goes wrong
     */
    public void setUp() throws Exception {
        super.setUp();
        onView(withText("Guest User")).perform(click());
        waitForProgressFragment();
        onView(withText("Triage")).perform(click());
    }

    /** Checks for a populated title. */
    public void testTitlePopulation() {
        // TODO(akalachman): Check that title count actually matches patient count.
        screenshot("Test Start");
        assert getActivity().getTitle().toString().matches("$Triage \\((No|[0-9]+) Patients\\)");
    }

    /** Checks that at least one patient is displayed. */
    public void testAtLeastOnePatientDisplayed() {
        screenshot("Test Start");
        // Click the first patient
        onData(is(AppPatient.class))
                .inAdapterView(withId(R.id.fragment_patient_list))
                .atPosition(0)
                .perform(click());
        screenshot("After Patient Clicked");
    }
}
