package org.msf.records.ui.userlogin;

import org.msf.records.R;
import org.msf.records.ui.FunctionalTestCase;
import org.msf.records.ui.ProgressFragment;
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

    public void setUp() {
        // We need to explicitly specify the ProgressFragment here since we can't check the current
        // activity during setup. This works for UserLoginActivity because it is always returned
        // by getActivity().
        waitForProgressFragment(
                (ProgressFragment)(
                        getActivity().getSupportFragmentManager().getFragments().get(0)));
    }

    /** Adds a new user and logs in. */
    public void testAddUser() {
        screenshot("Test Start");
        long n = new Date().getTime() % 1000;
        String username = "test" + n;
        String given = "Testgiven" + n;
        String family = "Testfamily" + n;

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
        onView(withText("MSF Medical Records")).check(matches(isDisplayed()));
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
        onView(withText("MSF Medical Records")).check(matches(isDisplayed()));
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
