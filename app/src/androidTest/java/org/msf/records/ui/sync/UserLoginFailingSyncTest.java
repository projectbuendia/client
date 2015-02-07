package org.msf.records.ui.sync;

import org.msf.records.App;
import org.msf.records.R;

import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.click;
import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.matches;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isDisplayed;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withText;

/**
 * Test cases for handling when the initial sync fails on the user login screen.
 */
public class UserLoginFailingSyncTest extends SyncTestCase {
    @Override
    public void setUp() throws Exception {
        setWaitForUserSync(false);
        super.setUp();

        // Potentially flaky, but hopefully shouldn't be in practice.
        App.getInstance().getUserManager().cancelLastUserSyncTask();
    }

    /** Tests that sync failure results in the sync failed dialog appearing. */
    public void testSyncFailedDialogAppearsWhenSyncFails() {
        onView(withText(R.string.user_sync_failed_dialog_message)).check(matches(isDisplayed()));
        screenshot("After Sync Fails");

        onView(withText(R.string.sync_failed_settings)).check(matches(isDisplayed()));
        onView(withText(R.string.sync_failed_retry)).check(matches(isDisplayed()));
        screenshot("Test Finish");
    }

    /** Tests that clicking 'Settings' in sync failed dialog loads settings activity. */
    public void testSyncFailedDialog_SettingsButtonLoadsSettings() {
        onView(withText(R.string.sync_failed_settings)).check(matches(isDisplayed()));
        screenshot("After Sync Fails");

        onView(withText(R.string.sync_failed_settings)).perform(click());
        screenshot("After Settings Clicked");

        onView(withText("OpenMRS base URL")).check(matches(isDisplayed()));
    }

    /** Tests that 'Retry' actually works if the the retried sync is successful. */
    public void testSyncFailedDialog_RetryButtonActuallyRetries() {
        onView(withText(R.string.sync_failed_retry)).check(matches(isDisplayed()));
        screenshot("After Sync Failed");

        onView(withText(R.string.sync_failed_retry)).perform(click());

        waitForProgressFragment();
        screenshot("After Retry Clicked");

        // Should be at user selection screen with users available.
        onView(withText("GU")).check(matches(isDisplayed()));

        screenshot("After Sync Completed");
    }
}
