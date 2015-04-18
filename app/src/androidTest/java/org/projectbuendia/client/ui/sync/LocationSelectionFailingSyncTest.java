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

import org.projectbuendia.client.R;

import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.Espresso.pressBack;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.click;
import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.matches;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isDisplayed;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withText;

/** Test cases for handling when the initial sync fails on the location selection screen. */
public class LocationSelectionFailingSyncTest extends SyncTestCase {
    @Override
    public void setUp() throws Exception {
        super.setUp();

        onView(withText("Guest User")).perform(click());
    }

    /** Tests that sync failure results in the sync failed dialog appearing. */
    public void testSyncFailedDialogAppearsWhenSyncFails() {
        setWifiEnabled(false);
        waitForSyncFailure();

        checkViewDisplayedSoon(withText(R.string.sync_failed_dialog_message));
        screenshot("After Sync Fails");

        onView(withText(R.string.sync_failed_settings)).check(matches(isDisplayed()));
        onView(withText(R.string.sync_failed_retry)).check(matches(isDisplayed()));
        onView(withText("Back")).perform(click());
        screenshot("Test Finish");

        cleanupWifi();
    }

    /** Tests that the back button in the sync failed dialog returns to user selection. */
    public void testSyncFailedDialog_backButtonReturnsToUserSelection() {
        setWifiEnabled(false);
        waitForSyncFailure();

        checkViewDisplayedSoon(withText(R.string.sync_failed_dialog_message));

        onView(withText(R.string.sync_failed_back)).perform(click());
        screenshot("After Sync Fails");
        waitForProgressFragment(); // Wait for user screen to display.
        onView(withText("GU")).check(matches(isDisplayed()));
        screenshot("Test Finish");

        cleanupWifi();
    }

    /** Tests that clicking 'Settings' in sync failed dialog loads settings activity. */
    public void testSyncFailedDialog_SettingsButtonLoadsSettings() {
        setWifiEnabled(false);
        waitForSyncFailure();

        checkViewDisplayedSoon(withText(R.string.sync_failed_settings));
        screenshot("After Sync Fails");

        onView(withText(R.string.sync_failed_settings)).perform(click());
        screenshot("After Settings Clicked");

        onView(withText("OpenMRS base URL")).check(matches(isDisplayed()));

        // Go back to user selection before cleaning up wifi, or a sync will start.
        pressBack();
        pressBack();
        cleanupWifi();
    }

    /** Tests that clicking 'Retry' in sync failed dialog reshows the progress bar. */
    // TODO/robustness: This test is flaky because of a real bug -- Volley is
    // unresponsive to changes in connectivity state, so a sync may not fail
    // for seconds or even minutes after wifi is turned off.
    /*public void testSyncFailedDialog_RetryButtonRetainsProgressBar() {
        screenshot("Test Start");
        waitForSyncFailure();
        setWifiEnabled(false);

        checkViewDisplayedSoon(withText(R.string.sync_failed_retry));
        screenshot("After Sync Failed");

        onView(withText(R.string.sync_failed_retry)).perform(click());

        setWifiEnabled(true);
        Espresso.registerIdlingResources(new WifiStateIdlingResource());
        // Showing progress bar may be slow as the spinner may show while sync is still starting up.
        checkViewDisplayedSoon(withId(R.id.progress_fragment_progress_bar));
        screenshot("After Retry Clicked");
    }*/

    /** Tests that 'Retry' actually works if the the retried sync is successful. */
    public void testSyncFailedDialog_RetryButtonActuallyRetries() {
        waitForSyncFailure();
        setWifiEnabled(false);

        checkViewDisplayedSoon(withText(R.string.sync_failed_retry));
        screenshot("After Sync Failed");

        setWifiEnabled(true);
        onView(withText(R.string.sync_failed_retry)).perform(click());

        waitForInitialSync();
        waitForProgressFragment();
        screenshot("After Retry Clicked");

        // Should be at location selection screen with locations available.
        onView(withText("S1")).check(matches(isDisplayed()));

        screenshot("After Sync Completed");
    }

    /**
     * Tests that clicking 'Settings' in sync failed dialog and returning to
     * this activity results in the progress bar still being shown.
     */
    // TODO/robustness: This test is flaky because of a real bug -- Volley is
    // unresponsive to changes in connectivity state, so a sync may not fail
    // for seconds or even minutes after wifi is turned off.
    /*public void testSyncFailedDialog_ReturningFromSettingsRetainsProgressBar() {
        setWifiEnabled(false);
        waitForSyncFailure();

        checkViewDisplayedSoon(withText(R.string.sync_failed_settings));
        screenshot("After Sync Failed");

        onView(withText(R.string.sync_failed_settings)).perform(click());
        screenshot("After Settings Clicked");

        setWifiEnabled(true);
        pressBack();
        checkViewDisplayedSoon(withId(R.id.progress_fragment_progress_bar));
        screenshot("After Back Pressed");

        cleanupWifi();
    }*/
}
