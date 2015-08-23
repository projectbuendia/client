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

package org.projectbuendia.client.ui.sync;

import com.google.android.apps.common.testing.ui.espresso.Espresso;

import org.projectbuendia.client.R;
import org.projectbuendia.client.events.sync.SyncCanceledEvent;

import java.util.UUID;

import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.click;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withId;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withText;

/** Test case for behavior during and immediately after an initial sync. */
public class InitialSyncTest extends SyncTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        click(viewWithText("Guest User"));
    }

    /** Expects zones and tents to appear within Espresso's idling period (60s). */
    public void testZonesAndTentsDisplayed() {
        screenshot("Before Sync Completed");
        waitForProgressFragment();
        inLocationSelectionCheckZonesAndTentsDisplayed();
    }

    /** Tests that search functionality works right after initial sync. */
    public void testSearchAfterSync() {
        screenshot("Before Sync Completed");

        waitForProgressFragment();
        screenshot("After Sync Completed");

        click(viewWithId(R.id.action_search));
        screenshot("After Search Clicked");

        // Check that at least one patient is returned (since clicking search
        // should show all patients).
        inPatientListClickFirstPatient(); // open patient chart
        screenshot("Final State");
    }

    /** Tests the behavior of the sync cancel button. */
    public void testSyncCancelButton() {
        // Cancel the sync.
        EventBusIdlingResource<SyncCanceledEvent> syncCanceledResource =
                new EventBusIdlingResource<>(UUID.randomUUID().toString(), mEventBus);
        // There may be a slight delay before the cancel button appears.
        expectVisibleSoon(viewWithId(R.id.action_cancel));
        click(viewWithId(R.id.action_cancel));
        Espresso.registerIdlingResources(syncCanceledResource);

        // Select guest user again -- give plenty of time for cancellation to occur since canceling
        // certain network operations can take an exceedingly long time.
        expectVisibleWithin(90000, viewWithText("Guest User"));
        click(viewWithText("Guest User"));

        // The second sync should actually complete.
        waitForProgressFragment();
        expectVisibleSoon(viewWithText("ALL PRESENT PATIENTS"));
        expectVisibleSoon(viewWithText("S1"));
    }
}
