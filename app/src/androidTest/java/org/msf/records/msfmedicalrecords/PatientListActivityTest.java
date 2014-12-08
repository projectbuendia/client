package org.msf.records.msfmedicalrecords;

import static com.google.android.apps.common.testing.ui.espresso.Espresso.onData;
import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.click;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.scrollTo;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.typeText;
import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.matches;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isDisplayed;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withId;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import android.test.ActivityInstrumentationTestCase2;

import org.msf.records.R;
import org.msf.records.ui.PatientListActivity;

/**
* Espresso functional tests that test the flow from the {@link PatientListActivity}.
*
* @author mtnguyen@google.com (Minh T. Nguyen)
*/
public class PatientListActivityTest extends
    ActivityInstrumentationTestCase2<PatientListActivity> {

  public PatientListActivityTest() {
    super(PatientListActivity.class);
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    getActivity();
  }

  /**
   * Tests filling out the "Add patient" form completely and then cancelling out of the form.
   */
  public void testAddingNewPatientAndCancelingOut() {
    // Open the overflow menu
    openActionBarOverflowOrOptionsMenu(getInstrumentation().getTargetContext());

    // Click on the menu item that has the text "Add Patient"
    onView(withText("Add Patient")).perform(click());

    // Verify that we see "Basic Details" somewhere
    onView(withText("Basic Details")).check(matches(isDisplayed()));

    // Fill in basic data
    onView(withId(R.id.add_patient_given_name)).perform(typeText("Minh"));
    onView(withId(R.id.add_patient_family_name)).perform(typeText("Nguyen"));
    onView(withId(R.id.add_patient_dob_estimated)).perform(click());
    onView(withId(R.id.add_patient_dob)).perform(typeText("123"));

    // Select month
    onView(withId(R.id.add_patient_age_type)).perform(scrollTo(), click());
    onData(allOf(is(instanceOf(String.class)), is("Months"))).perform(click());

    // Select gender
    onView(withId(R.id.add_patient_gender)).perform(scrollTo(), click());
    onData(allOf(is(instanceOf(String.class)), is("Female"))).perform(click());

    // Slide pregnancy
    onView(withId(R.id.add_patient_is_pregnant)).perform(scrollTo(), click());

    // Set location
    onView(withId(R.id.add_patient_location_zone)).perform(scrollTo(), click());
    onData(allOf(is(instanceOf(String.class)), is("Mortuary"))).perform(click());
    onView(withId(R.id.add_patient_location_tent)).perform(typeText("183"));
    onView(withId(R.id.add_patient_location_bed)).perform(typeText("3"));

    // Medical details
    onView(withId(R.id.add_patient_status)).perform(scrollTo(), click());
    onData(allOf(is(instanceOf(String.class)), is("Suspect"))).perform(click());
    onView(withId(R.id.add_patient_date_first_shown_symptoms)).perform(scrollTo(), click());
    // can't select a date due to bug
    // https://code.google.com/p/android-test-kit/issues/detail?id=43
    onView(withText("Done")).perform(click());
    onView(withId(R.id.add_patient_movement)).perform(scrollTo(), click());
    onData(allOf(is(instanceOf(String.class)), is("Partially dependent"))).perform(click());

    // Other details
    onView(withId(R.id.add_patient_important_information))
        .perform(scrollTo(), typeText("This is a functional test"));
    onView(withId(R.id.add_patient_next_of_kin)).perform(scrollTo(), typeText("Please ignore"));
    onView(withId(R.id.add_patient_area_district)).perform(scrollTo(), typeText("7"));
    onView(withId(R.id.add_patient_area_chiefdom)).perform(scrollTo(), typeText("Alpha"));
    onView(withId(R.id.add_patient_area_village)).perform(scrollTo(), typeText("Beta"));
    onView(withId(R.id.actionbar_cancel)).perform(click());
  }
}
