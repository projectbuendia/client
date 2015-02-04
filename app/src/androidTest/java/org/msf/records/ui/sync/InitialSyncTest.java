package org.msf.records.ui.sync;

import com.google.android.apps.common.testing.ui.espresso.Espresso;

import org.mockito.Mock;
import org.msf.records.App;
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

import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.click;
import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.matches;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isDisplayed;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withText;
import static org.mockito.Mockito.verify;

public class InitialSyncTest extends SyncTestCase {
    private EventBusRegistrationInterface mEventBus;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        mEventBus = new EventBusWrapper(EventBus.getDefault());

        // Wait for users to sync.
        EventBusIdlingResource<KnownUsersLoadedEvent> resource =
                new EventBusIdlingResource<>("USERS", mEventBus);
        Espresso.registerIdlingResources(resource);

        onView(withText("Guest User")).perform(click());
    }

    /** Expects zones and tents to appear within Espresso's idling period (60s). */
    public void testZonesAndTentsDisplayed() {
        EventBusIdlingResource<SyncStartedEvent> syncStartedResource =
                new EventBusIdlingResource<>("SYNC", mEventBus);
        EventBusIdlingResource<SyncSucceededEvent> syncSucceededResource =
                new EventBusIdlingResource<>("SYNC_FINISH", mEventBus);
        Espresso.registerIdlingResources(syncStartedResource, syncSucceededResource);

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
