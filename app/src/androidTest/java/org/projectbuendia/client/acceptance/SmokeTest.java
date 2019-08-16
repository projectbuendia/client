package org.projectbuendia.client.acceptance;

import android.support.test.annotation.UiThreadTest;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.action.GeneralClickAction;
import android.support.test.espresso.action.GeneralLocation;
import android.support.test.espresso.action.Press;
import android.support.test.espresso.action.Tap;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.NumberPicker;
import android.widget.RadioButton;

import com.google.common.base.Optional;

import org.hamcrest.Matcher;
import org.junit.Test;
import org.odk.collect.android.views.MediaLayout;
import org.odk.collect.android.views.ODKView;
import org.odk.collect.android.widgets2.group.TableWidgetGroup;
import org.odk.collect.android.widgets2.selectone.ButtonsSelectOneWidget;
import org.projectbuendia.client.App;
import org.projectbuendia.client.R;
import org.projectbuendia.client.events.CrudEventBus;
import org.projectbuendia.client.json.JsonUser;
import org.projectbuendia.client.models.AppModel;
import org.projectbuendia.client.models.PatientDelta;
import org.projectbuendia.client.sync.SyncManager;
import org.projectbuendia.client.ui.FunctionalTestCase;
import org.projectbuendia.client.ui.chart.PatientChartController;

import java.util.ArrayList;
import java.util.List;

import androidx.test.filters.MediumTest;

import static android.support.test.espresso.Espresso.pressBack;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.projectbuendia.client.acceptance.ListItemCountAssertion.hasItemCount;

/** A test that exercises all the API calls to a clean server. */
@MediumTest public class SmokeTest extends FunctionalTestCase {
    public static final String HOSTNAME = "ping.buendia.org";

    @Test @UiThreadTest public void test() {
        screenshot("Start");
        waitFor("Guest User");
        initSettings();  // step 0
        screenshot("Loaded users");
        final String id = "" + getNextAvailableId(R.id.users);
        addUser("Test" + id, "User" + id);  // step 1
        screenshot("Added new user");
        signInFullSync("Test" + id + " User" + id);  // step 2
        screenshot("Signed in");
        addPatient(id, "Given" + id, "Family" + id, 11);  // step 3
        screenshot("Added new patient");
        String patientUuid = PatientChartController.currentPatientUuid;
        String locationUuid = PatientChartController.currentPatientLocationUuid;
        movePatient("Discharged");
        pressBack();  // back to location list
        screenshot("Moved patient away");
        expectPatientCount("Triage", 0);
        internalMovePatient(patientUuid, locationUuid);
        internalIncrementalSync();  // step 4
        screenshot("Moved patient back");
        expectPatientCount("Triage", 1);
        goToPatientById(id, "Given" + id, "Family" + id);  // step 5
        screenshot("Opened chart by ID");
        editPatientAge(22);  // step 6
        screenshot("Edited patient age");
        rewindAdmissionDate(2, "Day 3");  // step 7
        screenshot("Edited admission date");
        openForm("Patient attributes");  // step 8
        screenshot("Opened form");
        answerMultipleCodedQuestion("Attributes", "Oxygen mask");
        submitForm(); // step 9
        screenshot("Submitted form");
        expectVisibleSoon(viewThat(hasId(R.id.patient_chart_pregnant), hasText("Oxygen")));

        addOrder("Sunshine", "25 rays", "Get outside!");  // step 10
        screenshot("Added new order");
        editOrder("Sunshine", "Exercise!");  // step 11
        screenshot("Edited order");
        deleteOrder("Sunshine");  // step 12
        screenshot("Deleted order");
    }

    private void initSettings() {
        click(R.id.settings);
        enterSetting("App update check interval (seconds)", 3600);
        enterSetting("Small sync interval (seconds)", 0);
        enterSetting("Medium sync interval (seconds)", 0);
        enterSetting("Large sync interval (seconds)", 0);
        back();  // necessary to apply the above settings and stop all syncs

        click(R.id.settings);
        enterSetting("Buendia server", HOSTNAME, "Apply and clear local data");
        enterSetting("OpenMRS username", "tester", "Apply and clear local data");
        enterSetting("OpenMRS password", "tester", "Apply and clear local data");
        back();
    }

    private void enterSetting(String title, Object value) {
        enterSetting(title, value, "OK");
    }

    private void enterSetting(String title, Object value, String buttonText) {
        click(title);
        clearAndType(value, viewThat(
            isA(EditText.class),
            hasSiblingThat(hasText(""))
        ));
        click(buttonText);
    }

    private void addUser(String given, String family) {
        click(R.id.action_new_user);
        type(given, R.id.add_user_given_name_tv);
        type(family, R.id.add_user_family_name_tv);
        click("OK");
        waitFor(given + " " + family);
    }

    private void signInFullSync(String name) {
        click(name);

        waitFor("Discharged");
    }

    private void addPatient(String id, String given, String family, int age) {
        click(R.id.action_new_patient);
        expectVisible(viewWithText("New patient"));

        type(id, R.id.patient_id);
        type(given, R.id.patient_given_name);
        type(family, R.id.patient_family_name);
        type(age, R.id.patient_age_years);
        click("OK");

        waitFor(id + ". " + given + " " + family);
        waitFor(age + " y");
    }

    private void movePatient(String location) {
        click(R.id.attribute_location);
        click(location);
        click("OK");

        expectVisibleSoon(viewThat(hasId(R.id.attribute_location),
            hasDescendantThat(hasId(R.id.view_attribute_name), hasText("Location")),
            hasDescendantThat(hasId(R.id.view_attribute_value), hasText(location))
        ));
    }

    private void internalMovePatient(String patientUuid, String locationUuid) {
        AppModel model = App.getModel();
        CrudEventBus bus = App.getCrudEventBus();
        PatientDelta delta = new PatientDelta();
        delta.assignedLocationUuid = Optional.of(locationUuid);
        model.updatePatient(bus, patientUuid, delta);
    }

    private void internalIncrementalSync() {
        App.getInstance().getSyncManager().sync(SyncManager.SMALL_PHASES);
    }

    private void goToPatientById(String id, String given, String family) {
        click(R.id.action_go_to);
        type(id, R.id.go_to_patient_id);
        click("Go to chart");

        waitFor(id + ". " + given + " " + family);
    }

    private void editPatientAge(int age) {
        click(R.id.action_edit);
        click("Edit patient");
        clearAndType(age, R.id.patient_age_years);
        click("OK");

        expectVisible(viewContainingText(age + " y"));
    }

    private void rewindAdmissionDate(int numDays, String expected) {
        click(R.id.attribute_admission_days);
        ViewAction tapUp = new GeneralClickAction(
            Tap.SINGLE, GeneralLocation.TOP_CENTER, Press.FINGER);
        for (int i = 0; i < numDays; i++) {
            act(firstViewThat(
                isA(NumberPicker.class),
                hasAncestorThat(isA(DatePicker.class))
            ), tapUp);
            sleep(50);
        }
        click("OK");

        waitFor(expected);
    }

    private void openForm(String titleSubstring) {
        click(R.id.action_edit);
        click(viewThat(hasTextContaining(titleSubstring)));
    }

    private void submitForm() {
        click("Save");
        waitFor("Submitting form");
    }

    private void addOrder(String medication, String dosage, String notes) {
        clickXpath("//th[@class='command'][contains(text(),'Treatment')]");

        waitFor("New treatment");
        type(medication, R.id.order_medication);
        type(dosage, R.id.order_dosage);
        type(notes, R.id.order_notes);
        click("OK");

        expectXpathText("//div[@class='medication']", is("Sunshine"));
    }

    private void editOrder(String medication, String notes) {
        clickXpath(getMedicationXpath(medication));

        waitFor("Edit treatment");
        type(notes, R.id.order_notes);
        click("OK");

        expectXpathText("//div[@class='notes']", is(notes));
    }

    private void deleteOrder(String medication) {
        clickXpath(getMedicationXpath(medication));

        waitFor("Edit treatment");
        click("Delete this treatment");

        waitFor("Confirmation");
        click("Delete");

        waitFor("//th[@class='command'][contains(text(),'Treatment')]");
        expectXpathNotFound(getMedicationXpath(medication));
    }

    private String getMedicationXpath(String medication) {
        return String.format(
            "//div[@class='medication'][contains(text(),'%s')]",
            medication);
    }

    private void answerTextQuestion(String questionSubstring, String answerText) {
        scrollToAndType(answerText, viewThat(
            isA(EditText.class),
            hasSiblingThat(
                isA(MediaLayout.class),
                hasDescendantThat(hasTextContaining(questionSubstring)))));
    }

    private void answerSingleCodedQuestion(String questionText, String answerText) {
        answerCodedQuestion(questionText, answerText,
            ButtonsSelectOneWidget.class, TableWidgetGroup.class);
    }

    private void answerMultipleCodedQuestion(String questionText, String answerText) {
        answerCodedQuestion(questionText, answerText,
            ButtonsSelectOneWidget.class, TableWidgetGroup.class, ODKView.class);
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


    private void waitFor(String text) {
        expectVisibleSoon(viewWithText(text));
    }

    private void expectPatientCount(String location, int count) {
        expectVisibleSoon(viewThat(hasId(R.id.button), hasChildThat(
            hasChildThat(hasId(R.id.location_name), hasText(location)),
            hasChildThat(hasId(R.id.patient_count), hasText("" + count))
        )));
    }

    private void expectListItemCount(int id, Matcher<Integer> matcher) {
        expectVisibleSoon(viewWithId(R.id.users).check(hasItemCount(greaterThan(0))));
    }

    private int getNextAvailableId(int listId) {
        View listView = getViewThat(hasId(listId));
        ListAdapter adapter = ((AdapterView<ListAdapter>) listView).getAdapter();
        List<String> names = new ArrayList<>();
        for (int i = 0; i < adapter.getCount(); i++) {
            JsonUser user = (JsonUser) adapter.getItem(i);
            names.add(user.fullName);
        }
        int nextId = 1;
        while (names.contains("Test" + nextId + " User" + nextId)) {
            nextId++;
        }
        return nextId;
    }
}
