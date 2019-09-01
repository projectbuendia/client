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

import android.support.test.annotation.UiThreadTest;

import org.junit.After;
import org.junit.Test;
import org.projectbuendia.client.App;
import org.projectbuendia.client.R;

/** Test cases for handling user sync failure on the user login screen. */
public class UserLoginFailingSyncTest extends SyncTestCase {
    @Override public void setUp() throws Exception {
        setWaitForUserSync(false);
        // TODO/cleanup: Remove all this manual UserManager management once daggered.
        App.getUserManager().reset();
        // Force all user sync tasks to fail. If you need a sync task to pass,
        // remember to reset this to false!
        App.getUserManager().setAutoCancelEnabled(true);
        super.setUp();
    }

    @After
    public void tearDown() {
        App.getUserManager().setAutoCancelEnabled(false);
        super.tearDown();
    }

    /** Tests that sync failure results in the sync failed dialog appearing. */
    @Test
    @UiThreadTest
    public void testSyncFailedDialogAppearsWhenSyncFails() {
        screenshot("Test Start");
        expectVisible(viewWithText(R.string.user_sync_failed_dialog_message));
        App.getUserManager().setAutoCancelEnabled(false);
        screenshot("After Sync Fails");

        expectVisible(viewWithText(R.string.sync_failed_settings));
        expectVisible(viewWithText(R.string.sync_failed_retry));
        screenshot("Test Finish");
    }

    /** Tests that clicking 'Settings' in sync failed dialog loads settings activity. */
    @Test
    @UiThreadTest
    public void testSyncFailedDialog_SettingsButtonLoadsSettings() {
        screenshot("Test Start");
        expectVisible(viewWithText(R.string.sync_failed_settings));
        App.getUserManager().setAutoCancelEnabled(false);
        screenshot("After Sync Fails");

        click(viewWithText(R.string.sync_failed_settings));
        screenshot("After Settings Clicked");

        expectVisible(viewWithText(R.string.pref_title_server));
    }

    /** Tests that 'Retry' actually works if the the retried sync is successful. */
    @Test
    @UiThreadTest
    public void testSyncFailedDialog_RetryButtonActuallyRetries() {
        screenshot("Test Start");
        expectVisible(viewWithText(R.string.sync_failed_retry));
        App.getUserManager().setAutoCancelEnabled(false);
        screenshot("After Sync Failed");

        click(viewWithText(R.string.sync_failed_retry));

        waitForProgressFragment();
        screenshot("After Retry Clicked");

        // Should be at user selection screen with users available.
        expectVisible(firstViewWithText("GU"));

        screenshot("After Sync Completed");
    }
}
