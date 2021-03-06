package org.projectbuendia.client.smoke;

import android.content.Intent;
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
import android.widget.TextView;

import com.android.volley.toolbox.RequestFuture;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.joda.time.DateTime;
import org.junit.Test;
import org.odk.collect.android.views.MediaLayout;
import org.odk.collect.android.views.ODKView;
import org.odk.collect.android.widgets2.group.TableWidgetGroup;
import org.odk.collect.android.widgets2.selectone.ButtonsSelectOneWidget;
import org.projectbuendia.client.App;
import org.projectbuendia.client.R;
import org.projectbuendia.client.json.Datatype;
import org.projectbuendia.client.json.JsonEncounter;
import org.projectbuendia.client.json.JsonUser;
import org.projectbuendia.models.ConceptUuids;
import org.projectbuendia.models.Encounter;
import org.projectbuendia.models.Location;
import org.projectbuendia.models.LocationForest;
import org.projectbuendia.models.Obs;
import org.projectbuendia.client.net.OpenMrsServer;
import org.projectbuendia.client.sync.SyncManager;
import org.projectbuendia.client.ui.AuthorizationActivity;
import org.projectbuendia.client.ui.FunctionalTestCase;
import org.projectbuendia.client.ui.chart.PatientChartController;
import org.projectbuendia.client.utils.Logger;
import org.projectbuendia.client.utils.Utils;
import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import androidx.test.filters.MediumTest;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.hamcrest.Matchers.greaterThan;
import static org.projectbuendia.client.acceptance.ListItemCountAssertion.hasItemCount;
import static org.projectbuendia.client.utils.Utils.eq;

/** A quick test suite that exercises all basic functionality. */
@MediumTest public class SmokeTest extends FunctionalTestCase {
    public static final String HOSTNAME = "buendia";
    public static final String OPENMRS_USERNAME = "tester";
    public static final String OPENMRS_PASSWORD = "tester";
    public static final Logger LOG = Logger.create();

    public static final String DEFAULT_LOCATION = "Suspect";
    public static final String TARGET_LOCATION = "Confirmed";
    public static final String FINAL_LOCATION = "Discharged";

    /** A test that exercises all the API methods and endpoints. */
    @Test @UiThreadTest public void testApi() {
        screenshot("Start");
        initSettings();  // step 0
        screenshot("Loaded users");
        final String id = "" + getNextAvailableId(R.id.users);
        addUser("Test" + id, "User" + id);  // step 1
        screenshot("Added new user");
        signInFullSync("Test" + id + " User" + id);  // step 2
        screenshot("Signed in");
        int targetCount = getPatientCount(TARGET_LOCATION);
        addPatient(id, "Given" + id, "Family" + id, 11);  // step 3
        screenshot("Added new patient");
        String patientUuid = PatientChartController.currentPatientUuid;
        movePatient(DEFAULT_LOCATION, TARGET_LOCATION);
        back();  // back to location list
        screenshot("Moved patient away");
        expectPatientCount(TARGET_LOCATION, targetCount + 1);
        int finalCount = getPatientCount(FINAL_LOCATION);
        internalMovePatient(patientUuid, internalGetLocationUuid(FINAL_LOCATION));
        internalIncrementalSync();  // step 4
        screenshot("Moved patient back");
        expectPatientCount(TARGET_LOCATION, targetCount);
        expectPatientCount(FINAL_LOCATION, finalCount + 1);
        goToPatientById(id, "Given" + id, "Family" + id);  // step 5
        screenshot("Opened chart by ID");
        editPatientAge(22);  // step 6
        screenshot("Edited patient age");
        rewindAdmissionDate(2, "Day 3");  // step 7
        screenshot("Edited admission date");
        openForm("Patient attributes");  // step 8
        screenshot("Opened form");
        answerMultipleCodedQuestion("Attributes", "Pregnant");
        submitForm(); // step 9
        screenshot("Submitted form");
        waitForViewThat(hasTextContaining("pregnant, 22 y"));
        addOrder("Sunshine", "25 rays", "Get outside!");  // step 10
        screenshot("Added new order");
        editOrder("Sunshine", "Exercise!");  // step 11
        screenshot("Edited order");
        deleteOrder("Sunshine");  // step 12
        screenshot("Deleted order");
        toast("Smoke test passed!");
    }

    private void authorize() {
        App.getSettings().setPeriodicSyncDisabled(true);
        App.getSyncManager().applyPeriodicSyncSettings();
        App.getSettings().setLocale("en");
        App.getSettings().deauthorize();
        getActivity().startActivity(
            new Intent(getActivity(), AuthorizationActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)
        );
        clearAndType(HOSTNAME, R.id.server_field);
        clearAndType(OPENMRS_USERNAME, R.id.openmrs_user_field);
        clearAndType(OPENMRS_PASSWORD, R.id.openmrs_password_field);
        click(R.id.authorize_button);
    }

    private void initSettings() {
        App.getSettings().setPeriodicSyncDisabled(true);
        App.getSyncManager().applyPeriodicSyncSettings();
        App.getSettings().setLocale("en");
        Utils.restartActivity(getActivity());
        click(R.id.settings);
        click("Developer");
        clickIfUnchecked(viewThat(
            isA(CheckBox.class),
            whoseParent(hasSiblingThat(hasChildThat(hasText("Periodic sync disabled"))))
        ));
        click("General");
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
        EditText editText = (EditText) getViewThat(
            isA(EditText.class),
            hasSiblingThat(hasText(""))
        );
        if (!eq(editText.getText().toString(), value)) {
            clearAndType(value, viewThat(
                isA(EditText.class),
                hasSiblingThat(hasText(""))
            ));
        }
        click(buttonText);
    }

    private void addUser(String given, String family) {
        click(R.id.action_new_user);
        type(given, R.id.given_name_field);
        type(family, R.id.family_name_field);
        click("OK");
        expect(given + " " + family);
    }

    private void signInFullSync(String name) {
        click(name);
        waitFor("Discharged");
    }

    private void addPatient(String id, String given, String family, int age) {
        click(R.id.action_new_patient);
        waitFor("New patient");

        type(id, R.id.patient_id);
        type(given, R.id.patient_given_name);
        type(family, R.id.patient_family_name);
        type(age, R.id.patient_age_years);
        click("Male");
        click("Female");
        click("Other");
        click("Other");
        click("OK");

        expect(id + ". " + given + " " + family);
        expectRegex("Sex unknown, " + age + " y");
    }

    private void movePatient(String oldLocation, String newLocation) {
        waitForViewThat(containsElementMatchingSelectorWithText(
            ".tile.concept-buendia_concept_placement .value", oldLocation));
        clickElementMatchingSelector(".tile.concept-buendia_concept_placement");
        click(newLocation);
        click("OK");
        waitForViewThat(containsElementMatchingSelectorWithText(
            ".tile.concept-buendia_concept_placement .value", newLocation));
    }

    private String internalGetLocationUuid(String name) {
        LocationForest forest = App.getModel().getForest();
        for (Location location : forest.allNodes()) {
            if (eq(location.name, name)) return location.uuid;
        }
        throw new RuntimeException("No location named \"" + name + "\" could be found");
    }

    /** Moves the patient on the server without updating the local data store. */
    private void internalMovePatient(String patientUuid, String locationUuid) {
        Encounter encounter = new Encounter(
            null, patientUuid, Utils.getProviderUuid(), DateTime.now(), new Obs[] {new Obs(
                null, null, patientUuid, Utils.getProviderUuid(), ConceptUuids.PLACEMENT_UUID,
                Datatype.TEXT, DateTime.now(), null, locationUuid, null
            )}
        );
        RequestFuture<JsonEncounter> future = RequestFuture.newFuture();
        App.getServer().addEncounter(encounter, future, future);
        try {
            JsonEncounter result = future.get(OpenMrsServer.TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException("Could not move patient", e);
        }
    }

    private void internalIncrementalSync() {
        sleep(500);
        App.getSyncManager().sync(SyncManager.SMALL_PHASES);
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
        waitForViewThat(hasTextContaining(age + " y"));
    }

    private void rewindAdmissionDate(int numDays, String expected) {
        clickElementMatchingSelector(".tile.concept-162622");
        ViewAction tapUp = new GeneralClickAction(
            Tap.SINGLE, GeneralLocation.TOP_CENTER, Press.FINGER);
        for (int i = 0; i < numDays; i++) {
            waitForViewThat(
                isA(NumberPicker.class),
                hasAncestorThat(isA(DatePicker.class)),
                hasChildThat(hasTextMatchingRegex("[0-9]{1,2}"))
            ).perform(tapUp);
            sleep(50);
        }
        click("OK");
        waitForViewThat(containsElementMatchingSelectorWithText(
            ".tile.concept-162622 .value", expected));
    }

    private void openForm(String titleSubstring) {
        click(R.id.action_edit);
        click(viewContainingText(titleSubstring));
    }

    private void submitForm() {
        click("Save");
    }

    private void addOrder(String medication, String dosage, String notes) {
        for (int i = 0; i < 10; i++) {
            clickElementMatchingSelector("#down");
            sleep(50);
        }
        clickElementMatchingSelector("#new-treatment");

        waitUntilVisible(viewThat(hasText("New treatment")));
        type(medication, R.id.order_drug);
        type(dosage, R.id.order_dosage);
        type(notes, R.id.order_notes);
        click("OK");

        waitForViewThat(containsElementMatchingSelector(
            ".order-" + Utils.toCssIdentifier(medication)));
    }

    private void editOrder(String medication, String notes) {
        clickElementMatchingSelector(".order-" + Utils.toCssIdentifier(medication) + " th");

        waitFor("Edit treatment");
        clearAndType(notes, R.id.order_notes);
        click("OK");

        waitForViewThat(containsElementMatchingSelectorWithText(
            ".order-" + Utils.toCssIdentifier(medication) + " .notes", notes));
    }

    private void deleteOrder(String medication) {
        clickElementMatchingSelector(".order-" + Utils.toCssIdentifier(medication) + " th");

        waitFor("Edit treatment");
        click("Delete this treatment");

        waitFor("Confirmation");
        click("Delete");

        waitForViewThat(
            containsElementMatchingSelector("#new-treatment"),
            containsNoElementMatchingSelector(".order-" + Utils.toCssIdentifier(medication))
        );
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

    private int getPatientCount(String location) {
        TextView countView = (TextView) getViewThat(
            hasId(R.id.patient_count),
            hasSiblingThat(hasId(R.id.location_name), hasText(location))
        );
        String text = countView.getText().toString();
        try {
            return Integer.valueOf(text);
        } catch (NumberFormatException e) {
            return -1;
        }
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
            names.add(user.getName());
        }
        int nextId = 9000;
        while (names.contains("Test" + nextId + " User" + nextId)) {
            nextId++;
        }
        return nextId;
    }


}
