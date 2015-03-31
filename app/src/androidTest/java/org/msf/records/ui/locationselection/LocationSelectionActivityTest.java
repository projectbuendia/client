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
import org.msf.records.ui.FunctionalTestCase;

import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.Espresso.pressBack;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.click;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withId;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withText;

/** Tests for {@link LocationSelectionActivity}. */
public class LocationSelectionActivityTest extends FunctionalTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    /** Looks for the expected zones and tents. */
    public void testZonesAndTentsDisplayed() {
        inUserLoginGoToLocationSelection();
        inLocationSelectionCheckZonesAndTentsDisplayed();
    }

    /** Tests that zones and tents are still displayed after returning from round view. */
    public void testZonesAndTentsDisplayed_afterRoundView() {
        inUserLoginGoToLocationSelection();
        inLocationSelectionClickLocation("S1");
        pressBack();
        inLocationSelectionCheckZonesAndTentsDisplayed();
    }

    /** Tests that zones and tents are still displayed after returning from list view. */
    public void testZonesAndTentsDisplayed_afterPatientListView() {
        inUserLoginGoToLocationSelection();
        inLocationSelectionClickLocation("ALL PRESENT PATIENTS");
        pressBack();
        inLocationSelectionCheckZonesAndTentsDisplayed();
    }

    /** Tests that zones and tents are still displayed after returning from settings view. */
    public void testZonesAndTentsDisplayed_afterSettingsView() {
        inUserLoginGoToLocationSelection();

        // Enter settings view and return.
        onView(withText("GU")).perform(click());
        onView(withId(R.id.button_settings)).perform(click());
        pressBack();
        inLocationSelectionCheckZonesAndTentsDisplayed();
    }

    /** Tests that zones and tents are still displayed after returning from chart view. */
    public void testZonesAndTentsDisplayed_afterChartView() {
        inUserLoginInitDemoPatient();
        inUserLoginGoToLocationSelection();
        inLocationSelectionClickLocation("S1");
        inPatientListClickFirstPatient(); // open patient chart

        pressBack(); // back to search fragment
        pressBack(); // back to tent selection screen
        inLocationSelectionCheckZonesAndTentsDisplayed();
    }

    /** Tests that zones and tents are still displayed after changing a patient's location. */
    public void testZonesAndTentsDisplayed_afterPatientLocationChanged() {
        inUserLoginInitDemoPatient();
        inUserLoginGoToLocationSelection();
        inLocationSelectionClickLocation("S1");
        inPatientListClickFirstPatient(); // open patient chart

        // Relocate the patient to C1.
        onView(withId(R.id.action_relocate_patient)).perform(click());
        onView(withText("C1")).perform(click());

        pressBack(); // back to search fragment
        pressBack(); // back to tent selection screen
        inLocationSelectionCheckZonesAndTentsDisplayed();

        invalidateDemoPatient(); // don't reuse the relocated patient for future tests
    }
}
