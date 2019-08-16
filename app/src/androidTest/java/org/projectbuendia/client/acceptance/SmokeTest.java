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

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
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
import org.projectbuendia.client.utils.Logger;
import org.projectbuendia.client.utils.Utils;
import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.List;

import androidx.test.filters.MediumTest;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hamcrest.Matchers.greaterThan;
import static org.projectbuendia.client.acceptance.ListItemCountAssertion.hasItemCount;

/** A quick test suite that exercises all basic functionality. */
@MediumTest public class SmokeTest extends FunctionalTestCase {
    public static final String HOSTNAME = "ping.buendia.org";
    public static final String OPENMRS_USERNAME = "tester";
    public static final String OPENMRS_PASSWORD = "tester";
    public static final Logger LOG = Logger.create();

    /** A test that exercises all the API methods and endpoints. */
    @Test @UiThreadTest public void testApi() {
        screenshot("Start");
        expect("Guest User");
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
        back();  // back to location list
        screenshot("Moved patient away");
        expectPatientCount("Triage", 0);
        internalMovePatient(patientUuid, locationUuid);
        internalIncrementalSync();  // step 4
        screenshot("Moved patient back");
        expectPatientCount("Triage", 1);
        goToPatientById(id, "Given" + id, "Family" + id);  // step 5
        screenshot("Opened chart by ID");
        movePatient("Confirmed");  // move elsewhere to avoid interfering with next run
        editPatientAge(22);  // step 6
        screenshot("Edited patient age");
        rewindAdmissionDate(2, "Day 3");  // step 7
        screenshot("Edited admission date");
        openForm("Patient attributes");  // step 8
        screenshot("Opened form");
        answerMultipleCodedQuestion("Attributes", "Oxygen mask");
        submitForm(); // step 9
        screenshot("Submitted form");
        waitFor(viewThat(hasId(R.id.patient_chart_pregnant), hasText("Oxygen")));
        addOrder("Sunshine", "25 rays", "Get outside!");  // step 10
        screenshot("Added new order");
        editOrder("Sunshine", "Exercise!");  // step 11
        screenshot("Edited order");
        deleteOrder("Sunshine");  // step 12
        screenshot("Deleted order");
    }

    private void initSettings() {
        click(R.id.settings);
        clickIfUnchecked(viewThat(
            isA(CheckBox.class),
            whoseParent(hasSiblingThat(hasChildThat(hasText("Periodic sync disabled"))))
        ));
        enterSetting("Buendia server", HOSTNAME, "Apply and clear local data");
        enterSetting("OpenMRS username", OPENMRS_USERNAME, "Apply and clear local data");
        enterSetting("OpenMRS password", OPENMRS_PASSWORD, "Apply and clear local data");
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
        expect(given + " " + family);
    }

    private void signInFullSync(String name) {
        click(name);
        waitFor("Discharged");
    }

    private void addPatient(String id, String given, String family, int age) {
        click(R.id.action_new_patient);
        expect("New patient");

        type(id, R.id.patient_id);
        type(given, R.id.patient_given_name);
        type(family, R.id.patient_family_name);
        type(age, R.id.patient_age_years);
        click("OK");

        expect(id + ". " + given + " " + family);
        expect(age + " y");
    }

    private void movePatient(String location) {
        click(R.id.attribute_location);
        click(location);
        click("OK");
        waitFor(viewThat(hasId(R.id.attribute_location),
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
        expect(viewContainingText(age + " y"));
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
        click(viewContainingText(titleSubstring));
    }

    private void submitForm() {
        click("Save");
    }

    private void addOrder(String medication, String dosage, String notes) {
        clickElementWithId("new_treatment");

        waitFor("New treatment");
        type(medication, R.id.order_medication);
        type(dosage, R.id.order_dosage);
        type(notes, R.id.order_notes);
        click("OK");

        waitFor(viewThat(containsElementWithId(Utils.toCssIdentifier(medication))));
    }

    private void editOrder(String medication, String notes) {
        clickElementWithId(Utils.toCssIdentifier(medication) + "_row");

        waitFor("Edit treatment");
        clearAndType(notes, R.id.order_notes);
        click("OK");

        waitFor(viewThat(containsElementWithIdAndText(
            Utils.toCssIdentifier(medication) + "_notes", notes)));
    }

    private void deleteOrder(String medication) {
        clickElementWithId(Utils.toCssIdentifier(medication) + "_row");

        waitFor("Edit treatment");
        click("Delete this treatment");

        waitFor("Confirmation");
        click("Delete");

        waitFor(viewThat(
            containsElementWithId("new_treatment"),
            containsNoElementWithId(Utils.toCssIdentifier(medication))
        ));
    }

    /** Matches Documents that do not have any elements with a given ID. */
    public static Matcher<Document> hasNoElementWithId(final String id) {
        checkNotNull(id);
        return new TypeSafeMatcher<Document>() {
            @Override public void describeTo(Description description) {
                description.appendText("has no element with id: " + id);
            }

            @Override public boolean matchesSafely(Document document) {
                return document.getElementById(id) == null;
            }
        };
    }

    private void answerTextQuestion(String questionSubstring, String answerText) {
        type(answerText, viewThat(
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

        click(viewThat(
            isAnyOf(CheckBox.class, RadioButton.class),
            hasAncestorThat(
                isAnyOf(classes),
                hasDescendantThat(hasTextContaining(questionText))),
            hasTextContaining(answerText)));
    }

    private void expectPatientCount(String location, int count) {
        waitUntilVisible(viewThat(hasId(R.id.button), hasChildThat(
            hasChildThat(hasId(R.id.location_name), hasText(location)),
            hasChildThat(hasId(R.id.patient_count), hasText("" + count))
        )));
    }

    private void expectListItemCount(int id, Matcher<Integer> matcher) {
        waitUntilVisible(viewWithId(R.id.users).check(hasItemCount(greaterThan(0))));
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
