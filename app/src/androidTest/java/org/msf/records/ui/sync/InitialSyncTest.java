package org.msf.records.ui.sync;

import org.msf.records.R;
import org.msf.records.data.app.AppPatient;

import static com.google.android.apps.common.testing.ui.espresso.Espresso.onData;
import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.click;
import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.matches;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isDisplayed;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withId;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.is;

public class InitialSyncTest extends SyncTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();

        onView(withText("Guest User")).perform(click());
    }

    /** Expects zones and tents to appear within Espresso's idling period (60s). */
    public void testZonesAndTentsDisplayed() {
        screenshot("Before Sync Completed");

        waitForProgressFragment();

        // Should be at tent selection screen
        onView(withText("ALL PRESENT PATIENTS")).check(matches(isDisplayed()));

        screenshot("After Sync Completed");

        // Zones and tents should be visible
        onView(withText("Triage")).check(matches(isDisplayed()));
        onView(withText("S1")).check(matches(isDisplayed()));
        onView(withText("S2")).check(matches(isDisplayed()));
        onView(withText("P1")).check(matches(isDisplayed()));
        onView(withText("P2")).check(matches(isDisplayed()));
        onView(withText("C1")).check(matches(isDisplayed()));
        onView(withText("C2")).check(matches(isDisplayed()));
        onView(withText("Discharged")).check(matches(isDisplayed()));
    }

    /** Tests that search functionality works right after initial sync. */
    public void testSearchAfterSync() {
        screenshot("Before Sync Completed");

        waitForProgressFragment();

        screenshot("After Sync Completed");

        onView(withId(R.id.action_search)).perform(click());

        screenshot("After Search Clicked");

        // Check that at least one patient is returned (since clicking search should show
        // all patients).
        onData(is(AppPatient.class))
                .inAdapterView(withId(R.id.fragment_patient_list))
                .atPosition(0)
                .perform(click());

        screenshot("Final State");
    }
}
