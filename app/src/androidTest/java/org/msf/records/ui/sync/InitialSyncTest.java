// Copyright 2015 The Project Buendia Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at: http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distrib-
// uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
// specific language governing permissions and limitations under the License.

package org.msf.records.ui.sync;

import com.google.android.apps.common.testing.ui.espresso.Espresso;

import org.msf.records.R;
import org.msf.records.data.app.AppPatient;
import org.msf.records.events.sync.SyncCanceledEvent;

import java.util.UUID;

import static com.google.android.apps.common.testing.ui.espresso.Espresso.onData;
import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.click;
import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.matches;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isDisplayed;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withId;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.is;

/** Test case for behavior during and immediately after an initial sync. */
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
        checkViewDisplayedSoon(withText("ALL PRESENT PATIENTS"));

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

    /** Tests the behavior of the sync cancel button. */
    public void testSyncCancelButton() {
        // Cancel the sync.
        EventBusIdlingResource<SyncCanceledEvent> syncCanceledResource =
                new EventBusIdlingResource<>(UUID.randomUUID().toString(), mEventBus);
        // There may be a slight delay before the cancel button appears.
        checkViewDisplayedSoon(withId(R.id.action_cancel));
        onView(withId(R.id.action_cancel)).perform(click());
        Espresso.registerIdlingResources(syncCanceledResource);

        // Select guest user again -- give plenty of time for cancellation to occur since canceling
        // certain network operations can take an exceedingly long time.
        checkViewDisplayedWithin(withText("Guest User"), 90000);
        onView(withText("Guest User")).perform(click());

        // The second sync should actually complete.
        waitForProgressFragment();
        checkViewDisplayedSoon(withText("ALL PRESENT PATIENTS"));
        checkViewDisplayedSoon(withText("S1"));
    }
}
