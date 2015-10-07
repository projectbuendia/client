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

import android.support.test.espresso.Espresso;
import android.support.test.espresso.IdlingResource;
import android.test.suitebuilder.annotation.MediumTest;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;

import org.odk.collect.android.views.MediaLayout;
import org.odk.collect.android.views.ODKView;
import org.odk.collect.android.widgets2.group.TableWidgetGroup;
import org.odk.collect.android.widgets2.selectone.ButtonsSelectOneWidget;
import org.projectbuendia.client.R;
import org.projectbuendia.client.events.FetchXformSucceededEvent;
import org.projectbuendia.client.events.SubmitXformSucceededEvent;
import org.projectbuendia.client.ui.FunctionalTestCase;
import org.projectbuendia.client.ui.sync.EventBusIdlingResource;
import org.projectbuendia.client.utils.Logger;

import java.util.UUID;

/** Functional tests for {@link PatientChartActivity}. */
@MediumTest
public class PatientChartActivityTest extends FunctionalTestCase {
    private static final Logger LOG = Logger.create();

    private static final int ROW_HEIGHT = 84;

    public PatientChartActivityTest() {
        super();
    }

    /**
     * Tests that the general condition dialog successfully changes general condition.
     * TODO/completeness: Currently disabled because this is now rendered in a WebView.
     * A new test needs to be written that interacts with the WebView.
     */
    /*
    public void testGeneralConditionDialog_AppliesGeneralConditionChange() {
        inUserLoginGoToDemoPatientChart();
        click(viewWithId(R.id.patient_chart_vital_general_parent));
        screenshot("General Condition Dialog");
        click(viewWithText(R.string.status_well));

        // Wait for a sync operation to update the chart.
        EventBusIdlingResource<SyncFinishedEvent> syncFinishedIdlingResource =
            new EventBusIdlingResource<>(UUID.randomUUID().toString(), mEventBus);
        Espresso.registerIdlingResources(syncFinishedIdlingResource);

        // Check for updated vital view.
        expectVisibleSoon(viewWithText(R.string.status_well));

        // Check for updated chart view.
        expectVisible(viewThat(
            hasText(R.string.status_short_desc_well),
            not(hasId(R.id.patient_chart_vital_general_condition_number))));
    }
    */

    /** Tests that the encounter form can be opened more than once. */
    public void testPatientChart_CanOpenEncounterFormMultipleTimes() {
        inUserLoginGoToDemoPatientChart();
        // Load the form once and dismiss it
        openEncounterForm();
        click(viewWithText("Discard"));

        // Load the form again and dismiss it
        openEncounterForm();
        click(viewWithText("Discard"));
    }

    /**
     * Tests that the admission date is correctly displayed in the header.
     * TODO/completeness: Currently disabled. Re-enable once date picker
     * selection works (supposedly works in Espresso 2.0).
     */
    /*public void testPatientChart_ShowsCorrectAdmissionDate() {
        mDemoPatient.admissionDate = Optional.of(DateTime.now().minusDays(5));
        inUserLoginGoToDemoPatientChart();
        expectVisible(viewThat(
                hasAncestorThat(hasId(R.id.attribute_admission_days)),
                hasText("Day 6")));
        screenshot("Patient Chart");
    }*/

    /**
     * Tests that the patient chart shows the correct symptoms onset date.
     * TODO/completeness: Currently disabled. Re-enable once date picker
     * selection works (supposedly works in Espresso 2.0).
     */
    /*public void testPatientChart_ShowsCorrectSymptomsOnsetDate() {
        inUserLoginGoToDemoPatientChart();
        expectVisible(viewThat(
                hasAncestorThat(hasId(R.id.attribute_symptoms_onset_days)),
                hasText("Day 8")));
        screenshot("Patient Chart");
    }*/

    /**
     * Tests that the patient chart shows all days, even when no observations are present.
     * TODO/completeness: Currently disabled. Re-enable once date picker
     * selection works (supposedly works in Espresso 2.0).
     */
     /*public void testPatientChart_ShowsAllDaysInChartWhenNoObservations() {
        inUserLoginGoToDemoPatientChart();
        expectVisibleWithin(5000, viewThat(hasTextContaining("Today (Day 6)")));
        screenshot("Patient Chart");
    }*/

    // TODO/completeness: Disabled as there seems to be no easy way of
    // scrolling correctly with no adapter view.

    /** Tests that encounter time can be set to a date in the past and still displayed correctly. */
    /*public void testCanSubmitObservationsInThePast() {
        inUserLoginGoToDemoPatientChart();
        openEncounterForm();
        selectDateFromDatePicker("2015", "Jan", null);
        answerTextQuestion("Temperature", "29.1");
        saveForm();
        checkObservationValueEquals(0, "29.1", "1 Jan"); // Temperature
    }*/
    protected void openEncounterForm() {
        openActionBarOptionsMenu();

        EventBusIdlingResource<FetchXformSucceededEvent> xformIdlingResource =
            new EventBusIdlingResource<FetchXformSucceededEvent>(
                UUID.randomUUID().toString(),
                mEventBus);
        click(viewWithText("[test] Form"));
        Espresso.registerIdlingResources(xformIdlingResource);

        // Give the form time to be parsed on the client (this does not result in an event firing).
        expectVisibleSoon(viewWithText("Encounter"));
    }

    /** Tests that dismissing a form immediately closes it if no changes have been made. */
    public void testDismissButtonReturnsImmediatelyWithNoChanges() {
        inUserLoginGoToDemoPatientChart();
        openEncounterForm();
        discardForm();
    }

    private void discardForm() {
        click(viewWithText("Discard"));
    }

    /** Tests that dismissing a form results in a dialog if changes have been made. */
    public void testDismissButtonShowsDialogWithChanges() {
        inUserLoginGoToDemoPatientChart();
        openEncounterForm();
        answerTextQuestion("Temperature", "29.2");

        // Try to discard and give up.
        discardForm();
        expectVisible(viewWithText(R.string.title_discard_observations));
        click(viewWithText(R.string.no));

        // Try to discard and actually go back.
        discardForm();
        expectVisible(viewWithText(R.string.title_discard_observations));
        click(viewWithText(R.string.yes));
    }

    private void answerTextQuestion(String questionText, String answerText) {
        scrollToAndType(answerText, viewThat(
            isA(EditText.class),
            hasSiblingThat(
                isA(MediaLayout.class),
                hasDescendantThat(hasTextContaining(questionText)))));
    }

    /** Tests that PCR submission does not occur without confirmation being specified. */
    public void testPcr_requiresConfirmation() {
        inUserLoginGoToDemoPatientChart();
        openPcrForm();
        answerTextQuestion("Ebola L gene", "38");
        answerTextQuestion("Ebola Np gene", "35");

        click(viewWithText("Save"));

        // Saving form should not work (can't check for a Toast within Espresso)
        expectVisible(viewWithText(R.string.form_entry_save));

        // Try again with confirmation
        answerCodedQuestion("confirm this lab test result", "Confirm Lab Test Results");
        saveForm();

        // Check that new values displayed.
        expectVisibleSoon(viewThat(hasTextContaining("38.0 / 35.0")));
    }

    protected void openPcrForm() {
        EventBusIdlingResource<FetchXformSucceededEvent> xformIdlingResource =
            new EventBusIdlingResource<FetchXformSucceededEvent>(
                UUID.randomUUID().toString(),
                mEventBus);
        click(viewWithId(R.id.attribute_pcr));
        Espresso.registerIdlingResources(xformIdlingResource);

        // Give the form time to be parsed on the client (this does not result in an event firing).
        expectVisibleSoon(viewWithText("Encounter"));
    }

    private void answerCodedQuestion(String questionText, String answerText) {
        // Close the soft keyboard before answering any toggle questions -- on rare occasions,
        // if Espresso answers one of these questions and is then instructed to type into another
        // field, the input event will actually be generated as the keyboard is hiding and will be
        // lost, but Espresso won't detect this case.
        Espresso.closeSoftKeyboard();

        scrollToAndClick(viewThat(
            isAnyOf(CheckBox.class, RadioButton.class),
            hasAncestorThat(
                isAnyOf(ButtonsSelectOneWidget.class, TableWidgetGroup.class, ODKView.class),
                hasDescendantThat(hasTextContaining(questionText))),
            hasTextContaining(answerText)));
    }

    private void saveForm() {
        IdlingResource xformWaiter = getXformSubmissionIdlingResource();
        click(viewWithText("Save"));
        Espresso.registerIdlingResources(xformWaiter);
    }

    private IdlingResource getXformSubmissionIdlingResource() {
        return new EventBusIdlingResource<SubmitXformSucceededEvent>(
            UUID.randomUUID().toString(),
            mEventBus);
    }

    /** Tests that PCR displays 'NEG' in place of numbers when 40.0 is specified. */
    public void testPcr_showsNegFor40() {
        inUserLoginGoToDemoPatientChart();
        openPcrForm();
        answerTextQuestion("Ebola L gene", "40");
        answerTextQuestion("Ebola Np gene", "40");
        answerCodedQuestion("confirm this lab test result", "Confirm Lab Test Results");
        saveForm();

        expectVisibleSoon(viewThat(hasTextContaining("NEG / NEG")));
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
            answerTextQuestion("Pulse", pulse);
            answerTextQuestion("Temperature", temp);
            answerTextQuestion("Vomiting", vomiting);
            saveForm();

            checkVitalValueContains("Pulse", pulse);
            checkObservationValueEquals(0 /*Temperature*/, temp, "Today");
            checkObservationValueEquals(6 /*Vomiting*/, vomiting, "Today");
        }
    }

    private void checkVitalValueContains(String vitalName, String vitalValue) {
        // Check for updated vital view.
        expectVisibleSoon(viewThat(
            hasTextContaining(vitalValue),
            hasSiblingThat(hasTextContaining(vitalName))));
    }

    private void checkObservationValueEquals(int row, String value, String dateKey) {
        // TODO/completeness: actually check dateKey

        scrollToAndExpectVisible(viewThat(
            hasText(value),
            hasAncestorThat(isInRow(row, ROW_HEIGHT))));
    }

    /** Ensures that non-overlapping observations for the same encounter are combined. */
    public void testCombinesNonOverlappingObservationsForSameEncounter() {
        inUserLoginGoToDemoPatientChart();
        // Enter first set of observations for this encounter.
        openEncounterForm();
        answerTextQuestion("Pulse", "74");
        answerTextQuestion("Respiratory rate", "23");
        answerTextQuestion("Temperature", "36.1");
        saveForm();
        // Enter second set of observations for this encounter.
        openEncounterForm();
        answerCodedQuestion("Signs and Symptoms", "Nausea");
        answerTextQuestion("Vomiting", "2");
        answerTextQuestion("Diarrhoea", "5");
        saveForm();

        // Check that all values are now visible.
        checkVitalValueContains("Pulse", "74");
        checkVitalValueContains("Respiration", "23");
        checkObservationValueEquals(0, "36.1", "Today"); // Temp
        checkObservationSet(5, "Today"); // Nausea
        checkObservationValueEquals(6, "2", "Today"); // Vomiting
        checkObservationValueEquals(7, "5", "Today"); // Diarrhoea
    }

    private void checkObservationSet(int row, String dateKey) {
        // TODO/completeness: actually check dateKey
        scrollToAndExpectVisible(viewThat(
            hasAncestorThat(isInRow(row, ROW_HEIGHT)),
            hasBackground(getActivity().getResources().getDrawable(R.drawable.chart_cell_active))));
    }

    /** Exercises all fields in the encounter form, except for encounter time. */
    public void testEncounter_allFieldsWorkOtherThanEncounterTime() {
        // TODO/robustness: Get rid of magic numbers in these tests.
        inUserLoginGoToDemoPatientChart();
        openEncounterForm();
        answerTextQuestion("Pulse", "80");
        answerTextQuestion("Respiratory rate", "20");
        answerTextQuestion("Temperature", "31");
        answerTextQuestion("Weight", "90");
        answerCodedQuestion("Signs and Symptoms", "Nausea");
        answerTextQuestion("Vomiting", "4");
        answerTextQuestion("Diarrhoea", "6");
        answerCodedQuestion("Pain level", "Severe");
        answerCodedQuestion("Pain (Detail)", "Headache");
        answerCodedQuestion("Pain (Detail)", "Back pain");
        answerCodedQuestion("Bleeding", "Yes");
        answerCodedQuestion("Bleeding (Detail)", "Nosebleed");
        answerCodedQuestion("Weakness", "Moderate");
        answerCodedQuestion("Other Symptoms", "Red eyes");
        answerCodedQuestion("Other Symptoms", "Hiccups");
        answerCodedQuestion("Consciousness", "Responds to voice");
        answerCodedQuestion("Mobility", "Assisted");
        answerCodedQuestion("Diet", "Fluids");
        answerCodedQuestion("Hydration", "Needs ORS");
        answerCodedQuestion("Condition", "5");
        answerCodedQuestion("Additional Details", "Pregnant");
        answerCodedQuestion("Additional Details", "IV access present");
        answerTextQuestion("Notes", "possible malaria");
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

        expectVisible(viewThat(hasTextContaining("Pregnant")));
        expectVisible(viewThat(hasTextContaining("IV Fitted")));

        // TODO/completeness: exercise the Notes field
    }
}
