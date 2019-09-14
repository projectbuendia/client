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

import android.support.test.annotation.UiThreadTest;

import org.junit.Test;
import org.projectbuendia.client.R;
import org.projectbuendia.client.ui.FunctionalTestCase;

import static android.support.test.espresso.Espresso.pressBack;

/** Tests for {@link LocationListActivity}. */
public class LocationListActivityTest extends FunctionalTestCase {

    private static final String ALL_PATIENTS_LABEL = "ALL PATIENTS";

    /** Looks for the expected zones and tents. */
    @Test
    @UiThreadTest
    public void testZonesAndTentsDisplayed() {
        inUserLoginGoToLocationSelection();
        inLocationSelectionCheckZonesAndTentsDisplayed();
    }

    /** Tests that zones and tents are still displayed after returning from round view. */
    @Test
    @UiThreadTest
    public void testZonesAndTentsDisplayed_afterRoundView() {
        inUserLoginGoToLocationSelection();
        inLocationSelectionClickLocation(LOCATION_NAME);
        pressBack();
        inLocationSelectionCheckZonesAndTentsDisplayed();
    }

    /** Tests that zones and tents are still displayed after returning from list view. */
    @Test
    @UiThreadTest
    public void testZonesAndTentsDisplayed_afterPatientListView() {
        inUserLoginGoToLocationSelection();
        // TODO/i18n: Use a string resource instead of the literal button text.
        inLocationSelectionClickLocation(ALL_PATIENTS_LABEL);
        pressBack();
        inLocationSelectionCheckZonesAndTentsDisplayed();
    }

    /** Tests that zones and tents are still displayed after returning from settings view. */
    @Test
    @UiThreadTest
    public void testZonesAndTentsDisplayed_afterSettingsView() {
        inUserLoginGoToLocationSelection();

        // Enter settings view and return.
        click(firstViewWithText("GU"));
        click(viewWithId(R.id.button_settings));
        pressBack();
        inLocationSelectionCheckZonesAndTentsDisplayed();
    }

    /** Tests that zones and tents are still displayed after returning from chart view. */
    @Test
    @UiThreadTest
    public void testZonesAndTentsDisplayed_afterChartView() {
        inUserLoginGoToPatientCreation();
        screenshot("Test Start");
        String id = generateId();
        populateNewPatientFields(id);
        click(viewWithText("OK"));
        waitForProgressFragment();
        pressBack(); // back to location selection screen
        inLocationSelectionCheckZonesAndTentsDisplayed();
    }

    /** Tests that zones and tents are still displayed after changing a patient's location. */
    @Test
    @UiThreadTest
    public void testZonesAndTentsDisplayed_afterPatientLocationChanged() {
        inUserLoginGoToPatientCreation();
        screenshot("Test Start");
        String id = generateId();
        populateNewPatientFields(id);
        click(viewWithText("OK"));
        waitForProgressFragment();
        screenshot("On Patient Chart");

        // Assign a location to the patient
        clickElementMatchingSelector(".tile.location");
        screenshot("After Location Dialog Shown");
        click(viewWithText(LOCATION_NAME));
        screenshot("After Location Selected");
        click(viewWithText("OK"));

        pressBack(); // back to location selection screen
        inLocationSelectionCheckZonesAndTentsDisplayed();
    }
}
