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

package org.projectbuendia.client.ui.userlogin;

import org.projectbuendia.client.R;
import org.projectbuendia.client.ui.FunctionalTestCase;
import org.projectbuendia.client.ui.matchers.UserMatchers;

import java.util.Date;

import static com.google.android.apps.common.testing.ui.espresso.Espresso.onData;
import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.click;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.typeText;
import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.matches;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isDisplayed;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withId;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasToString;

/** Tests for {@link UserLoginActivity}. */
public class UserLoginActivityTest extends FunctionalTestCase {

    /** Adds a new user and logs in. */
    public void testAddUser() {
        screenshot("Test Start");
        final long n = new Date().getTime() % 100000;
        final String username = "test" + n;
        final String given = "Testgiven" + n;
        final String family = "Testfamily" + n;

        // Add new user
        onView(withText("Add User")).perform(click());
        screenshot("After Add User Clicked");
        onView(withId(R.id.add_user_given_name_tv)).perform(typeText(given));
        onView(withId(R.id.add_user_family_name_tv)).perform(typeText(family));
        screenshot("After User Populated");
        onView(withText("OK")).perform(click());
        screenshot("After OK Pressed");

        waitForProgressFragment();

        // Click new user
        onData(allOf(hasToString(equalTo("TT")), isDisplayed()));
        screenshot("In User Selection");
        onData(new UserMatchers.HasFullName(given + " " + family)).perform(click());

        // Should be logged in
        onView(withText(R.string.title_activity_tent_selection)).check(matches(isDisplayed()));
        screenshot("After User Selected");
        onView(withText("TT")).perform(click());
        onView(withText(given + " " + family)).check(matches(isDisplayed()));
        screenshot("After User Selected in Action Bar");
    }

    /** Logs in as the guest user and logs out. */
    public void testGuestLoginLogout() {
        // Click guest user
        onView(withText("GU")).check(matches(isDisplayed()));
        screenshot("Test Start");
        onView(withText("Guest User")).perform(click());

        // Should be logged in; log out
        onView(withText(R.string.title_activity_tent_selection)).check(matches(isDisplayed()));
        screenshot("After Guest User Clicked");
        onView(withText("GU")).perform(click());
        onView(withText("Guest User")).check(matches(isDisplayed()));
        screenshot("After Guest User Selected in Action Bar");
        onView(withId(R.id.button_log_out)).perform(click());

        waitForProgressFragment();

        // Should be back at the user list
        onView(withText("Add User")).check(matches(isDisplayed()));
        screenshot("After Logout");
    }
}
