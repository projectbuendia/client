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

package org.projectbuendia.client.ui.login;

import android.support.test.annotation.UiThreadTest;

import org.junit.Test;
import org.mockito.Mock;
import org.projectbuendia.client.R;
import org.projectbuendia.client.ui.FunctionalTestCase;

import java.util.Date;

/** Tests for {@link LoginActivity}. */
public class LoginActivityTest extends FunctionalTestCase {

    @Mock
    private LoginController.Ui mMockUi;

    /** Adds a new user and logs in. */
    @Test
    @UiThreadTest
    public void testAddUser() {
        screenshot("Test Start");
        final long n = new Date().getTime()%100000;
        final String given = "Testgiven" + n;
        final String family = "Testfamily" + n;

        // Add new user
        click(viewWithId(R.id.action_new_user));
        screenshot("After Add User Clicked");
        type(given, viewWithId(R.id.add_user_given_name_tv));
        type(family, viewWithId(R.id.add_user_family_name_tv));
        screenshot("After User Populated");
        click(viewWithText("OK"));
        screenshot("After OK Pressed");

        waitForProgressFragment();

//        verify(mMockUi).populateActionBar(
//                argThat(new UserMatchers.HasFullName(given + " " + family)));
        // Click new user

        final String fullName = given + " " + family;

        expectVisible(viewWithText(fullName));

        screenshot("In User Selection");
        click(viewWithText(fullName));

        // Should be logged in
        screenshot("After User Selected");

        click(viewWithText("TT"));
        expectVisible(viewWithText(fullName));
        screenshot("After User Selected in Action Bar");
    }

    /** Logs in as the guest user and logs out. */
    @Test
    @UiThreadTest
    public void testGuestLoginLogout() {
        // Click guest user
        expectVisible(viewWithText("GU"));
        screenshot("Test Start");
        click(viewWithText("Guest User"));

        // Should be logged in; log out
        expectVisible(viewWithText(R.string.title_location_list));
        screenshot("After Guest User Clicked");
        click(viewWithText("GU"));
        expectVisible(viewWithText("Guest User"));
        screenshot("After Guest User Selected in Action Bar");
        click(viewWithId(R.id.button_log_out));

        waitForProgressFragment();

        // Should be back at the user list
        click(viewWithId(R.id.action_new_user));
        screenshot("After Logout");
    }
}
