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

import android.support.annotation.IdRes;
import android.support.test.annotation.UiThreadTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.projectbuendia.client.R;
import org.projectbuendia.client.ui.FunctionalTestCase;
import org.projectbuendia.client.ui.chart.PatientChartActivity;

import androidx.test.filters.SmallTest;

import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static junit.framework.TestCase.assertTrue;

/** Tests for adding a new patient. */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class EditPatientDialogFragmentTest extends FunctionalTestCase {

    /**
     * Tests adding a new patient;
     * Tests initial location of the patient;
     * Test symptom date;
     * Test last observation date;
     * Test admission date;
     * Tests adding a location to the patient;
     */
    @Test
    @UiThreadTest
    public void testNewPatient() {
        // Create the patient
        String id = inUserLoginGoToDemoPatientChart();

        waitUntilVisible(viewThat(hasTextContaining("Triage")));

        // The symptom onset date should not be assigned a default value.
        expectVisible(viewThat(
                hasAncestorThat(withId(R.id.attribute_symptoms_onset_days)),
                hasText("–")));

        waitUntilVisible(viewThat(
                hasAncestorThat(withId(R.id.attribute_admission_days)),
                hasText("Day 1")));

        // Assign a location to the patient
        click(viewWithId(R.id.attribute_location));
        screenshot("After Location Dialog Shown");
        click(viewWithText(LOCATION_NAME));
        screenshot("After Location Selected");

        waitUntilVisible(viewThat(hasTextContaining(LOCATION_NAME)));
    }

    @Test
    @UiThreadTest
    public void testPatientCreation() throws Throwable {
        inUserLoginGoToPatientCreation();
        screenshot("Test Start");
        String id = generateId();
        // Populate the patient info
        screenshot("Before Patient Populated");
        String given = "Giv" + id.substring(id.length() - 2);
        String family = "Fam" + id.substring(id.length() - 4, id.length() - 2);
        type(id, viewWithId(R.id.patient_id));
        type(given, viewWithId(R.id.patient_given_name));
        type(family, viewWithId(R.id.patient_family_name));
        type(4, viewWithId(R.id.patient_age_years));
        type(2, viewWithId(R.id.patient_age_months));
        boolean female = Integer.parseInt(id) % 2 == 0;
        @IdRes int sexButton = female ? R.id.patient_sex_female : R.id.patient_sex_male;
        click(viewWithId(sexButton));
        screenshot("After Patient Populated");
        click(viewWithText("OK"));
        waitForProgressFragment();
        screenshot("On Patient Chart");

        // Make sure we're on a PatientChartActivity
        // TODO(sdspikes): shouldn't this already be on ui thread because of @UiThreadTest annotation?
        runOnUiThread(() -> {
            try {
                assertTrue("Expected PatientChartActivity, got something else",
                        getCurrentActivity() instanceof PatientChartActivity);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        });

        // Now read off the patient info and check that it's all there.
        // It should all be in the action bar.
        expectVisible(viewThat(hasTextContaining(id + ".")));
        expectVisible(viewThat(hasTextContaining(given)));
        expectVisible(viewThat(hasTextContaining(family)));
        if (female) {
            expectVisible(viewThat(hasTextContaining("F,")));
        } else {
            expectVisible(viewThat(hasTextContaining("M,")));
        }
        expectVisible(viewThat(hasTextContaining((4 * 12 + 2) + " mo")));
    }
}
