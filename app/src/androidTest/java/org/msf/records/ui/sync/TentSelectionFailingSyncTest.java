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
        // TODO: Potentially flaky, as sync may finish before being force-failed.
        waitForInitialSync();
        failSync();

        onView(withText(R.string.sync_failed_dialog_message)).check(matches(isDisplayed()));
        screenshot("After Sync Fails");

        onView(withText(R.string.sync_failed_settings)).check(matches(isDisplayed()));
        onView(withText(R.string.sync_failed_retry)).check(matches(isDisplayed()));
        screenshot("Test Finish");
    }

    /** Tests that clicking 'Settings' in sync failed dialog loads settings activity. */
    public void testSyncFailedDialog_SettingsButtonLoadsSettings() {
        // TODO: Potentially flaky, as sync may finish before being force-failed.
        waitForInitialSync();
        failSync();

        onView(withText(R.string.sync_failed_settings)).check(matches(isDisplayed()));
        screenshot("After Sync Fails");

        onView(withText(R.string.sync_failed_settings)).perform(click());
        screenshot("After Settings Clicked");

        onView(withText("OpenMRS base URL")).check(matches(isDisplayed()));
    }

    /** Tests that clicking 'Retry' in sync failed dialog reshows the modal 'loading' dialog. */
    public void testSyncFailedDialog_RetryButtonRetainsLoadingDialog() {
        screenshot("Test Start");
        // TODO: Potentially flaky, as sync may finish before being force-failed.
        waitForInitialSync();
        failSync();

        onView(withText(R.string.sync_failed_retry)).check(matches(isDisplayed()));
        screenshot("After Sync Failed");

        onView(withText(R.string.sync_failed_retry)).perform(click());

        onView(withText(R.string.tent_selection_dialog_message)).check(matches(isDisplayed()));
        screenshot("After Retry Clicked");
    }

    /** Tests that 'Retry' actually works if the the retried sync is successful. */
    public void testSyncFailedDialog_RetryButtonActuallyRetries() {
        // TODO: Potentially flaky, as sync may finish before being force-failed.
        waitForInitialSync();
        failSync();

        onView(withText(R.string.sync_failed_retry)).check(matches(isDisplayed()));
        screenshot("After Sync Failed");

        onView(withText(R.string.sync_failed_retry)).perform(click());

        waitForInitialSync();  // Note the lack of failSync() this time.
        waitForProgressFragment(); // Also make sure dialog disappears before checking anything.
        screenshot("After Retry Clicked");

        // Should be at tent selection screen with tents available.
        onView(withText("S1")).check(matches(isDisplayed()));

        screenshot("After Sync Completed");
    }

    /**
     * Tests that clicking 'Settings' in sync failed dialog and returning to
     * this activity results in the 'loading' dialog still being shown.
     */
    public void testSyncFailedDialog_ReturningFromSettingsRetainsLoadingDialog() {
        // TODO: Potentially flaky, as sync may finish before being force-failed.
        waitForInitialSync();
        failSync();

        onView(withText(R.string.sync_failed_settings)).check(matches(isDisplayed()));
        screenshot("After Sync Failed");

        onView(withText(R.string.sync_failed_settings)).perform(click());
        screenshot("After Settings Clicked");

        pressBack();
        onView(withText(R.string.tent_selection_dialog_message)).check(matches(isDisplayed()));
        screenshot("After Back Pressed");
    }
}
