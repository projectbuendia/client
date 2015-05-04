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

package org.projectbuendia.client.ui.chart;

import android.test.suitebuilder.annotation.MediumTest;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import com.google.android.apps.common.testing.ui.espresso.Espresso;
import com.google.android.apps.common.testing.ui.espresso.IdlingResource;

import org.projectbuendia.client.R;
import org.projectbuendia.client.events.FetchXformSucceededEvent;
import org.projectbuendia.client.events.SubmitXformSucceededEvent;
import org.projectbuendia.client.events.sync.SyncFinishedEvent;
import org.projectbuendia.client.ui.FunctionalTestCase;
import org.projectbuendia.client.ui.sync.EventBusIdlingResource;
import org.projectbuendia.client.utils.Logger;
import org.projectbuendia.client.widget.DataGridView;
import org.odk.collect.android.views.MediaLayout;
import org.odk.collect.android.views.ODKView;
import org.odk.collect.android.widgets2.group.TableWidgetGroup;
import org.odk.collect.android.widgets2.selectone.ButtonsSelectOneWidget;

import java.util.UUID;

import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.click;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.scrollTo;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.typeText;
import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.matches;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.hasDescendant;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.hasSibling;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isAssignableFrom;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isDescendantOfA;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isDisplayed;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withId;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.AnyOf.anyOf;
import static org.projectbuendia.client.ui.matchers.ViewMatchers.hasBackground;
import static org.projectbuendia.client.ui.matchers.ViewMatchers.inRow;

/** Functional tests for {@link PatientChartActivity}. */
@MediumTest
public class PatientChartActivityTest extends FunctionalTestCase {
    private static final Logger LOG = Logger.create();

    private static final int ROW_HEIGHT = 84;

    public PatientChartActivityTest() {
        super();
    }

    /** Tests that the general condition dialog successfully changes general condition. */
    public void testGeneralConditionDialog_AppliesGeneralConditionChange() {
        inUserLoginGoToDemoPatientChart();
        onView(withId(R.id.patient_chart_vital_general_parent)).perform(click());
        screenshot("General Condition Dialog");
        onView(withText(R.string.status_well)).perform(click());

        // Wait for a sync operation to update the chart.
        EventBusIdlingResource<SyncFinishedEvent> syncFinishedIdlingResource =
                new EventBusIdlingResource<>(UUID.randomUUID().toString(), mEventBus);
        Espresso.registerIdlingResources(syncFinishedIdlingResource);

        // Check for updated vital view.
        checkViewDisplayedSoon(withText(R.string.status_well));

        // Check for updated chart view.
        onView(allOf(
                withText(R.string.status_short_desc_well),
                not(withId(R.id.patient_chart_vital_general_condition_number))))
                .check(matches(isDisplayed()));
    }

    /** Tests that the encounter form can be opened more than once. */
    public void testPatientChart_CanOpenEncounterFormMultipleTimes() {
        inUserLoginGoToDemoPatientChart();
        // Load the chart once
        openEncounterForm();

        // Dismiss
        onView(withText("Discard")).perform(click());

        // Load the chart again
        openEncounterForm();

        // Dismiss
        onView(withText("Discard")).perform(click());
    }

    /**
     * Tests that the admission date is correctly displayed in the header.
     * TODO/completeness: Currently disabled. Re-enable once date picker
     * selection works (supposedly works in Espresso 2.0).
     */
    /*public void testPatientChart_ShowsCorrectAdmissionDate() {
        mDemoPatient.admissionDate = Optional.of(DateTime.now().minusDays(5));
        inUserLoginGoToDemoPatientChart();
        onView(allOf(
                isDescendantOfA(withId(R.id.attribute_admission_days)),
                withText("Day 6")))
                .check(matches(isDisplayed()));
        screenshot("Patient Chart");
    }*/

    /**
     * Tests that the patient chart shows the correct symptoms onset date.
     * TODO/completeness: Currently disabled. Re-enable once date picker
     * selection works (supposedly works in Espresso 2.0).
     */
    /*public void testPatientChart_ShowsCorrectSymptomsOnsetDate() {
        inUserLoginGoToDemoPatientChart();
        onView(allOf(
                isDescendantOfA(withId(R.id.attribute_symptoms_onset_days)),
                withText("Day 8")))
                .check(matches(isDisplayed()));
        screenshot("Patient Chart");
    }*/

    /**
     * Tests that the patient chart shows all days, even when no observations are present.
     * TODO/completeness: Currently disabled. Re-enable once date picker
     * selection works (supposedly works in Espresso 2.0).
     */
     /*public void testPatientChart_ShowsAllDaysInChartWhenNoObservations() {
        inUserLoginGoToDemoPatientChart();
        onView(withText(containsString("Today (Day 6)"))).check(matchesWithin(isDisplayed(), 5000));
        screenshot("Patient Chart");
    }*/

    // TODO/completeness: Disabled as there seems to be no easy way of
    // scrolling correctly with no adapter view.
    /** Tests that encounter time can be set to a date in the past and still displayed correctly. */
    /*public void testCanSubmitObservationsInThePast() {
        inUserLoginGoToDemoPatientChart();
        openEncounterForm();
        selectDateFromDatePicker("2015", "Jan", null);
        answerVisibleTextQuestion("Temperature", "29.1");
        saveForm();
        checkObservationValueEquals(0, "29.1", "1 Jan"); // Temperature
    }*/

    /** Tests that dismissing a form immediately closes it if no changes have been made. */
    public void testDismissButtonReturnsImmediatelyWithNoChanges() {
        inUserLoginGoToDemoPatientChart();
        openEncounterForm();
        discardForm();
    }

    /** Tests that dismissing a form results in a dialog if changes have been made. */
    public void testDismissButtonShowsDialogWithChanges() {
        inUserLoginGoToDemoPatientChart();
        openEncounterForm();
        answerVisibleTextQuestion("Temperature", "29.2");

        // Try to discard and give up.
        discardForm();
        onView(withText(R.string.title_discard_observations)).check(matches(isDisplayed()));
        onView(withText(R.string.no)).perform(click());

        // Try to discard and actually go back.
        discardForm();
        onView(withText(R.string.title_discard_observations)).check(matches(isDisplayed()));
        onView(withText(R.string.yes)).perform(click());
    }

    /** Tests that PCR submission does not occur without confirmation being specified. */
    public void testPcr_requiresConfirmation() {
        inUserLoginGoToDemoPatientChart();
        openPcrForm();
        answerVisibleTextQuestion("Ebola L gene", "38");
        answerVisibleTextQuestion("Ebola Np gene", "35");

        onView(withText("Save")).perform(click());

        // Saving form should not work (can't check for a Toast within Espresso)
        onView(withText(R.string.form_entry_save)).check(matches(isDisplayed()));

        // Try again with confirmation
        answerVisibleToggleQuestion("confirm this lab test result", "Confirm Lab Test Results");
        saveForm();

        // Check that new values displayed.
        checkViewDisplayedSoon(withText(containsString("38.0 / 35.0")));
    }

    /** Tests that PCR displays 'NEG' in place of numbers when 40.0 is specified. */
    public void testPcr_showsNegFor40() {
        inUserLoginGoToDemoPatientChart();
        openPcrForm();
        answerVisibleTextQuestion("Ebola L gene", "40");
        answerVisibleTextQuestion("Ebola Np gene", "40");
        answerVisibleToggleQuestion("confirm this lab test result", "Confirm Lab Test Results");
        saveForm();

        checkViewDisplayedSoon(withText(containsString("NEG / NEG")));
    }

    /**
     * Tests that, when multiple encounters for the same encounter time are submitted within a short
     * period of time, that only the latest encounter is present in the relevant column.
     */
    public void testEncounter_latestEncounterIsAlwaysShown() {
        inUserLoginGoToDemoPatientChart();

        // Update a vital tile (pulse) as well as a couple of observations (temperature, vomiting
        // count), and verify that the latest value is visible for each.
        for (int i = 0; i < 6; i++) {
            openEncounterForm();

            String pulse = Integer.toString(i + 80);
            String temp = Integer.toString(i + 35) + ".0";
            String vomiting = Integer.toString(5 - i);
            answerVisibleTextQuestion("Pulse", pulse);
            answerVisibleTextQuestion("Temperature", temp);
            answerVisibleTextQuestion("Vomiting", vomiting);
            saveForm();

            checkVitalValueContains("Pulse", pulse);
            checkObservationValueEquals(0 /*Temperature*/, temp, "Today");
            checkObservationValueEquals(6 /*Vomiting*/, vomiting, "Today");
        }
    }

    /** Ensures that non-overlapping observations for the same encounter are combined. */
    public void testCombinesNonOverlappingObservationsForSameEncounter() {
        inUserLoginGoToDemoPatientChart();
        // Enter first set of observations for this encounter.
        openEncounterForm();
        answerVisibleTextQuestion("Pulse", "74");
        answerVisibleTextQuestion("Respiratory rate", "23");
        answerVisibleTextQuestion("Temperature", "36");
        saveForm();
        // Enter second set of observations for this encounter.
        openEncounterForm();
        answerVisibleToggleQuestion("Signs and Symptoms", "Nausea");
        answerVisibleTextQuestion("Vomiting", "2");
        answerVisibleTextQuestion("Diarrhoea", "5");
        saveForm();

        // Check that all values are now visible.
        checkVitalValueContains("Pulse", "74");
        checkVitalValueContains("Respiration", "23");
        checkObservationValueEquals(0, "36.0", "Today"); // Temp
        checkObservationSet(5, "Today"); // Nausea
        checkObservationValueEquals(6, "2", "Today"); // Vomiting
        checkObservationValueEquals(7, "5", "Today"); // Diarrhoea
    }

    /** Exercises all fields in the encounter form, except for encounter time. */
    public void testEncounter_allFieldsWorkOtherThanEncounterTime() {
        // TODO/robustness: Get rid of magic numbers in these tests.
        inUserLoginGoToDemoPatientChart();
        openEncounterForm();
        answerVisibleTextQuestion("Pulse", "80");
        answerVisibleTextQuestion("Respiratory rate", "20");
        answerVisibleTextQuestion("Temperature", "31");
        answerVisibleTextQuestion("Weight", "90");
        answerVisibleToggleQuestion("Signs and Symptoms", "Nausea");
        answerVisibleTextQuestion("Vomiting", "4");
        answerVisibleTextQuestion("Diarrhoea", "6");
        answerVisibleToggleQuestion("Pain level", "Severe");
        answerVisibleToggleQuestion("Pain (Detail)", "Headache");
        answerVisibleToggleQuestion("Pain (Detail)", "Back pain");
        answerVisibleToggleQuestion("Bleeding", "Yes");
        answerVisibleToggleQuestion("Bleeding (Detail)", "Nosebleed");
        answerVisibleToggleQuestion("Weakness", "Moderate");
        answerVisibleToggleQuestion("Other Symptoms", "Red eyes");
        answerVisibleToggleQuestion("Other Symptoms", "Hiccups");
        answerVisibleToggleQuestion("Consciousness", "Responds to voice");
        answerVisibleToggleQuestion("Mobility", "Assisted");
        answerVisibleToggleQuestion("Diet", "Fluids");
        answerVisibleToggleQuestion("Hydration", "Needs ORS");
        answerVisibleToggleQuestion("Condition", "5");
        answerVisibleToggleQuestion("Additional Details", "Pregnant");
        answerVisibleToggleQuestion("Additional Details", "IV access present");
        answerVisibleTextQuestion("Notes", "possible malaria");
        saveForm();

        checkVitalValueContains("Pulse", "80");
        checkVitalValueContains("Respiration", "20");
        checkVitalValueContains("Consciousness", "Responds to voice");
        checkVitalValueContains("Mobility", "Assisted");
        checkVitalValueContains("Diet", "Fluids");
        checkVitalValueContains("Hydration", "Needs ORS");
        checkVitalValueContains("Condition", "5");
        checkVitalValueContains("Pain level", "Severe");

        checkObservationValueEquals(0, "31.0", "Today"); // Temp
        checkObservationValueEquals(1, "90", "Today"); // Weight
        checkObservationValueEquals(2, "5", "Today"); // Condition
        checkObservationValueEquals(3, "V", "Today"); // Consciousness
        checkObservationValueEquals(4, "As", "Today"); // Mobility
        checkObservationSet(5, "Today"); // Nausea
        checkObservationValueEquals(6, "4", "Today"); // Vomiting
        checkObservationValueEquals(7, "6", "Today"); // Diarrhoea
        checkObservationValueEquals(8, "3", "Today"); // Pain level
        checkObservationSet(9, "Today"); // Bleeding
        checkObservationValueEquals(10, "2", "Today"); // Weakness
        checkObservationSet(13, "Today"); // Hiccups
        checkObservationSet(14, "Today"); // Red eyes
        checkObservationSet(15, "Today"); // Headache
        checkObservationSet(21, "Today"); // Back pain
        checkObservationSet(24, "Today"); // Nosebleed

        onView(withText(containsString("Pregnant"))).check(matches(isDisplayed()));
        onView(withText(containsString("IV Fitted"))).check(matches(isDisplayed()));

        // TODO/completeness: exercise the Notes field
    }

    protected void openEncounterForm() {
        checkViewDisplayedSoon(withId(R.id.action_update_chart));
        EventBusIdlingResource<FetchXformSucceededEvent> xformIdlingResource =
                new EventBusIdlingResource<FetchXformSucceededEvent>(
                        UUID.randomUUID().toString(),
                        mEventBus);
        onView(withId(R.id.action_update_chart)).perform(click());
        Espresso.registerIdlingResources(xformIdlingResource);

        // Give the form time to be parsed on the client (this does not result in an event firing).
        checkViewDisplayedSoon(withText("Encounter"));
    }

    protected void openPcrForm() {
        EventBusIdlingResource<FetchXformSucceededEvent> xformIdlingResource =
                new EventBusIdlingResource<FetchXformSucceededEvent>(
                        UUID.randomUUID().toString(),
                        mEventBus);
        onView(withId(R.id.action_add_test_result)).perform(click());
        Espresso.registerIdlingResources(xformIdlingResource);

        // Give the form time to be parsed on the client (this does not result in an event firing).
        checkViewDisplayedSoon(withText("Encounter"));
    }

    private void discardForm() {
        onView(withText("Discard")).perform(click());
    }

    private void saveForm() {
        IdlingResource xformWaiter = getXformSubmissionIdlingResource();
        onView(withText("Save")).perform(click());
        Espresso.registerIdlingResources(xformWaiter);
    }

    private void answerVisibleTextQuestion(String questionText, String answerText) {
        onView(allOf(
                isAssignableFrom(EditText.class),
                hasSibling(allOf(
                        isAssignableFrom(MediaLayout.class),
                        hasDescendant(allOf(
                                isAssignableFrom(TextView.class),
                                withText(containsString(questionText))))))))
                .perform(scrollTo(), typeText(answerText));
    }

    private void answerVisibleToggleQuestion(String questionText, String answerText) {
        // Close the soft keyboard before answering any toggle questions -- on rare occasions,
        // if Espresso answers one of these questions and is then instructed to type into another
        // field, the input event will actually be generated as the keyboard is hiding and will be
        // lost, but Espresso won't detect this case.
        Espresso.closeSoftKeyboard();
        onView(allOf(
                anyOf(isAssignableFrom(CheckBox.class), isAssignableFrom(RadioButton.class)),
                isDescendantOfA(allOf(
                        anyOf(
                                isAssignableFrom(ButtonsSelectOneWidget.class),
                                isAssignableFrom(TableWidgetGroup.class),
                                isAssignableFrom(ODKView.class)),
                        hasDescendant(withText(containsString(questionText))))),
                withText(containsString(answerText))))
                .perform(scrollTo(), click());
    }

    private void checkObservationValueEquals(int row, String value, String dateKey) {
        // TODO/completeness: actually check dateKey

        onView(allOf(
                withText(value),
                isDescendantOfA(inRow(row, ROW_HEIGHT)),
                isDescendantOfA(isAssignableFrom(DataGridView.LinkableRecyclerView.class))))
                .perform(scrollTo())
                .check(matches(isDisplayed()));
    }

    private void checkObservationSet(int row, String dateKey) {
        // TODO/completeness: actually check dateKey
        onView(allOf(
                isDescendantOfA(inRow(row, ROW_HEIGHT)),
                hasBackground(
                        getActivity().getResources().getDrawable(R.drawable.chart_cell_active)),
                isDescendantOfA(isAssignableFrom(DataGridView.LinkableRecyclerView.class))))
                .perform(scrollTo())
                .check(matches(isDisplayed()));
    }

    private void checkVitalValueContains(String vitalName, String vitalValue) {
        // Check for updated vital view.
        checkViewDisplayedSoon(allOf(
                withText(containsString(vitalValue)),
                hasSibling(withText(containsString(vitalName)))));
    }

    private IdlingResource getXformSubmissionIdlingResource() {
        return new EventBusIdlingResource<SubmitXformSucceededEvent>(
                UUID.randomUUID().toString(),
                mEventBus);
    }
}
