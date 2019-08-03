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

import android.support.test.annotation.UiThreadTest;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.IdlingResource;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.web.webdriver.Locator;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;

import org.junit.Ignore;
import org.junit.Test;
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

import androidx.test.filters.MediumTest;

import static android.support.test.espresso.matcher.ViewMatchers.isJavascriptEnabled;
import static android.support.test.espresso.web.assertion.WebViewAssertions.webMatches;
import static android.support.test.espresso.web.sugar.Web.onWebView;
import static android.support.test.espresso.web.webdriver.DriverAtoms.findElement;
import static android.support.test.espresso.web.webdriver.DriverAtoms.getText;
import static org.hamcrest.Matchers.containsString;

/** Functional tests for {@link PatientChartActivity}. */
@MediumTest
public class PatientChartActivityTest extends FunctionalTestCase {
    private static final Logger LOG = Logger.create();

    private static final String NO = "○";
    private static final String YES = "●";

    private static final String BP_SYS = "BP, systolic";
    private static final String BP_DIA = "BP, diastolic";

    private static final int ROW_HEIGHT = 84;

    private static final String VITALS_FORM = "Vitals and physical exam";
    private static final String ATTRIBUTES_FORM = "Patient attributes";

    public PatientChartActivityTest() {
        super();
    }


    /**
     * Tests that the general condition dialog successfully changes general condition.
     * TODO/completeness: Currently disabled because this is now rendered in a WebView.
     * A new test needs to be written that interacts with the WebView.
     */
    /*
    @Test
    @UiThreadTest
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
    @Test
    @UiThreadTest
    @Ignore
    // TODO(sdspikes): re-enable if it can be made less flaky
    public void testPatientChart_CanOpenEncounterFormMultipleTimes() {
        inUserLoginGoToDemoPatientChart();
        // Load the form once and dismiss it
        openEncounterForm(ATTRIBUTES_FORM);
        click(viewWithText("Discard"));

        try {
            Thread.sleep(5000);
        } catch (InterruptedException ignored) {
        }


        // Load the form again and dismiss it
        openEncounterForm(ATTRIBUTES_FORM);
        click(viewWithText("Discard"));
    }

    /**
     * Tests that the admission date is correctly displayed in the header.
     * TODO/completeness: Currently disabled. Re-enable once date picker
     * selection works (supposedly works in Espresso 2.0).
     */
    /*@UiThreadTest
    public void testPatientChart_ShowsCorrectAdmissionDate() {
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
    /*@UiThreadTest
    public void testPatientChart_ShowsCorrectSymptomsOnsetDate() {
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
     /*@UiThreadTest
    public void testPatientChart_ShowsAllDaysInChartWhenNoObservations() {
        inUserLoginGoToDemoPatientChart();
        expectVisibleWithin(5000, viewThat(hasTextContaining("Today (Day 6)")));
        screenshot("Patient Chart");
    }*/

    // TODO/completeness: Disabled as there seems to be no easy way of
    // scrolling correctly with no adapter view.

    /** Tests that encounter time can be set to a date in the past and still displayed correctly. */
    /*@UiThreadTest
    public void testCanSubmitObservationsInThePast() {
        inUserLoginGoToDemoPatientChart();
        openEncounterForm();
        selectDateFromDatePicker("2015", "Jan", null);
        answerTextQuestion("Temperature", "29.1");
        saveForm();
        checkObservationValueEquals(0, "29.1", "1 Jan"); // Temperature
    }*/

    // TODO(sdspikes): this method is somewhat flaky, the click sometimes doesn't bring up the menu
    protected void openEncounterForm(String menuLabel) {
        // Wait until the edit menu button is available.
        expectVisibleSoon(viewWithId(R.id.action_edit));
        click(viewWithId(R.id.action_edit));

        EventBusIdlingResource<FetchXformSucceededEvent> xformIdlingResource =
                new EventBusIdlingResource<>(UUID.randomUUID().toString(), mEventBus);
        ViewInteraction testForm = viewWithText(menuLabel);
        expectVisibleSoon(testForm);
        click(testForm);
        Espresso.registerIdlingResources(xformIdlingResource);

        // Give the form time to be parsed on the client (this does not result in an event firing).
        expectVisibleSoon(viewWithText("Encounter"));
    }

    /** Tests that dismissing a form immediately closes it if no changes have been made. */
    @Test
    @UiThreadTest
    @Ignore
    public void testDismissButtonReturnsImmediatelyWithNoChanges() {
        inUserLoginGoToDemoPatientChart();
        openEncounterForm(ATTRIBUTES_FORM);
        click(viewWithText("Discard"));
    }

    /** Tests that dismissing a form results in a dialog if changes have been made. */
    @Test
    @UiThreadTest
    public void testDismissButtonShowsDialogWithChanges() {
        inUserLoginGoToDemoPatientChart();
        openEncounterForm(VITALS_FORM);
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
        click(viewWithText("Yes"));
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
    @Test
    @UiThreadTest
    @Ignore
    // TODO(sdspikes): re-enable if it can be made less flaky (menu doesn't always load in time)
    public void testEncounter_latestEncounterIsAlwaysShown() {
        inUserLoginGoToDemoPatientChart();

        // Update a vital tile (pulse) as well as a couple of observations (temperature, vomiting
        // count), and verify that the latest value is visible for each.
        for (int i = 0; i < 3; i++) {
            openEncounterForm(VITALS_FORM);

            String temp = Integer.toString(i + 35) + ".7";
            String respiratoryRate = Integer.toString(i + 80);
            String bpSystolic = Integer.toString(i + 80);
            String bpDiastolic = Integer.toString(i + 100);

            answerTextQuestion("Temperature", temp);
            answerTextQuestion("Respiratory rate", respiratoryRate);
            answerTextQuestion(BP_SYS, bpSystolic);
            answerTextQuestion(BP_DIA, bpDiastolic);
            saveForm();

            waitForProgressFragment();

            // TODO: implement IdlingResource for webview to remove this sleep.
            // Wait a bit for the chart to update it's values.
//            try{
//                Thread.sleep(500);
//            } catch (InterruptedException ignored){}

            //checkVitalValueContains("Pulse", pulse);
//            TODO(sdspikes): debug why these checks always time out
//            checkObservationValueEquals("Temperature (°C)", temp);
//            checkObservationValueEquals("Respiratory rate (bpm)", respiratoryRate);
//            checkObservationValueEquals(BP_SYS, bpSystolic);
//            checkObservationValueEquals(BP_SYS, bpDiastolic);
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
        String cssSelector = "tr." + Utils.toCssIdentifier(obsName) + " td:last-child";
        onWebView()
            .withElement(findElement(Locator.CSS_SELECTOR, cssSelector))
            .check(webMatches(getText(), containsString(value)));
    }


    /** Ensures that non-overlapping observations for the same encounter are combined. */
    @Test
    public void testCombinesNonOverlappingObservationsForSameEncounter() {
        inUserLoginGoToDemoPatientChart();
        waitForProgressFragment();

        // Enter first set of observations for this encounter.
        openEncounterForm(VITALS_FORM);
        answerTextQuestion("Temperature", "36.5");
        answerTextQuestion("Respiratory rate", "23");
        answerTextQuestion("saturation", "95");
        answerTextQuestion(BP_SYS, "80");
        answerTextQuestion(BP_DIA, "100");
        answerMultipleCodedQuestion("Other symptoms", "Hiccups");
        saveForm();

        // Enter second set of observations for this encounter.
        expectVisibleWithin(10000, viewWithId(R.id.patient_chart_root));
        openEncounterForm(VITALS_FORM);
        answerSingleCodedQuestion("Consciousness", "Responds to voice");
        answerMultipleCodedQuestion("Other symptoms", "Cough");
        saveForm();

        // Enter third set of observations for this encounter.
        expectVisibleWithin(10000, viewWithId(R.id.patient_chart_root));
        openEncounterForm(VITALS_FORM);
        answerTextQuestion("Temperature", "37.7");
        answerSingleCodedQuestion("Condition", "Unwell");
        answerTextQuestion("Notes", "Call family");
        saveForm();

        // Check that all values are now visible.
        // Expect a WebView with JS enabled to be visible soon (the chart).
        expectVisibleWithin(10000, viewThat(isJavascriptEnabled()));
        checkObservationValueEquals("Temperature (°C)", "37.7");
        checkObservationValueEquals("Respiratory rate (bpm)", "23");
        checkObservationValueEquals("O₂ saturation", "95");
        checkObservationValueEquals(BP_SYS, "80");
        checkObservationValueEquals(BP_DIA, "100");
        checkObservationValueEquals("AVPU", "V");
        checkObservationValueEquals("Cough", YES);
        checkObservationValueEquals("Hiccups", YES);
        checkObservationValueEquals("Condition", "2");
        checkObservationValueEquals("Notes", "Call …");
    }

    private void checkObservationSet(int row, String dateKey) {
        // TODO/completeness: actually check dateKey
        scrollToAndExpectVisible(viewThat(
            hasAncestorThat(isInRow(row, ROW_HEIGHT)),
            hasBackground(getActivity().getResources().getDrawable(R.drawable.chart_cell_active))));
    }

    /** Exercises all fields in the encounter form, except for encounter time. */
    @Test
    @UiThreadTest
    public void testEncounter_allFieldsWorkOtherThanEncounterTime() {
        inUserLoginGoToDemoPatientChart();
        waitForProgressFragment();

        openEncounterForm(VITALS_FORM);
        answerTextQuestion("Heart rate", "70");
        answerTextQuestion("Respiratory rate", "23");
        answerTextQuestion("saturation", "95");
        answerTextQuestion(BP_SYS, "110");
        answerTextQuestion(BP_DIA, "75");
        answerTextQuestion("Cap Refill Time", "2");
        answerTextQuestion("Temperature", "36.1");
        answerSingleCodedQuestion("Consciousness", "Unresponsive");
        answerSingleCodedQuestion("Urinary function", "Oliguria");
        answerSingleCodedQuestion("Anorexia", "++");

        answerTextQuestion("Vomiting", "1");
        answerTextQuestion("Diarrhea", "2");
        answerMultipleCodedQuestion("Other symptoms", "Headache");
        answerMultipleCodedQuestion("Other symptoms", "Asthenia");
        answerMultipleCodedQuestion("Other symptoms", "Cough");
        answerMultipleCodedQuestion("Other symptoms", "Chest pain");
        answerMultipleCodedQuestion("Other symptoms", "Nausea");
        answerMultipleCodedQuestion("Other symptoms", "Back pain");
        answerMultipleCodedQuestion("Other symptoms", "Photophobia");
        answerMultipleCodedQuestion("Other symptoms", "Dysphagia");
        answerMultipleCodedQuestion("Other symptoms", "Hiccups");
        answerMultipleCodedQuestion("Other symptoms", "Dyspnea");
        answerMultipleCodedQuestion("Other symptoms", "Abdominal pain");
        answerMultipleCodedQuestion("Other symptoms", "Myalgia");

        answerSingleCodedQuestion("Condition", "Stable");
        answerMultipleCodedQuestion("Appearance", "Pallor");
        answerMultipleCodedQuestion("Appearance", "Jaundice");
        answerMultipleCodedQuestion("Appearance", "Cyanosis");
        answerSingleCodedQuestion("Dehydration", "Mild");
        answerSingleCodedQuestion("Neuro", "Confusion");

        // This is not all of the bleeding answers.
        answerMultipleCodedQuestion("Bleeding", "Epistaxis");
        answerMultipleCodedQuestion("Bleeding", "Hemoptysis");
        answerMultipleCodedQuestion("Bleeding", "Hematuria");
        answerMultipleCodedQuestion("Bleeding", "Petechiae");

        answerSingleCodedQuestion("Respiratory", "Mild dyspnea");
        answerSingleCodedQuestion("Circulation", "Weak pulse");
        answerSingleCodedQuestion("Abdomen", "Soft");
        answerSingleCodedQuestion("Abdomen", "Distended");
        answerSingleCodedQuestion("Abdomen", "Painful");
        answerSingleCodedQuestion("Abdomen", "Hepatomegaly");
        answerSingleCodedQuestion("Abdomen", "Splenomegaly");

        answerTextQuestion("Notes", "Possible malaria.");
        saveForm();

        // Check that all values are now visible.
        // Expect a WebView with JS enabled to be visible soon (the chart).
        expectVisibleWithin(10000, viewThat(isJavascriptEnabled()));
        // Wait for WebView to render and scripts to run.
        try { Thread.sleep(10000); } catch (InterruptedException e) { }

        checkObservationValueEquals("Heart rate", "70");
        checkObservationValueEquals("Respiratory rate", "23");
        checkObservationValueEquals("O₂ saturation", "95");
        checkObservationValueEquals(BP_SYS, "110");
        checkObservationValueEquals(BP_DIA, "75");
        checkObservationValueEquals("Cap Refill Time (s)", "2");
        checkObservationValueEquals("Temperature (°C)", "36.1");
        checkObservationValueEquals("AVPU", "U");
        checkObservationValueEquals("Oliguria", YES);
        checkObservationValueEquals("Anorexia", "++");

        checkObservationValueEquals("Vomiting in 24h", "1");
        checkObservationValueEquals("Diarrhea in 24h", "2");
        checkObservationValueEquals("Anorexia", "++");

        checkObservationValueEquals("Headache", YES);
        checkObservationValueEquals("Asthenia", YES);
        checkObservationValueEquals("Cough", YES);
        checkObservationValueEquals("Chest pain", YES);
        checkObservationValueEquals("Nausea", YES);
        checkObservationValueEquals("Back pain", YES);
        checkObservationValueEquals("Photophobia", YES);
        checkObservationValueEquals("Dysphagia", YES);
        checkObservationValueEquals("Hiccups", YES);
        checkObservationValueEquals("Dyspnea", YES);
        checkObservationValueEquals("Abdominal pain", YES);
        checkObservationValueEquals("Myalgia", YES);

        checkObservationValueEquals("Condition", "1");
        checkObservationValueEquals("Pallor", YES);
        checkObservationValueEquals("Jaundice", YES);
        checkObservationValueEquals("Cyanosis", YES);
        checkObservationValueEquals("Dehydration", "A");
        checkObservationValueEquals("Confusion", YES);

        checkObservationValueEquals("Epistaxis", YES);
        checkObservationValueEquals("Hemoptysis", YES);
        checkObservationValueEquals("Hematuria", YES);
        checkObservationValueEquals("Petechiae", YES);

        checkObservationValueEquals("Mild dyspnea", YES);
        checkObservationValueEquals("Weak pulse", YES);
        checkObservationValueEquals("Soft", YES);
        checkObservationValueEquals("Distended", YES);
        checkObservationValueEquals("Painful", YES);
        checkObservationValueEquals("Hepatomegaly", YES);
        checkObservationValueEquals("Splenomegaly", YES);

        checkObservationValueEquals("Notes", "Possi…");
    }
}
