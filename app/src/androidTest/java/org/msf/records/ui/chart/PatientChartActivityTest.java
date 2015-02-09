package org.msf.records.ui.chart;

import android.preference.PreferenceManager;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.common.base.Optional;

import org.joda.time.DateTime;
import org.msf.records.App;
import org.msf.records.R;
import org.msf.records.data.app.AppPatientDelta;
import org.msf.records.model.Zone;
import org.msf.records.net.model.Patient;
import org.msf.records.sync.GenericAccountService;
import org.msf.records.ui.FunctionalTestCase;
import org.msf.records.utils.Logger;

import java.util.UUID;

import static com.google.android.apps.common.testing.ui.espresso.Espresso.onData;
import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.click;
import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.matches;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isDisplayed;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withId;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.core.AllOf.allOf;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.Matchers.startsWith;
import static org.msf.records.ui.matchers.AppPatientMatchers.isPatientWithId;
import static org.msf.records.ui.matchers.MetaMatchers.matchesWithin;

/**
 * Functional test for {@link PatientChartActivity}.
 */
public class PatientChartActivityTest extends FunctionalTestCase {
    private static final Logger LOG = Logger.create();

    // TODO: Use proper demo data.
    private String mDemoPatientId;
    private DemoPatientResponseHandler mResponseHandler = new DemoPatientResponseHandler();
    private final AppPatientDelta mDemoPatient = new AppPatientDelta();

    @Override
    public void setUp() throws Exception {
        super.setUp();
        onView(withText("Guest User")).perform(click());
        populateDemoPatient();
    }

    /** Setup steps that cannot be performed during setUp. */
    private void postSetUp() {
        // Wait for tents to load (cannot be done in setUp()).
        waitForProgressFragment();
        // Enter patient list screen.
        onView(withText("ALL PRESENT PATIENTS")).perform(click());
        // Wait for patient list to load (cannot be done in setUp()).
        waitForProgressFragment();
        screenshot("In Patient List");
    }

    /** Selects a patient by id from the patient list. */
    private void selectPatient(String id) {
        onData(isPatientWithId(equalToIgnoringCase(id)))
                .inAdapterView(withId(R.id.fragment_patient_list))
                .perform(click());
    }

    /** Tests that the vital views are displayed in patient chart. */
    public void testPatientChart_VitalViewsDisplayed() {
        submitDemoPatient();
        postSetUp();
        selectPatient(mDemoPatientId);
        onView(withText(equalToIgnoringCase("GENERAL CONDITION"))).check(matches(isDisplayed()));
        screenshot("Patient Chart");
    }

    /** Tests that the chart views are displayed in patient chart. */
    public void testPatientChart_ChartViewsDisplayed() {
        submitDemoPatient();
        postSetUp();
        selectPatient(mDemoPatientId);
        onView(withText(equalToIgnoringCase("Weight (kg)"))).check(matches(isDisplayed()));
        screenshot("Patient Chart");
    }

    /** Tests that the admission date is correctly displayed in the header. */
    public void testPatientChart_ShowsCorrectAdmissionDate() {
        mDemoPatient.admissionDate = Optional.of(DateTime.now().minusDays(5));
        submitDemoPatient();
        postSetUp();
        selectPatient(mDemoPatientId);
        onView(withText("Day 6")).check(matches(isDisplayed()));
        screenshot("Patient Chart");
    }

    /** Tests that the admission date is not shown if not explicitly set. */
    public void testPatientChart_HidesMissingAdmissionDate() {
        submitDemoPatient();
        postSetUp();
        selectPatient(mDemoPatientId);
        onView(withText("Day 1")).check(matches(not(isDisplayed())));
        screenshot("Patient Chart");
    }

    /**
     * Tests that the 'day' counter is not shown in the patient chart if admission date isn't
     * set.
     */
    public void testPatientChart_HidesDayCounterForMissingAdmissionDate() {
        submitDemoPatient();
        postSetUp();
        selectPatient(mDemoPatientId);
        onView(withText(containsString("Today\n"))).check(matches(isDisplayed()));
        screenshot("Patient Chart");
    }

    /** Tests that the patient chart shows the correct symptoms onset date. */
    public void testPatientChart_ShowsCorrectSymptomsOnsetDate() {
        postSetUp();
        selectPatient(mDemoPatientId);
        onView(withText("Day 8")).check(matches(isDisplayed()));
        screenshot("Patient Chart");
    }

    /** Tests that the patient chart shows all days, even when no observations are present. */
    public void testPatientChart_ShowsAllDaysInChartWhenNoObservations() {
        postSetUp();
        selectPatient(mDemoPatientId);
        onView(withText(containsString("Today (Day 6)"))).check(matchesWithin(isDisplayed(), 5000));
        screenshot("Patient Chart");
    }

    // TODO: Replace with more extensive, externalized demo data.
    private void submitDemoPatient() {
        LOG.i("Adding patient: %s", mDemoPatient.toContentValues().toString());
        App.getServer().addPatient(mDemoPatient, mResponseHandler, mResponseHandler);
    }

    private void populateDemoPatient() {
        mDemoPatient.assignedLocationUuid = Optional.of(Zone.TRIAGE_ZONE_UUID);
        mDemoPatient.familyName = Optional.of("ChartActivity");
        mDemoPatient.givenName = Optional.of("TestPatientFor");
        mDemoPatient.firstSymptomDate = Optional.of(DateTime.now().minusDays(7));
        mDemoPatient.gender = Optional.of(Patient.GENDER_FEMALE);
        mDemoPatient.id = Optional.of(UUID.randomUUID().toString().substring(30));
        mDemoPatient.birthdate = Optional.of(DateTime.now().minusYears(12).minusMonths(3));
    }

    private final class DemoPatientResponseHandler
            implements Response.Listener<Patient>, Response.ErrorListener {
        @Override
        public void onResponse(Patient response) {
            mDemoPatientId = response.id;
            LOG.i("Patient successfully added to server: %s", response.toString());

            // Force the app to sync so we see the new patient right away.

            LOG.i("Forcing a sync to pick up new patient");
            GenericAccountService.triggerRefresh(
                    PreferenceManager.getDefaultSharedPreferences(App.getInstance()));
            waitForInitialSync();
        }

        @Override
        public void onErrorResponse(VolleyError error) {
            LOG.e("Failed to create demo data for test", error);
            fail("Failed to create demo patient.");
        }
    }
}
