package org.msf.records.ui.userlogin;

import org.msf.records.R;
import org.msf.records.ui.FunctionalTestCase;
import org.msf.records.ui.matchers.UserMatchers;

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

public class UserLoginActivityTest extends FunctionalTestCase {

    /** Adds a new user and logs in. */
    public void testAddUser() {
        long n = new Date().getTime() % 1000;
        String username = "test" + n;
        String given = "Testgiven" + n;
        String family = "Testfamily" + n;

        // Add new user
        onView(withText("Add User")).perform(click());
        onView(withId(R.id.add_user_given_name_tv)).perform(typeText(given));
        onView(withId(R.id.add_user_family_name_tv)).perform(typeText(family));
        onView(withText("OK")).perform(click());

        // Click new user
        onData(allOf(hasToString(equalTo("TT")), isDisplayed()));
        onData(new UserMatchers.HasFullName(given + " " + family)).perform(click());

        // Should be logged in
        onView(withText("MSF Medical Records")).check(matches(isDisplayed()));
        onView(withText("TT")).perform(click());
        onView(withText(given + " " + family)).check(matches(isDisplayed()));
    }

    /** Logs in as the guest user and logs out. */
    public void testGuestLoginLogout() {
        // Click guest user
        onView(withText("GU")).check(matches(isDisplayed()));
        onView(withText("Guest User")).perform(click());

        // Should be logged in; log out
        onView(withText("MSF Medical Records")).check(matches(isDisplayed()));
        onView(withText("GU")).perform(click());
        onView(withText("Guest User")).check(matches(isDisplayed()));
        onView(withId(R.id.button_log_out)).perform(click());

        // Should be back at the user list
        onView(withText("Add User")).check(matches(isDisplayed()));
    }
}
