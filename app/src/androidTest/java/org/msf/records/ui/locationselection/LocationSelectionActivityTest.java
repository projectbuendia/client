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

package org.msf.records.ui.locationselection;

import org.msf.records.R;
import org.msf.records.data.app.AppPatient;
import org.msf.records.ui.FunctionalTestCase;

import static com.google.android.apps.common.testing.ui.espresso.Espresso.onData;
import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.Espresso.pressBack;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.click;
import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.matches;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isDisplayed;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withId;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.is;

/** Tests for {@link LocationSelectionActivity}. */
public class LocationSelectionActivityTest extends FunctionalTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        onView(withText("Guest User")).perform(click());
    }

    /** Looks for the expected zones and tents. */
    public void testZonesAndTentsDisplayed() {
        waitForProgressFragment();

        // Should be at location selection screen
        checkViewDisplayedSoon(withText("ALL PRESENT PATIENTS"));
        screenshot("Test Start");

        // Zones and tents should be visible
        onView(withText("Triage")).check(matches(isDisplayed()));
        onView(withText("S1")).check(matches(isDisplayed()));
        onView(withText("S2")).check(matches(isDisplayed()));
        onView(withText("P1")).check(matches(isDisplayed()));
        onView(withText("P2")).check(matches(isDisplayed()));
        onView(withText("C1")).check(matches(isDisplayed()));
        onView(withText("C2")).check(matches(isDisplayed()));
        onView(withText("Discharged")).check(matches(isDisplayed()));
    }

    /** Tests that zones and tents are still displayed after returning from round view. */
    public void testZonesAndTentsDisplayed_afterRoundView() {
        waitForProgressFragment(); // Wait for initial load.

        // Enter round view and return.
        onView(withText("S1")).perform(click());
        waitForProgressFragment(); // Wait for search fragment to load.
        pressBack();

        // Make sure zones and tents are still displayed correctly.
        testZonesAndTentsDisplayed();
    }

    /** Tests that zones and tents are still displayed after returning from list view. */
    public void testZonesAndTentsDisplayed_afterPatientListView() {
        waitForProgressFragment(); // Wait for initial load.

        // Enter patient list view and return.
        onView(withText("ALL PRESENT PATIENTS")).perform(click());
        waitForProgressFragment(); // Wait for search fragment to load.
        pressBack();

        // Make sure zones and tents are still displayed correctly.
        testZonesAndTentsDisplayed();
    }

    /** Tests that zones and tents are still displayed after returning from settings view. */
    public void testZonesAndTentsDisplayed_afterSettingsView() {
        waitForProgressFragment(); // Wait for initial load.

        // Enter settings view and return.
        onView(withText("GU")).perform(click());
        onView(withId(R.id.button_settings)).perform(click());
        pressBack();

        // Make sure zones and tents are still displayed correctly.
        testZonesAndTentsDisplayed();
    }

    /** Tests that zones and tents are still displayed after returning from chart view. */
    public void testZonesAndTentsDisplayed_afterChartView() {
        waitForProgressFragment(); // Wait for initial load.

        // Enter a round view.
        onView(withText("S1")).perform(click());
        waitForProgressFragment(); // Wait for search fragment to load.

        // Click the first patient.
        onData(is(AppPatient.class))
                .inAdapterView(withId(R.id.fragment_patient_list))
                .atPosition(0)
                .perform(click());

        pressBack(); // Back to search fragment.
        pressBack(); // Back to location selection screen.

        // Make sure zones and tents are still displayed correctly.
        testZonesAndTentsDisplayed();
    }

    /** Tests that zones and tents are still displayed after changing a patient's location. */
    public void testZonesAndTentsDisplayed_afterPatientLocationChanged() {
        waitForProgressFragment(); // Wait for initial load.

        // Enter a round view and return.
        onView(withText("Triage")).perform(click());
        waitForProgressFragment(); // Wait for search fragment to load.

        // Click the first patient.
        onData(is(AppPatient.class))
                .inAdapterView(withId(R.id.fragment_patient_list))
                .atPosition(0)
                .perform(click());

        // Relocate the patient to C1.
        onView(withId(R.id.action_relocate_patient)).perform(click());
        onView(withText("C1")).perform(click());

        pressBack(); // Back to search fragment.
        pressBack(); // Back to tent selection screen.

        // Make sure zones and tents are still displayed correctly.
        testZonesAndTentsDisplayed();
    }
}
