package org.msf.records.ui.userlogin;

import android.test.ActivityInstrumentationTestCase2;

import org.msf.records.R;

import java.util.Date;

import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.click;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.typeText;
import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.matches;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isDisplayed;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withId;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

public class UserLoginActivityTest extends
        ActivityInstrumentationTestCase2<UserLoginActivity> {

    public UserLoginActivityTest() {
        super(UserLoginActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        getActivity();
    }

    /** Adds a new user and logs in. */
    public void testAddUser() {
        String id = "" + new Date().getTime() % 1000;
        String username = "user" + id;
        String given = "Foo" + id;
        String family = "Bar" + id;
        // Add new user
        onView(withText("Add User")).perform(click());
        onView(withId(R.id.add_user_username_tv)).perform(typeText(username));
        onView(withId(R.id.add_user_given_name_tv)).perform(typeText(given));
        onView(withId(R.id.add_user_family_name_tv)).perform(typeText(family));
        onView(withText("OK")).perform(click());
        // Click new user
        onView(allOf(withText("FB"), isDisplayed()));
        onView(withText(given + " " + family)).perform(click());
        // Should be logged in
        onView(withText("MSF Medical Records")).check(matches(isDisplayed()));
        onView(withText("FB")).perform(click());
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
        // Should be back to user list
        onView(withText("Add User")).check(matches(isDisplayed()));
    }
}
