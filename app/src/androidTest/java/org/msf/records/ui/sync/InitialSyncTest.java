package org.msf.records.ui.sync;

import com.google.android.apps.common.testing.ui.espresso.Espresso;

import org.mockito.Mock;
import org.msf.records.App;
import org.msf.records.R;
import org.msf.records.data.app.AppPatient;
import org.msf.records.events.sync.SyncFinishedEvent;
import org.msf.records.events.sync.SyncStartedEvent;
import org.msf.records.events.sync.SyncSucceededEvent;
import org.msf.records.events.user.KnownUsersLoadedEvent;
import org.msf.records.sync.SyncManager;
import org.msf.records.ui.FakeEventBus;
import org.msf.records.ui.FunctionalTestCase;
import org.msf.records.utils.EventBusRegistrationInterface;
import org.msf.records.utils.EventBusWrapper;

import de.greenrobot.event.EventBus;

import static com.google.android.apps.common.testing.ui.espresso.Espresso.onData;
import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.click;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.typeText;
import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.matches;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isDisplayed;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withId;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;

public class InitialSyncTest extends SyncTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();

        onView(withText("Guest User")).perform(click());
    }

    /** Expects zones and tents to appear within Espresso's idling period (60s). */
    public void testZonesAndTentsDisplayed() {
        waitForInitialSync();

        // Should be at tent selection screen
        onView(withText("ALL PRESENT PATIENTS")).check(matches(isDisplayed()));

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
        waitForInitialSync();

        onView(withId(R.id.action_search)).perform(click());

        // Check that at least one patient is returned (since clicking search should show
        // all patients).
        onData(is(AppPatient.class))
                .inAdapterView(withId(R.id.fragment_patient_list))
                .atPosition(0)
                .perform(click());
    }
}
