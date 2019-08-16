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

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.projectbuendia.client.R;
import org.projectbuendia.client.models.Patient;
import org.projectbuendia.client.ui.FunctionalTestCase;

import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.is;

/** Tests for {@link FilteredPatientListActivity}. */
public class FilteredPatientListActivityTest extends FunctionalTestCase {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        click(viewWithText("Guest User"));
    }

    /** Opens the patient list. */
    private void openPatientList() {
        throw new UnsupportedOperationException("There's no button to get to the patient list any more.");
    }

    /** Looks for the filter menu. */
    @Test
    @UiThreadTest
    @Ignore
    public void testFilterMenu() {
        getActivity();
        openPatientList();
        screenshot("Test Start");
        click(viewWithText("All present patients"));
        waitForProgressFragment();
        expectVisible(viewWithText("Triage"));
        expectVisible(viewWithText("Pregnant"));
        screenshot("In Filter Menu");
    }

    /** Looks for one zone heading and at least one patient. */
    @Test
    @UiThreadTest
    public void testZoneAndPatientDisplayed() {
        openPatientList();
        screenshot("Test Start");
        // There should be at least one patient in Triage.
        expectVisibleSoon(viewThat(hasTextContaining("Triage  ·  ")));
        // Click the first patient
        click(dataThat(is(Patient.class))
            .inAdapterView(withId(R.id.fragment_patient_list))
            .atPosition(0));
        screenshot("After Patient Clicked");
    }
}
