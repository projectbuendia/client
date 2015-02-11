package org.msf.records.ui.chart;

import android.content.res.Resources;

import com.google.android.apps.common.testing.ui.espresso.Espresso;
import android.test.suitebuilder.annotation.MediumTest;

import com.google.android.apps.common.testing.ui.espresso.matcher.RootMatchers;
import com.google.common.base.Optional;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.msf.records.R;
import org.msf.records.data.app.AppPatient;
import org.msf.records.data.app.AppPatientDelta;
import org.msf.records.events.data.SingleItemCreatedEvent;
import org.msf.records.events.sync.SyncFinishedEvent;
import org.msf.records.net.model.Patient;
import org.msf.records.ui.FunctionalTestCase;
import org.msf.records.ui.sync.EventBusIdlingResource;
import org.msf.records.utils.Logger;

import java.util.UUID;

import static com.google.android.apps.common.testing.ui.espresso.Espresso.onData;
import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.click;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.pressBack;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.typeText;
import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.matches;
import static com.google.android.apps.common.testing.ui.espresso.matcher.RootMatchers.isDialog;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isDisplayed;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withClassName;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withId;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withParent;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.AllOf.allOf;
import static org.msf.records.ui.matchers.AppPatientMatchers.isPatientWithId;

/**
 * Functional test for {@link PatientChartActivity}.
 */
@MediumTest
public class PatientChartActivityTest extends FunctionalTestCase {
    private static final Logger LOG = Logger.create();

    public PatientChartActivityTest() {
        super();
    }

    // TODO: Use proper demo data.
    private final AppPatientDelta mDemoPatient = new AppPatientDelta();

    @Override
    public void setUp() throws Exception {
        super.setUp();
        onView(withText("Guest User")).perform(click());
        populateDemoPatient();
    }

    /** Selects a patient by id from the patient list. */
    private void selectPatient(String id) {
        onData(isPatientWithId(equalToIgnoringCase(id)))
                .inAdapterView(withId(R.id.fragment_patient_list))
                .perform(click());
    }

    /** Tests that the vital views are displayed in patient chart. */
    public void testPatientChart_VitalViewsDisplayed() {
        initWithDemoPatient();
        onView(withText(equalToIgnoringCase("GENERAL CONDITION"))).check(matches(isDisplayed()));
        screenshot("Patient Chart");
    }

    /** Tests that the chart views are displayed in patient chart. */
    public void testPatientChart_ChartViewsDisplayed() {
        initWithDemoPatient();
        onView(withText(equalToIgnoringCase("Weight (kg)"))).check(matches(isDisplayed()));
        screenshot("Patient Chart");
    }

    /** Tests that the general condition dialog successfully changes general condition. */
    public void testGeneralConditionDialog_AppliesGeneralConditionChange() {
        initWithDemoPatient();
        onView(withId(R.id.patient_chart_vital_general_parent)).perform(click());
        screenshot("General Condition Dialog");

        onView(withText(R.string.status_convalescent)).perform(click());
        // Wait for a sync operation to update the chart.
        EventBusIdlingResource<SyncFinishedEvent> syncFinishedIdlingResource =
                new EventBusIdlingResource<SyncFinishedEvent>(
                        UUID.randomUUID().toString(), mEventBus);
        Espresso.registerIdlingResources(syncFinishedIdlingResource);

        // Check for updated vital view.
        onView(withText(R.string.status_convalescent)).check(matches(isDisplayed()));

        // Check for updated chart view.
        onView(allOf(
                withText(R.string.status_short_desc_convalescent),
                not(withId(R.id.patient_chart_vital_general_condition_number))))
                .check(matches(isDisplayed()));
    }

    /**
     * Tests that the admission date is correctly displayed in the header.
     * TODO: Currently disabled. Re-enable once date picker selection works.
     */
    /*public void testPatientChart_ShowsCorrectAdmissionDate() {
        mDemoPatient.admissionDate = Optional.of(DateTime.now().minusDays(5));
        initWithDemoPatient();
        onView(allOf(
                isDescendantOfA(withId(R.id.attribute_admission_days)),
                withText("Day 6")))
                .check(matches(isDisplayed()));
        screenshot("Patient Chart");
    }*/

    /**
     * Tests that the patient chart shows the correct symptoms onset date.
     * TODO: Currently disabled. Re-enable once date picker selection works.
     */
    /*public void testPatientChart_ShowsCorrectSymptomsOnsetDate() {
        initWithDemoPatient();
        onView(allOf(
                isDescendantOfA(withId(R.id.attribute_symptoms_onset_days)),
                withText("Day 8")))
                .check(matches(isDisplayed()));
        screenshot("Patient Chart");
    }*/

    /**
     * Tests that the patient chart shows all days, even when no observations are present.
     * TODO: Currently disabled. Re-enable once date picker selection works.
     */
     /*public void testPatientChart_ShowsAllDaysInChartWhenNoObservations() {
        initWithDemoPatient();
        onView(withText(containsString("Today (Day 6)"))).check(matchesWithin(isDisplayed(), 5000));
        screenshot("Patient Chart");
    }*/

    // TODO: Add significantly more test coverage.

    // TODO: Replace with more extensive, externalized demo data.

    /**
     * Creates a patient matching mDemoPatient on the server and navigates to that patient's chart.
     * Note: this function will not work during {@link #setUp()} as it relies on
     * {@link #waitForProgressFragment()}.
     */
    private void initWithDemoPatient() {
        LOG.i("Adding patient: %s", mDemoPatient.toContentValues().toString());

        onView(withId(R.id.action_add)).perform(click());
        onView(withText("New Patient")).check(matches(isDisplayed()));
        if (mDemoPatient.id.isPresent()) {
            onView(withId(R.id.patient_creation_text_patient_id))
                    .perform(typeText(mDemoPatient.id.get()));
        }
        if (mDemoPatient.givenName.isPresent()) {
            onView(withId(R.id.patient_creation_text_patient_given_name))
                    .perform(typeText(mDemoPatient.givenName.get()));
        }
        if (mDemoPatient.familyName.isPresent()) {
            onView(withId(R.id.patient_creation_text_patient_family_name))
                    .perform(typeText(mDemoPatient.familyName.get()));
        }
        if (mDemoPatient.birthdate.isPresent()) {
            Period age = new Period(mDemoPatient.birthdate.get().toLocalDate(), LocalDate.now());
            if (age.getYears() < 1) {
                onView(withId(R.id.patient_creation_text_age))
                        .perform(typeText(Integer.toString(age.getMonths())));
                onView(withId(R.id.patient_creation_radiogroup_age_units_months)).perform(click());
            } else {
                onView(withId(R.id.patient_creation_text_age))
                        .perform(typeText(Integer.toString(age.getYears())));
                onView(withId(R.id.patient_creation_radiogroup_age_units_years)).perform(click());
            }
        }
        if (mDemoPatient.gender.isPresent()) {
            if (mDemoPatient.gender.get() == AppPatient.GENDER_MALE) {
                onView(withId(R.id.patient_creation_radiogroup_age_sex_male)).perform(click());
            } else if (mDemoPatient.gender.get() == AppPatient.GENDER_FEMALE) {
                onView(withId(R.id.patient_creation_radiogroup_age_sex_female)).perform(click());
            }
        }
        if (mDemoPatient.admissionDate.isPresent()) {
            // TODO: Currently broken -- hopefully fixed by Espresso 2.0.
            // onView(withId(R.id.patient_creation_admission_date)).perform(click());
            // selectDateFromDatePicker(mDemoPatient.admissionDate.get());
        }
        if (mDemoPatient.firstSymptomDate.isPresent()) {
            // TODO: Currently broken -- hopefully fixed by Espresso 2.0.
            // onView(withId(R.id.patient_creation_symptoms_onset_date)).perform(click());
            // selectDateFromDatePicker(mDemoPatient.firstSymptomDate.get());
        }
        if (mDemoPatient.assignedLocationUuid.isPresent()) {
            // TODO: Add support. A little tricky as we need to select by UUID.
            // onView(withId(R.id.patient_creation_button_change_location)).perform(click());
        }

        onView(withId(R.id.patient_creation_button_create)).perform(click());

        // Wait for patient to be created.
        Espresso.registerIdlingResources(
                new EventBusIdlingResource<SingleItemCreatedEvent<AppPatient>>(
                        UUID.randomUUID().toString(),
                        mEventBus
                ));

        // Open patient list.
        onView(withId(R.id.action_search)).perform(click());
        waitForProgressFragment();

        // Select the patient.
        selectPatient(mDemoPatient.id.get());
    }

    // Broken, but hopefully fixed in Espresso 2.0.
    private void selectDateFromDatePicker(DateTime dateTime) {
        String year = dateTime.toString("yyyy");
        String monthOfYear = dateTime.toString("MMM");
        String dayOfMonth = dateTime.toString("dd");

        LOG.e("Year: %s, Month: %s, Day: %s", year, monthOfYear, dayOfMonth);

        setDateSpinner("year", year);
        setDateSpinner("month", monthOfYear);
        setDateSpinner("day", dayOfMonth);
        onView(withText("Set"))
                .inRoot(isDialog())
                .perform(click());

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Broken, but hopefully fixed in Espresso 2.0.
    private void setDateSpinner(String spinnerName, String value) {
        int numberPickerId =
                Resources.getSystem().getIdentifier("numberpicker_input", "id", "android");
        int spinnerId =
                Resources.getSystem().getIdentifier(spinnerName, "id", "android");
        LOG.i("%s: %s", spinnerName, value);
        LOG.i("numberPickerId: %d", numberPickerId);
        LOG.i("spinnerId: %d", spinnerId);
        onView(allOf(withId(numberPickerId), withParent(withId(spinnerId))))
                .inRoot(isDialog())
                .check(matches(isDisplayed()));
                //.perform(click())
                //.perform(typeText(value));
                //.perform(click());
    }

    private void populateDemoPatient() {
        // Setting assigned location during this test is currently unsupported.
        // mDemoPatient.assignedLocationUuid = Optional.of(Zone.TRIAGE_ZONE_UUID);
        mDemoPatient.familyName = Optional.of("ChartActivity");
        mDemoPatient.givenName = Optional.of("TestPatientFor");
        mDemoPatient.firstSymptomDate = Optional.of(LocalDate.now().minusMonths(7));
        mDemoPatient.gender = Optional.of(Patient.GENDER_FEMALE);
        mDemoPatient.id = Optional.of(UUID.randomUUID().toString().substring(30));
        mDemoPatient.birthdate = Optional.of(DateTime.now().minusYears(12).minusMonths(3));
    }
}
