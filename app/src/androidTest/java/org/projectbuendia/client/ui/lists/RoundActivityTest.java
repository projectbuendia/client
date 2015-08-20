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
import org.projectbuendia.client.data.app.AppPatient;
import org.projectbuendia.client.ui.FunctionalTestCase;

import static com.google.android.apps.common.testing.ui.espresso.Espresso.onData;
import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.click;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withId;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.is;

/** Test case for {@link SingleLocationActivity}. */
public class RoundActivityTest extends FunctionalTestCase {
    /**
     * Initializes the test by entering the Round view for Triage.
     * @throws Exception if anything goes wrong
     */
    public void setUp() throws Exception {
        super.setUp();
        onView(withText("Guest User")).perform(click());
        waitForProgressFragment();
        onView(withText("Triage")).perform(click());
    }

    /** Checks for a populated title. */
    public void testTitlePopulation() {
        // TODO/completeness: Check that title count actually matches patient count.
        screenshot("Test Start");
        assert getActivity().getTitle().toString().matches("$Triage \\((No|[0-9]+) Patients\\)");
    }

    /** Checks that at least one patient is displayed. */
    public void testAtLeastOnePatientDisplayed() {
        screenshot("Test Start");
        // Click the first patient
        onData(is(AppPatient.class))
                .inAdapterView(withId(R.id.fragment_patient_list))
                .atPosition(0)
                .perform(click());
        screenshot("After Patient Clicked");
    }
}
