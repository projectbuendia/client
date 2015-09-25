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

import static android.support.test.espresso.Espresso.pressBack;

/** Test cases for handling when the initial sync fails on the location selection screen. */
public class LocationSelectionFailingSyncTest extends SyncTestCase {
    @Override public void setUp() throws Exception {
        super.setUp();

        click(viewWithText("Guest User"));
    }

    /** Tests that sync failure results in the sync failed dialog appearing. */
    public void testSyncFailedDialogAppearsWhenSyncFails() {
        setWifiEnabled(false);
        try (WifiDisabler wd = new WifiDisabler()) {
            waitForSyncFailure();

            expectVisibleSoon(viewWithText(R.string.sync_failed_dialog_message));
            screenshot("After Sync Fails");

            expectVisible(viewWithText(R.string.sync_failed_settings));
            expectVisible(viewWithText(R.string.sync_failed_retry));
            click(viewWithText("Back"));
            screenshot("Test Finish");
        }
    }

    /** Tests that the back button in the sync failed dialog returns to user selection. */
    public void testSyncFailedDialog_backButtonReturnsToUserSelection() {
        try (WifiDisabler wd = new WifiDisabler()) {
            waitForSyncFailure();

            expectVisibleSoon(viewWithText(R.string.sync_failed_dialog_message));

            click(viewWithText(R.string.sync_failed_back));
            screenshot("After Sync Fails");
            waitForProgressFragment(); // Wait for user screen to display.
            expectVisible(viewWithText("Guest User"));
            screenshot("Test Finish");
        }
    }

    /** Tests that clicking 'Settings' in sync failed dialog loads settings activity. */
    public void testSyncFailedDialog_SettingsButtonLoadsSettings() {
        setWifiEnabled(false);
        waitForSyncFailure();

        expectVisibleSoon(viewWithText(R.string.sync_failed_settings));
        screenshot("After Sync Fails");

        click(viewWithText(R.string.sync_failed_settings));
        screenshot("After Settings Clicked");

        expectVisible(viewWithText("Advanced"));
        click(viewWithText("Advanced"));

        expectVisible(viewWithText("OpenMRS base URL"));

        // Go back to user selection before cleaning up wifi, or a sync will start.
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

        expectVisibleSoon(viewWithText(R.string.sync_failed_retry));
        screenshot("After Sync Failed");

        click(viewWithText(R.string.sync_failed_retry));

        setWifiEnabled(true);
        Espresso.registerIdlingResources(new WifiStateIdlingResource());
        // Showing progress bar may be slow as the spinner may show while sync is still starting up.
        expectVisibleSoon(viewWithId(R.id.progress_fragment_progress_bar));
        screenshot("After Retry Clicked");
    }*/

    /** Tests that 'Retry' actually works if the the retried sync is successful. */
    public void testSyncFailedDialog_RetryButtonActuallyRetries() {
        try (WifiDisabler wd = new WifiDisabler()) {
            waitForSyncFailure();

            expectVisibleSoon(viewWithText(R.string.sync_failed_retry));
            screenshot("After Sync Failed");

            setWifiEnabled(true);
            click(viewWithText(R.string.sync_failed_retry));

            waitForInitialSync();
            waitForProgressFragment();
            screenshot("After Retry Clicked");

            // Should be at location selection screen with locations available.
            expectVisible(viewWithText("Triage"));
            expectVisible(viewWithText("Discharged"));

            screenshot("After Sync Completed");
        }
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

        expectVisibleSoon(viewWithText(R.string.sync_failed_settings));
        screenshot("After Sync Failed");

        click(viewWithText(R.string.sync_failed_settings));
        screenshot("After Settings Clicked");

        setWifiEnabled(true);
        pressBack();
        expectVisibleSoon(viewWithId(R.id.progress_fragment_progress_bar));
        screenshot("After Back Pressed");

        cleanupWifi();
    }*/
}
