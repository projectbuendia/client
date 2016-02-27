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

import org.projectbuendia.client.R;
import org.projectbuendia.client.ui.FunctionalTestCase;
import org.projectbuendia.client.ui.matchers.UserMatchers;

import java.util.Date;

/** Tests for {@link LoginActivity}. */
public class LoginActivityTest extends FunctionalTestCase {

    /** Adds a new user and logs in. */
    public void testAddUser() {
        final long n = new Date().getTime()%100000;
        final String given = "Testgiven" + n;
        final String family = "Testfamily" + n;

        // Add new user
        click(viewWithId(R.id.action_new_user));
        type(given, viewWithId(R.id.add_user_given_name_tv));
        type(family, viewWithId(R.id.add_user_family_name_tv));
        click(viewWithText("OK"));

        waitForProgressFragment();

        // Click new user
        expectVisible(dataThat(new UserMatchers.HasFullName(given + " " + family)));
        click(dataThat(new UserMatchers.HasFullName(given + " " + family)));

        // Should be logged in
        click(viewWithText("TT"));
        expectVisible(viewWithText(given + " " + family));
    }

    /** Logs in as the guest user and logs out. */
    public void testGuestLoginLogout() {
        // Click guest user
        expectVisible(viewWithText("GU"));
        click(viewWithText("Guest User"));

        // Should be logged in; log out
        expectVisible(viewWithText(R.string.title_location_list));
        click(viewWithText("GU"));
        expectVisible(viewWithText("Guest User"));
        click(viewWithId(R.id.button_log_out));

        waitForProgressFragment();

        // Should be back at the user list
        click(viewWithId(R.id.action_new_user));
    }
}
