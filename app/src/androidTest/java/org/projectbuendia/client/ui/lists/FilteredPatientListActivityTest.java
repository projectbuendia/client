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

package org.projectbuendia.client.ui.lists;

import org.projectbuendia.client.R;
import org.projectbuendia.client.data.app.AppLocation;
import org.projectbuendia.client.data.app.AppPatient;
import org.projectbuendia.client.ui.FunctionalTestCase;

import static com.google.android.apps.common.testing.ui.espresso.Espresso.onData;
import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.click;
import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.matches;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isDisplayed;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withId;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.text.StringStartsWith.startsWith;
import static org.projectbuendia.client.ui.matchers.StringMatchers.matchesRegex;

/** Tests for {@link FilteredPatientListActivity}. */
public class FilteredPatientListActivityTest extends FunctionalTestCase {

    public void setUp() throws Exception {
        super.setUp();
        onView(withText("Guest User")).perform(click());
    }

    /** Opens the patient list. */
    public void openPatientList() {
        waitForProgressFragment(); // Wait for tents.
        onView(withText("ALL PRESENT PATIENTS")).perform(click());
        waitForProgressFragment(); // Wait for patients.
    }

    /** Looks for the filter menu. */
    public void testFilterMenu() {
        openPatientList();
        screenshot("Test Start");
        onView(withText("All Present Patients")).perform(click());
        onView(withText("Triage")).check(matches(isDisplayed()));
        onView(withText("Pregnant")).check(matches(isDisplayed()));
        screenshot("In Filter Menu");
    }

    /** Looks for two zone headings and at least one patient. */
    public void testZoneAndPatientDisplayed() {
        openPatientList();
        screenshot("Test Start");
        // There should be patients in both Triage and S1.
        checkViewDisplayedSoon(withText(matchesRegex("Triage \\((No|[0-9]+) patients?\\)")));

        onData(allOf(is(AppLocation.class), hasToString(startsWith("S1"))))
                .inAdapterView(withId(R.id.fragment_patient_list))
                .check(matches(isDisplayed()));

        // Click the first patient
        onData(is(AppPatient.class))
                .inAdapterView(withId(R.id.fragment_patient_list))
                .atPosition(0)
                .perform(click());
        screenshot("After Patient Clicked");
    }
}
