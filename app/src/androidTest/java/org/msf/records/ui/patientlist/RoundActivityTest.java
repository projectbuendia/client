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

public class RoundActivityTest extends FunctionalTestCase {
    public void setUp() throws Exception {
        super.setUp();
        onView(withText("Guest User")).perform(click());
        onView(withText("Triage")).perform(click());
    }

    /** Checks for a populated title. */
    public void testTitlePopulation() {
        // TODO(akalachman): Check that title count actually matches patient count.
        assert(getActivity().getTitle().toString().matches("$Triage \\((No|[0-9]+) Patients\\)"));
    }

    /** Check that at least one patient is displayed. */
    public void testAtLeastOnePatientDisplayed() {
        // Click the first patient
        onData(is(AppPatient.class))
                .inAdapterView(withId(R.id.fragment_patient_list))
                .atPosition(0)
                .perform(click());
    }
}
