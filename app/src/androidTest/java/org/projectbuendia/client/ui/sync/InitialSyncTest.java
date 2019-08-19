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

import android.support.test.espresso.Espresso;

import androidx.test.annotation.UiThreadTest;

import org.junit.Ignore;
import org.junit.Test;
import org.projectbuendia.client.R;
import org.projectbuendia.client.events.sync.SyncCancelledEvent;

import java.util.UUID;

/** Test case for behavior during and immediately after an initial sync. */
public class InitialSyncTest extends SyncTestCase {

    @Override public void setUp() throws Exception {
        super.setUp();
        click(viewWithText("Guest User"));
    }

    /** Expects zones and tents to appear within Espresso's idling period (60s). */
    @Test
    @UiThreadTest
    @Ignore
    public void testZonesAndTentsDisplayed() {
        screenshot("Before Sync Completed");
        waitForProgressFragment();
        try {
            Thread.sleep(5000);
        } catch (InterruptedException ignored) {
        }


        inLocationSelectionCheckZonesAndTentsDisplayed();
    }

    /** Tests that search functionality works right after initial sync. */
    @Test
    @UiThreadTest
    @Ignore
    // TODO(sdspikes): re-enable once there is a working waitForProgressFragment
    public void testSearchAfterSync() throws Throwable {
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
    @Test
    @UiThreadTest
    @Ignore
    // TODO(sdspikes): should there be a cancel button on sync screen?
    public void testSyncCancelButton() {
        // Cancel the sync.
        EventBusIdlingResource<SyncCancelledEvent> syncCanceledResource =
            new EventBusIdlingResource<>(UUID.randomUUID().toString(), mEventBus);
        // There may be a slight delay before the cancel button appears.
        waitUntilVisible(viewWithId(R.id.cancel_action));
        click(viewWithId(R.id.cancel_action));
        Espresso.registerIdlingResources(syncCanceledResource);

        // Select guest user again -- give plenty of time for cancellation to occur since canceling
        // certain network operations can take an exceedingly long time.
        waitUntilVisible(90000, viewWithText("Guest User"));
        click(viewWithText("Guest User"));

        // The second sync should actually complete.
        waitForProgressFragment();
        waitUntilVisible(viewWithText("ALL PRESENT PATIENTS"));
        waitUntilVisible(viewWithText(LOCATION_NAME));
    }
}
