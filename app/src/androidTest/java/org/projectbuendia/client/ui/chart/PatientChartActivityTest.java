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
import android.support.test.espresso.web.webdriver.Locator;

import static android.support.test.espresso.web.assertion.WebViewAssertions.webMatches;
import static android.support.test.espresso.web.sugar.Web.onWebView;
import static android.support.test.espresso.web.webdriver.DriverAtoms.findElement;
import static android.support.test.espresso.web.webdriver.DriverAtoms.getText;

import static org.hamcrest.Matchers.containsString;

import android.test.suitebuilder.annotation.MediumTest;
import android.view.View;
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
import org.projectbuendia.client.utils.Utils;

import java.util.UUID;

/** Functional tests for {@link PatientChartActivity}. */
@MediumTest
public class PatientChartActivityTest extends FunctionalTestCase {
    private static final Logger LOG = Logger.create();

    private static final String NO = "○";
    private static final String YES = "●";

    private static final int ROW_HEIGHT = 84;

    public PatientChartActivityTest() {
        super();
    }

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
        // TODO: this method is still producing intermittent errors
        openActionBarOptionsMenu();

        EventBusIdlingResource<FetchXformSucceededEvent> xformIdlingResource =
            new EventBusIdlingResource<FetchXformSucceededEvent>(
                UUID.randomUUID().toString(), mEventBus);
        click(viewWithText("[test] Form"));
        Espresso.registerIdlingResources(xformIdlingResource);

        // Give the form time to be parsed on the client (this does not result in an event firing).
        expectVisibleSoon(viewWithText("Encounter"));
    }

    /** Tests that dismissing a form immediately closes it if no changes have been made. */
    public void testDismissButtonReturnsImmediatelyWithNoChanges() {
        inUserLoginGoToDemoPatientChart();
        openEncounterForm();
        click(viewWithText("Discard"));
    }

    /** Tests that dismissing a form results in a dialog if changes have been made. */
    public void testDismissButtonShowsDialogWithChanges() {
        inUserLoginGoToDemoPatientChart();
        openEncounterForm();
        answerTextQuestion("Temperature", "29.2");

        // Try to discard and give up.
        click(viewWithText("Discard"));
        expectVisible(viewWithText(R.string.title_discard_observations));
        click(viewWithText(R.string.no));

        // Try to discard and actually go back.
        click(viewWithText("Discard"));
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

    private void answerSingleCodedQuestion(String questionText, String answerText) {
        answerCodedQuestion(questionText, answerText, ButtonsSelectOneWidget.class,
            TableWidgetGroup.class);
    }

    private void answerMultipleCodedQuestion(String questionText, String answerText) {
        answerCodedQuestion(questionText, answerText, ButtonsSelectOneWidget.class,
            TableWidgetGroup.class, ODKView.class);
    }

    private void answerCodedQuestion(String questionText, String answerText,
                                     final Class<? extends View>... classes) {
        // Close the soft keyboard before answering any toggle questions -- on rare occasions,
        // if Espresso answers one of these questions and is then instructed to type into another
        // field, the input event will actually be generated as the keyboard is hiding and will be
        // lost, but Espresso won't detect this case.
        Espresso.closeSoftKeyboard();

        scrollToAndClick(viewThat(
            isAnyOf(CheckBox.class, RadioButton.class),
            hasAncestorThat(
                isAnyOf(classes),
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

            String temp = Integer.toString(i + 35) + ".7";
            String respiratoryRate = Integer.toString(i + 80);
            String bpSystolic = Integer.toString(i + 80);
            String bpDiastolic = Integer.toString(i + 100);

            answerTextQuestion("Temperature", temp);
            answerTextQuestion("Respiratory rate", respiratoryRate);
            answerTextQuestion("Blood pressure, systolic", bpSystolic);
            answerTextQuestion("Blood pressure, diastolic", bpDiastolic);
            saveForm();

            waitForProgressFragment();

            // TODO: implement IdlingResource for webview to remove this sleep.
            // Wait a bit for the chart to update it's values.
            try{
                Thread.sleep(30000);
            } catch (InterruptedException e){}

            //checkVitalValueContains("Pulse", pulse);
            checkObservationValueEquals("[test] Temperature (°C)", temp);
            checkObservationValueEquals("[test] Respiratory rate (bpm)", respiratoryRate);
            checkObservationValueEquals("[test] Blood pressure, systolic", bpSystolic);
            checkObservationValueEquals("[test] Blood pressure, diastolic", bpDiastolic);
        }
    }

    private void checkVitalValueContains(String vitalName, String vitalValue) {
        // Check for updated vital view.
        expectVisibleSoon(viewThat(
            hasTextContaining(vitalValue),
            hasSiblingThat(hasTextContaining(vitalName))));
    }

    //TODO: check the todo bellow and remove this commented method
//    private void checkObservationValueEquals(int row, String value, String dateKey) {
//        // TODO/completeness: actually check dateKey
//
//        scrollToAndExpectVisible(viewThat(
//            hasText(value),
//            hasAncestorThat(isInRow(row, ROW_HEIGHT))));
//    }

    /**
     * Look for the informed value on the last cell of the Observation named row.
     * @param obsName the class name added to the tr where the value is. The class name is the
     *                  name of the observation with all non alphanumeric chars replaced by "_".
     * @param value the text inside the table cell.
     */
    private void checkObservationValueEquals(String obsName, String value) {
        String cssSelector = "tr." + Utils.removeUnsafeChars(obsName) + " td:last-child";
        onWebView()
            .withElement(findElement(Locator.CSS_SELECTOR, cssSelector))
            .check(webMatches(getText(), containsString(value)));
    }


    /** Ensures that non-overlapping observations for the same encounter are combined. */
    public void testCombinesNonOverlappingObservationsForSameEncounter() {
        inUserLoginGoToDemoPatientChart();
        waitForProgressFragment();

        // Enter first set of observations for this encounter.
        openEncounterForm();
        answerTextQuestion("Temperature", "36.5");
        answerTextQuestion("Respiratory rate", "23");
        answerTextQuestion("oxygen sat", "95");
        answerTextQuestion("Blood pressure, systolic", "80");
        answerTextQuestion("Blood pressure, diastolic", "100");
        saveForm();

        // Enter second set of observations for this encounter.
        waitForProgressFragment();
        openEncounterForm();
        answerTextQuestion("Weight", "80.4");
        answerTextQuestion("Height", "170");
        answerSingleCodedQuestion("Shock", "Mild");
        answerSingleCodedQuestion("Consciousness", "Responds to voice");
        answerMultipleCodedQuestion("Other symptoms", "Cough");
        saveForm();

        // Enter third set of observations for this encounter.
        waitForProgressFragment();
        openEncounterForm();
        answerSingleCodedQuestion("Hiccups", "No");
        answerSingleCodedQuestion("Headache", "No");
        answerSingleCodedQuestion("Sore throat", "Yes");
        answerSingleCodedQuestion("Heartburn", "No");
        answerSingleCodedQuestion("Pregnant", "Yes");
        answerSingleCodedQuestion("Condition", "Unwell");
        answerTextQuestion("Notes", "Call family");
        saveForm();

        // Check that all values are now visible.
        waitForProgressFragment();
        checkObservationValueEquals("[test] Temperature (°C)", "36");
        checkObservationValueEquals("[test] Respiratory rate (bpm)", "23");
        checkObservationValueEquals("[test] SpO₂ oxygen sat (%)", "95");
        checkObservationValueEquals("[test] Blood pressure, systolic", "80");
        checkObservationValueEquals("[test] Blood pressure, diastolic", "100");
        checkObservationValueEquals("[test] Weight (kg)", "80.4");
        checkObservationValueEquals("[test] Height (cm)", "170");
        checkObservationValueEquals("[test] Shock", "Mild");
        checkObservationValueEquals("[test] Consciousness (AVPU)", "V");
        checkObservationValueEquals("[test] Cough", YES);
        checkObservationValueEquals("[test] Hiccups", NO);
        checkObservationValueEquals("[test] Headache", NO);
        checkObservationValueEquals("[test] Sore throat", YES);
        checkObservationValueEquals("Condition", "2");
        checkObservationValueEquals("[test] Notes", "Call …");
    }

    private void checkObservationSet(int row, String dateKey) {
        // TODO/completeness: actually check dateKey
        scrollToAndExpectVisible(viewThat(
            hasAncestorThat(isInRow(row, ROW_HEIGHT)),
            hasBackground(getActivity().getResources().getDrawable(R.drawable.chart_cell_active))));
    }

    /** Exercises all fields in the encounter form, except for encounter time. */
    public void testEncounter_allFieldsWorkOtherThanEncounterTime() {
        inUserLoginGoToDemoPatientChart();
        waitForProgressFragment();

        openEncounterForm();
        answerTextQuestion("Temperature", "36.5");
        answerTextQuestion("Respiratory rate", "23");
        answerTextQuestion("oxygen sat", "95");
        answerTextQuestion("Blood pressure, systolic", "80");
        answerTextQuestion("Blood pressure, diastolic", "100");
        answerTextQuestion("Weight", "80.5");
        answerTextQuestion("Height", "170");
        answerSingleCodedQuestion("Shock", "Severe");
        answerSingleCodedQuestion("Consciousness", "Unresponsive");
        answerMultipleCodedQuestion("Other symptoms", "Gingivitis");
        answerSingleCodedQuestion("Hiccups", "Unknown");
        answerSingleCodedQuestion("Headache", "Yes");
        answerSingleCodedQuestion("Sore throat", "No");
        answerSingleCodedQuestion("Heartburn", "Yes");
        answerSingleCodedQuestion("Pregnant", "No");
        answerSingleCodedQuestion("Condition", "Confirmed Dead");
        answerTextQuestion("Notes", "Possible malaria.");
        saveForm();

        waitForProgressFragment();
        checkObservationValueEquals("[test] Temperature (°C)", "36.5");
        checkObservationValueEquals("[test] Respiratory rate (bpm)", "23");
        checkObservationValueEquals("[test] SpO₂ oxygen sat (%)", "95");
        checkObservationValueEquals("[test] Blood pressure, systolic", "80");
        checkObservationValueEquals("[test] Blood pressure, diastolic", "100");
        checkObservationValueEquals("[test] Weight (kg)", "80.5");
        checkObservationValueEquals("[test] Height (cm)", "170");
        checkObservationValueEquals("[test] Shock", "Severe");
        checkObservationValueEquals("[test] Consciousness (AVPU)", "U");
        checkObservationValueEquals("[test] Gingivitis", YES);
        checkObservationValueEquals("[test] Hiccups", NO);
        checkObservationValueEquals("[test] Headache", YES);
        checkObservationValueEquals("[test] Sore throat", NO);
        checkObservationValueEquals("Condition", "6");
        checkObservationValueEquals("[test] Notes", "Possi…");

/*
        TODO: for now tests are not checking Vital values. We will implement a Test profile to correct this.
        checkVitalValueContains("Pulse", "80");
        checkVitalValueContains("Respiration", "20");
        checkVitalValueContains("Consciousness", "Responds to voice");
        checkVitalValueContains("Mobility", "Assisted");
        checkVitalValueContains("Diet", "Fluids");
        checkVitalValueContains("Hydration", "Needs ORS");
        checkVitalValueContains("Condition", "5");
        checkVitalValueContains("Pain level", "Severe");
*/
    }
}