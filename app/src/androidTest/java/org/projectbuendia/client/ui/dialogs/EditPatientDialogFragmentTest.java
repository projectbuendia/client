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

package org.projectbuendia.client.ui.dialogs;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormat;

import org.projectbuendia.client.R;
import org.projectbuendia.client.ui.FunctionalTestCase;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.assertion.ViewAssertions.matches;

/** Tests for adding a new patient. */
public class EditPatientDialogFragmentTest extends FunctionalTestCase {

    /**
     * Tests adding a new patient;
     * Tests adding a location to the patient;
     * Test symptom date;
     * Test last observation date;
     * Test admission date;
     */
    public void testNewPatient() {

        // Create the patient
        String id = inUserLoginGoToDemoPatientChart();

        // Assign a location to the patient
        click(viewWithId(R.id.attribute_location));
        screenshot("After Location Dialog Shown");
        click(viewWithText(LOCATION_NAME));
        screenshot("After Location Selected");

        pressBack();

        // The new patient should be visible in the list for their location
        click(viewWithText(LOCATION_NAME));
        screenshot("In " + LOCATION_NAME);
        inPatientListClickPatientWithId(id);
        screenshot("After Patient Clicked");

        // The symptom onset date should not be assigned a default value.
        expectVisible(viewThat(
            hasAncestorThat(withId(R.id.attribute_symptoms_onset_days)),
            hasText("–")));

        // The admission date should be visible right after adding a patient.
        // Flaky because of potential periodic syncs.
        expectVisibleWithin(399999, viewThat(
            hasAncestorThat(withId(R.id.attribute_admission_days)),
            hasText("Day 1")));

        // The last observation should be today.
        DateTimeFormatter formatter = DateTimeFormat.forPattern("MMM d, yyyy");
        expectVisibleWithin(399999, viewThat(
            withId(R.id.patient_chart_last_observation_date_time),
            hasTextContaining(formatter.print(DateTime.now()))));
    }
}
