package org.msf.records.ui.tentselection;

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

public class TentSelectionActivityTest extends
        ActivityInstrumentationTestCase2<TentSelectionActivity> {

    public TentSelectionActivityTest() {
        super(TentSelectionActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        getActivity();
    }

    /** Looks for the expected zones. */
    public void testZonesDisplayed() {
        onView(allOf(isDisplayed(), withText("ALL PATIENTS")));
        onView(allOf(isDisplayed(), withText("Triage")));
        onView(allOf(isDisplayed(), withText("Discharged")));
    }
}
