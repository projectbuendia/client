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

package org.msf.records.ui.sync;

import com.google.android.apps.common.testing.ui.espresso.Espresso;
import com.google.common.base.Optional;

import org.joda.time.DateTime;
import org.msf.records.R;
import org.msf.records.data.app.AppPatient;
import org.msf.records.data.app.AppPatientDelta;
import org.msf.records.events.FetchXformSucceededEvent;
import org.msf.records.net.model.Patient;

import java.util.UUID;

import static com.google.android.apps.common.testing.ui.espresso.Espresso.onData;
import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.click;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withId;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

/** Tests the loading of the encounter xform from the patient chart activity. */
public class PatientChartActivityXformSyncTest extends SyncTestCase {
    @Override
    public void setUp() throws Exception {
        super.setUp();

        onView(withText("Guest User")).perform(click());
    }

    /**
     * Tests that clicking the load xform button after a fresh sync causes the xform to
     * eventually load.
     */
    public void testXformRetrievedFromServer() {
        loadChart();
        screenshot("Patient Chart");
        EventBusIdlingResource<FetchXformSucceededEvent> xformIdlingResource =
                new EventBusIdlingResource<FetchXformSucceededEvent>(
                        UUID.randomUUID().toString(),
                        mEventBus);
        onView(withId(R.id.action_update_chart)).perform(click());
        Espresso.registerIdlingResources(xformIdlingResource);
        // This check is known to be particularly flaky.
        checkViewDisplayedWithin(withText("Encounter"), 45000);
        screenshot("Xform Loaded");
        onView(withText(R.string.form_entry_discard)).perform(click());
    }

    private void loadChart() {
        waitForProgressFragment();
        // Open patient list.
        onView(withId(R.id.action_search)).perform(click());
        // waitForProgressFragment() doesn't quite work here as we're actually waiting on the
        // search button in the action bar to finish its loading task.
        checkViewDisplayedSoon(withText(containsString("Triage (")));
        // Click first patient.
        onData(is(AppPatient.class))
                .inAdapterView(withId(R.id.fragment_patient_list))
                .atPosition(0)
                .perform(click());
    }

    private AppPatientDelta getBasicDemoPatient() {
        AppPatientDelta newPatient = new AppPatientDelta();
        newPatient.familyName = Optional.of("XformSyncTest");
        newPatient.givenName = Optional.of("TestPatientFor");
        newPatient.gender = Optional.of(Patient.GENDER_FEMALE);
        newPatient.id = Optional.of(UUID.randomUUID().toString().substring(30));
        newPatient.birthdate = Optional.of(DateTime.now().minusYears(12).minusMonths(3));
        return newPatient;
    }
}
