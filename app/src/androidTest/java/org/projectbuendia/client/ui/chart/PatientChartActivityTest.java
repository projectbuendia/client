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
import android.test.suitebuilder.annotation.MediumTest;
import android.view.View;
import android.webkit.WebView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;

import org.hamcrest.Matcher;
import org.odk.collect.android.views.MediaLayout;
import org.odk.collect.android.views.ODKView;
import org.odk.collect.android.widgets2.group.TableWidgetGroup;
import org.odk.collect.android.widgets2.selectone.ButtonsSelectOneWidget;
import org.projectbuendia.client.R;
import org.projectbuendia.client.events.FetchXformSucceededEvent;
import org.projectbuendia.client.events.SubmitXformSucceededEvent;
import org.projectbuendia.client.events.sync.SyncFinishedEvent;
import org.projectbuendia.client.ui.FunctionalTestCase;
import org.projectbuendia.client.ui.sync.EventBusIdlingResource;
import org.projectbuendia.client.utils.Logger;

import java.util.UUID;

import static android.support.test.espresso.web.assertion.WebViewAssertions.webMatches;
import static android.support.test.espresso.web.sugar.Web.onWebView;
import static android.support.test.espresso.web.webdriver.DriverAtoms.findElement;
import static android.support.test.espresso.web.webdriver.DriverAtoms.getText;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;

import static java.lang.String.format;

/** Functional tests for {@link PatientChartActivity}. */
@MediumTest
public class PatientChartActivityTest extends FunctionalTestCase {
    private static final Logger LOG = Logger.create();

    private static final int ROW_HEIGHT = 84;

    private static final String FORM_LABEL = "[test] Form";
    private static final String TEMPERATURE_LABEL = "[test] Temperature (°C)";
    private static final String RESPIRATORY_RATE_LABEL = "[test] Respiratory rate (bpm)";
    private static final String SPO2_OXYGEN_SAT_LABEL = "[test] SpO2 oxygen sat (%%)";
    private static final String BLOOD_PRESSURE_SYSTOLIC_LABEL = "[test] Blood pressure, systolic";
    private static final String BLOOD_PRESSURE_DIASTOLIC_LABEL = "[test] Blood pressure, diastolic";
    private static final String WEIGHT_LABEL = "[test] Weight (kg)";
    private static final String HEIGHT_LABEL = "[test] Height (cm)";
    private static final String SHOCK_LABEL = "[test] Shock";
    private static final String SHOCK_VALUE = "[test] Mild";
    private static final String CONSCIOUSNESS_LABEL = "[test] Consciousness (AVPU)";
    private static final String CONSCIOUSNESS_VALUE = "[test] Responds to voice";
    private static final String OTHER_SYMPTOMS_LABEL = "[test] Other symptoms";
    private static final String OTHER_SYMPTOMS_VALUE = "[test] Cough";
    private static final String HICCUPS_LABEL = "[test] Hiccups";
    private static final String HEADACHE_LABEL = "[test] Headache";
    private static final String SORE_THROAT_LABEL = "[test] Sore throat";
    private static final String HEARTBURN_LABEL = "[test] Heartburn";
    private static final String PREGNANT_LABEL = "Pregnant";
    private static final String CONDITION_LABEL = "Condition";
    private static final String CONDITION_VALUE = "Unwell";
    private static final String NOTES_LABEL = "[test] Notes";

    private static final String NO_VALUE = "○";
    private static final String YES_VALUE = "●";

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
        openActionBarOptionsMenu();

        EventBusIdlingResource<FetchXformSucceededEvent> xformIdlingResource =
            new EventBusIdlingResource<FetchXformSucceededEvent>(
                UUID.randomUUID().toString(), mEventBus);
        click(viewWithText(FORM_LABEL));
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
        answerTextQuestion(RESPIRATORY_RATE_LABEL, "17");

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

        // Update a couple of observations (respirattory rate and blood pressure),
        // and verify that the latest value is visible for each.
        for (int i = 0; i < 3; i++) {
            openEncounterForm();

            String respiratoryRate = Integer.toString(i + 80);
            String bpSystolic = Integer.toString(i + 80);
            String bpDiastolic = Integer.toString(5 + 100);
            answerTextQuestion(RESPIRATORY_RATE_LABEL, respiratoryRate);
            answerTextQuestion(BLOOD_PRESSURE_SYSTOLIC_LABEL, bpSystolic);
            answerTextQuestion(BLOOD_PRESSURE_DIASTOLIC_LABEL, bpDiastolic);
            saveForm();

            // Check the proper values.
            checkObservationValue("999005242", equalTo(respiratoryRate)); //RESPIRATORY_RATE
            checkObservationValue("999005085", equalTo(bpSystolic)); //BLOOD_PRESSURE_SYSTOLIC
            checkObservationValue("999005086", equalTo(bpDiastolic)); //BLOOD_PRESSURE_DIASTOLIC
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
        //answerTextQuestion(TEMPERATURE_LABEL, "37.5"); //issue #10
        answerTextQuestion(RESPIRATORY_RATE_LABEL, "23");
        //answerTextQuestion(SPO2_OXYGEN_SAT_LABEL, "95"); //issue #10
        answerTextQuestion(BLOOD_PRESSURE_SYSTOLIC_LABEL, "80");
        answerTextQuestion(BLOOD_PRESSURE_DIASTOLIC_LABEL, "100");
        saveForm();

        // Enter second set of observations for this encounter.
        openEncounterForm();
        answerTextQuestion(WEIGHT_LABEL, "80");
        answerTextQuestion(HEIGHT_LABEL, "170");
        answerSingleCodedQuestion(SHOCK_LABEL, SHOCK_VALUE);
        answerSingleCodedQuestion(CONSCIOUSNESS_LABEL, CONSCIOUSNESS_VALUE);
        answerMultipleCodedQuestion(OTHER_SYMPTOMS_LABEL, OTHER_SYMPTOMS_VALUE);
        saveForm();

        // Enter third set of observations for this encounter.
        openEncounterForm();
        answerSingleCodedQuestion(HICCUPS_LABEL, "No");
        answerSingleCodedQuestion(HEADACHE_LABEL, "No");
        answerSingleCodedQuestion(SORE_THROAT_LABEL, "Yes");
        answerSingleCodedQuestion(HEARTBURN_LABEL, "No");
        answerSingleCodedQuestion(PREGNANT_LABEL, "Yes");
        answerSingleCodedQuestion(CONDITION_LABEL, CONDITION_VALUE);
        answerTextQuestion(NOTES_LABEL, "Call family");
        saveForm();

        // Check that all values are now visible.
        //checkObservationValue("999005088", equalTo("37.5")); //TEMPERATURE //issue #10
        checkObservationValue("999005242", equalTo("23")); //RESPIRATORY_RATE
        //checkObservationValue("999005092", equalTo("95")); //SPO2_OXYGEN_SAT //issue #10
        checkObservationValue("999005085", equalTo("80")); //BLOOD_PRESSURE_SYSTOLIC
        checkObservationValue("999005086", equalTo("100")); //BLOOD_PRESSURE_DIASTOLIC
        checkObservationValue("999005089", equalTo("80")); //WEIGHT
        checkObservationValue("999005090", equalTo("170")); //HEIGHT
        checkObservationValue("999112989", equalTo(SHOCK_VALUE)); //SHOCK
        checkObservationValue("999162643", equalTo("V")); //CONSCIOUSNESS
        checkObservationValue("999143264", equalTo(YES_VALUE)); //COUGH
        checkObservationValue("999138862", equalTo(NO_VALUE)); //HICCUPS
        checkObservationValue("999139084", equalTo(NO_VALUE)); //HEADACHE
        checkObservationValue("999158843", equalTo(YES_VALUE)); //SORE_THROAT
        checkObservationValue("concept-a3657203-cfed-44b8-8e3f-960f8d4cf3b3",
            equalTo(CONDITION_VALUE)); //UNWELL
        checkObservationValue("concept-999162169", equalTo("Call family")); //NOTES
    }

    private void checkObservationValue(String conceptionId, Matcher<String> resultMatcher) {
        //TODO: Remove hard-coded class name.
        onWebView()
                .withElement(findElement(Locator.CSS_SELECTOR,
                    String.format(".concept-%s td:nth-child(2)", conceptionId)))
            .check(webMatches(getText(), resultMatcher));
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
        //answerTextQuestion(TEMPERATURE_LABEL, "37.5"); //issue #10
        answerTextQuestion(RESPIRATORY_RATE_LABEL, "23");
        //answerTextQuestion(SPO2_OXYGEN_SAT_LABEL, "95"); //issue #10
        answerTextQuestion(BLOOD_PRESSURE_SYSTOLIC_LABEL, "80");
        answerTextQuestion(BLOOD_PRESSURE_DIASTOLIC_LABEL, "100");
        answerTextQuestion(WEIGHT_LABEL, "80");
        answerTextQuestion(HEIGHT_LABEL, "170");
        answerCodedQuestion(SHOCK_LABEL, SHOCK_VALUE);
        answerCodedQuestion(CONSCIOUSNESS_LABEL, CONSCIOUSNESS_VALUE);
        answerCodedQuestion(OTHER_SYMPTOMS_LABEL, OTHER_SYMPTOMS_VALUE);
        answerCodedQuestion(HICCUPS_LABEL, "Yes");
        answerCodedQuestion(HEADACHE_LABEL, "Yes");
        answerCodedQuestion(SORE_THROAT_LABEL, "Yes");
        answerCodedQuestion(HEARTBURN_LABEL, "No");
        answerCodedQuestion(PREGNANT_LABEL, "Yes");
        answerCodedQuestion(CONDITION_LABEL, CONDITION_VALUE);
        answerTextQuestion(NOTES_LABEL, "Call the family");
        saveForm();

        // Check that all values are now visible.
        //checkObservationValue("999005088", equalTo("37.5")); //TEMPERATURE //issue #10
        checkObservationValue("999005242", equalTo("23")); //RESPIRATORY_RATE
        //checkObservationValue("999005092", equalTo("95")); //SPO2_OXYGEN_SAT //issue #10
        checkObservationValue("999005085", equalTo("80")); //BLOOD_PRESSURE_SYSTOLIC
        checkObservationValue("999005086", equalTo("100")); //BLOOD_PRESSURE_DIASTOLIC
        checkObservationValue("999005089", equalTo("80")); //WEIGHT
        checkObservationValue("999005090", equalTo("170")); //HEIGHT
        checkObservationValue("999112989", equalTo(SHOCK_VALUE)); //SHOCK
        checkObservationValue("999162643", equalTo(CONSCIOUSNESS_VALUE)); //CONSCIOUSNESS
        checkObservationValue("999143264", equalTo(OTHER_SYMPTOMS_VALUE)); //COUGH
        checkObservationValue("999138862", equalTo("No")); //HICCUPS
        checkObservationValue("999139084", equalTo("No")); //HEADACHE
        checkObservationValue("999158843", equalTo("Yes")); //SORE_THROAT
        checkObservationValue("concept-a3657203-cfed-44b8-8e3f-960f8d4cf3b3",
            equalTo(CONDITION_VALUE)); //UNWELL
        checkObservationValue("concept-999162169", equalTo("Call family")); //NOTES
    }
}