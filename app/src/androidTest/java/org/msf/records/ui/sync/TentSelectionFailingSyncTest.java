package org.msf.records.ui.sync;

import org.msf.records.R;

import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.Espresso.pressBack;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.click;
import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.matches;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isDisplayed;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withText;

/**
 * Test cases for handling when the initial sync fails on the tent selection screen.
 */
public class TentSelectionFailingSyncTest extends SyncTestCase {
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
    // TODO(akalachman): Temporarily disabled: known issue that spinner is shown in this case.
    /*public void testSyncFailedDialog_RetryButtonRetainsProgressBar() {
        screenshot("Test Start");
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
        setWifiEnabled(false);
        waitForSyncFailure();

        checkViewDisplayedSoon(withText(R.string.sync_failed_retry));
        screenshot("After Sync Failed");

        setWifiEnabled(true);
        onView(withText(R.string.sync_failed_retry)).perform(click());

        waitForInitialSync();
        waitForProgressFragment();
        screenshot("After Retry Clicked");

        // Should be at tent selection screen with tents available.
        onView(withText("S1")).check(matches(isDisplayed()));

        screenshot("After Sync Completed");
    }

    /**
     * Tests that clicking 'Settings' in sync failed dialog and returning to
     * this activity results in the progress bar still being shown
     */
    // TODO: Temporarily disabled.
    /*public void testSyncFailedDialog_ReturningFromSettingsRetainsProgressBar() {
        // TODO: Potentially flaky, as sync may finish before being force-failed.
        setWifiEnabled(false);

        onView(withText(R.string.sync_failed_settings)).check(matches(isDisplayed()));
        screenshot("After Sync Failed");

        onView(withText(R.string.sync_failed_settings)).perform(click());
        screenshot("After Settings Clicked");

        pressBack();
        onView(withId(R.id.progress_fragment_progress_bar)).check(matches(isDisplayed()));
        screenshot("After Back Pressed");

        cleanupWifi();
    }*/
}
