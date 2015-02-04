package org.msf.records.ui.sync;

import com.google.android.apps.common.testing.ui.espresso.Espresso;
import com.google.common.eventbus.EventBus;

import org.mockito.Mock;
import org.msf.records.sync.SyncManager;
import org.msf.records.ui.FakeEventBus;
import org.msf.records.ui.FunctionalTestCase;
import org.msf.records.utils.EventBusRegistrationInterface;

import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.click;
import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.matches;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isDisplayed;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withText;
import static org.mockito.Mockito.verify;

public class InitialSyncTest extends SyncTestCase {
    @Mock private SyncManager mMockSyncManager;
    private EventBusRegistrationInterface mEventBus;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        mEventBus = new FakeEventBus();

        // Wait for users to sync.
        UserManagerIdlingResource resource = new UserManagerIdlingResource(mEventBus);
        Espresso.registerIdlingResources(resource);

        onView(withText("Guest User")).perform(click());
    }

    /** Expects sync to be in progress. */
    public void testRequestsSync() {
        verify(mMockSyncManager).forceSync();
    }

    /** Expects zones and tents to appear within Espresso's idling period (60s). */
    public void testZonesAndTentsDisplayed() {
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
}
