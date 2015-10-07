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

package org.projectbuendia.client.ui;

import android.app.Activity;
import android.content.res.Resources;
import android.support.annotation.Nullable;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.IdlingPolicies;
import android.support.test.espresso.NoActivityResumedException;
import android.support.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import android.support.test.runner.lifecycle.Stage;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.squareup.spoon.Spoon;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.projectbuendia.client.R;
import org.projectbuendia.client.events.data.ItemCreatedEvent;
import org.projectbuendia.client.events.sync.SyncSucceededEvent;
import org.projectbuendia.client.events.user.KnownUsersLoadedEvent;
import org.projectbuendia.client.models.Patient;
import org.projectbuendia.client.models.PatientDelta;
import org.projectbuendia.client.json.JsonPatient;
import org.projectbuendia.client.ui.login.LoginActivity;
import org.projectbuendia.client.ui.matchers.TestCaseWithMatcherMethods;
import org.projectbuendia.client.ui.sync.EventBusIdlingResource;
import org.projectbuendia.client.utils.EventBusRegistrationInterface;
import org.projectbuendia.client.utils.EventBusWrapper;
import org.projectbuendia.client.utils.Logger;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import de.greenrobot.event.EventBus;

import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.matcher.RootMatchers.isDialog;
import static org.hamcrest.Matchers.is;
import static org.projectbuendia.client.ui.matchers.AppPatientMatchers.isPatientWithId;

/**
 * Base class for functional tests that sets timeouts to be permissive, optionally logs in as a
 * user before continuing, and provides some utility functions for convenience.
 */
public class FunctionalTestCase extends TestCaseWithMatcherMethods<LoginActivity> {
    private static final Logger LOG = Logger.create();

    public static final String LOCATION_NAME = "ITFC ICU";

    // For now, we create a new demo patient for tests using the real patient
    // creation UI on each test run (see {@link #inUserLoginInitDemoPatient()}).
    // TODO/robustness: Use externally preloaded demo data instead.
    protected static String sDemoPatientId = null;
    private boolean mWaitForUserSync = true;
    protected EventBusRegistrationInterface mEventBus;

    public FunctionalTestCase() {
        super(LoginActivity.class);
    }

    @Override public void setUp() throws Exception {
        // Give additional leeway for idling resources, as sync may be slow, especially on Edisons.
        // Increased to 5 minutes as certain operations (like initial sync) may take an exceedingly
        // long time.
        IdlingPolicies.setIdlingResourceTimeout(300, TimeUnit.SECONDS);
        IdlingPolicies.setMasterPolicyTimeout(300, TimeUnit.SECONDS);

        mEventBus = new EventBusWrapper(EventBus.getDefault());

        // Wait for users to sync.
        if (mWaitForUserSync) {
            EventBusIdlingResource<KnownUsersLoadedEvent> resource =
                new EventBusIdlingResource<>("USERS", mEventBus);
            Espresso.registerIdlingResources(resource);
        }

        super.setUp();
        getActivity();
    }

    public void setWaitForUserSync(boolean waitForUserSync) {
        mWaitForUserSync = waitForUserSync;
    }

    @Override public void tearDown() {
        // Remove activities from the stack until the app is closed.  If we don't do this, the test
        // runner sometimes has trouble launching the activity to start the next test.
        try {
            closeAllActivities();
        } catch (Exception e) {
            LOG.e("Error tearing down test case; test isolation may be broken", e);
        }
    }

    /** Closes all activities on the stack. */
    protected void closeAllActivities() throws Exception {
        try {
            for (int i = 0; i < 20; i++) {
                pressBack();
                Thread.sleep(100);
            }
        } catch (NoActivityResumedException | InterruptedException e) {
            // nothing left to close
        }
    }

    protected void screenshot(String tag) {
        try {
            Spoon.screenshot(getCurrentActivity(), tag.replace(" ", ""));
        } catch (Throwable throwable) {
            LOG.w("Could not create screenshot with tag %s", tag);
        }
    }

    /**
     * Determines the currently loaded activity, rather than {@link #getActivity()}, which will
     * always return {@link LoginActivity}.
     */
    protected Activity getCurrentActivity() throws Throwable {
        getInstrumentation().waitForIdleSync();
        final Activity[] activity = new Activity[1];
        runTestOnUiThread(new Runnable() {
            @Override public void run() {
                java.util.Collection<Activity> activities =
                    ActivityLifecycleMonitorRegistry.getInstance()
                        .getActivitiesInStage(Stage.RESUMED);
                activity[0] = Iterables.getOnlyElement(activities);
            }
        });
        return activity[0];
    }

    /** Idles until sync has completed. */
    protected void waitForInitialSync() {
        // Use a UUID as a tag so that we can wait for an arbitrary number of events, since
        // EventBusIdlingResource<> only works for a single event.
        LOG.i("Registering resource to wait for initial sync.");
        EventBusIdlingResource<SyncSucceededEvent> syncSucceededResource =
            new EventBusIdlingResource<>(UUID.randomUUID().toString(), mEventBus);
        Espresso.registerIdlingResources(syncSucceededResource);
    }

    /**
     * Adds a new patient using the new patient form.  Assumes that the UI is
     * in the location selection activity, and leaves the UI in the same
     * activity.  Note: this function will not work during {@link #setUp()}
     * as it relies on {@link #waitForProgressFragment()}.
     * @param delta        an PatientDelta containing the data for the new patient;
     *                     use Optional.absent() to leave fields unset
     * @param locationName the name of a location to assign to the new patient,
     *                     or null to leave unset (assumes this name is unique among locations)
     */
    protected void inLocationSelectionAddNewPatient(PatientDelta delta, String locationName) {
        LOG.i("Adding patient: %s (location %s)",
            delta.toContentValues().toString(), locationName);

        click(viewWithId(R.id.action_new_patient));
        expectVisible(viewWithText("New patient"));
        if (delta.id.isPresent()) {
            type(delta.id.get(), viewWithId(R.id.patient_creation_text_patient_id));
        }
        if (delta.givenName.isPresent()) {
            type(delta.givenName.get(), viewWithId(R.id.patient_creation_text_patient_given_name));
        }
        if (delta.familyName.isPresent()) {
            type(delta.familyName.get(), viewWithId(R.id.patient_creation_text_patient_family_name));
        }
        if (delta.birthdate.isPresent()) {
            Period age = new Period(delta.birthdate.get(), LocalDate.now());
            type(age.getMonths(), viewWithId(R.id.patient_creation_age_months));
            type(age.getYears(), viewWithId(R.id.patient_creation_age_years));
        }
        if (delta.gender.isPresent()) {
            if (delta.gender.get() == Patient.GENDER_MALE) {
                click(viewWithId(R.id.patient_creation_radiogroup_age_sex_male));
            } else if (delta.gender.get() == Patient.GENDER_FEMALE) {
                click(viewWithId(R.id.patient_creation_radiogroup_age_sex_female));
            }
        }
        if (delta.admissionDate.isPresent()) {
            // TODO/completeness: Support admission date in addNewPatient().
            // The following code is broken -- hopefully fixed by Espresso 2.0.
            // click(viewWithId(R.id.patient_creation_admission_date));
            // selectDateFromDatePickerDialog(mDemoPatient.admissionDate.get());
        }
        if (delta.firstSymptomDate.isPresent()) {
            // TODO/completeness: Support first symptoms date in addNewPatient().
            // The following code is broken -- hopefully fixed by Espresso 2.0.
            // click(viewWithId(R.id.patient_creation_symptoms_onset_date));
            // selectDateFromDatePickerDialog(mDemoPatient.firstSymptomDate.get());
        }
        if (delta.assignedLocationUuid.isPresent()) {
            // TODO/completeness: Support assigned location in addNewPatient().
            // A little tricky as we need to select by UUID.
            // click(viewWithId(R.id.patient_creation_button_change_location));
        }
        if (locationName != null) {
            click(viewWithId(R.id.patient_creation_button_change_location));
            click(viewWithText(locationName));
        }

        EventBusIdlingResource<ItemCreatedEvent<Patient>> resource =
            new EventBusIdlingResource<>(UUID.randomUUID().toString(), mEventBus);

        click(viewWithId(R.id.patient_creation_button_create));
        Espresso.registerIdlingResources(resource); // wait for patient to be created
    }

    // Broken, but hopefully fixed in Espresso 2.0.
    private void selectDateFromDatePickerDialog(DateTime dateTime) {
        selectDateFromDatePicker(dateTime);
        click(viewWithText("Set").inRoot(isDialog()));
    }

    protected void selectDateFromDatePicker(DateTime dateTime) {
        String year = dateTime.toString("yyyy");
        String monthOfYear = dateTime.toString("MMM");
        String dayOfMonth = dateTime.toString("dd");
        selectDateFromDatePicker(year, monthOfYear, dayOfMonth);
    }

    protected void selectDateFromDatePicker(
        @Nullable String year,
        @Nullable String monthOfYear,
        @Nullable String dayOfMonth) {
        LOG.e("Year: %s, Month: %s, Day: %s", year, monthOfYear, dayOfMonth);

        if (year != null) {
            setDateSpinner("year", year);
        }
        if (monthOfYear != null) {
            setDateSpinner("month", monthOfYear);
        }
        if (dayOfMonth != null) {
            setDateSpinner("day", dayOfMonth);
        }
    }

    // Broken, but hopefully fixed in Espresso 2.0.
    protected void setDateSpinner(String spinnerName, String value) {
        int numberPickerId =
            Resources.getSystem().getIdentifier("numberpicker_input", "id", "android");
        int spinnerId =
            Resources.getSystem().getIdentifier(spinnerName, "id", "android");
        LOG.i("%s: %s", spinnerName, value);
        LOG.i("numberPickerId: %d", numberPickerId);
        LOG.i("spinnerId: %d", spinnerId);
        type(value, viewThat(hasId(numberPickerId), whoseParent(hasId(spinnerId))));
    }

    /**
     * Ensures that a demo patient exists, creating one if necessary.  Assumes
     * that the UI is in the user login activity, and leaves the UI back in
     * the user login activity.  Note: this function will not work during
     * {@link #setUp()} as it relies on {@link #waitForProgressFragment()}.
     * TODO: Remove this method.
     * TODO: inUserLoginGoToPatientCreation and populateNewPatientFields replaces this method.
     */
    protected void inUserLoginInitDemoPatient() {
        if (sDemoPatientId != null) { // demo patient exists and is reusable
            return;
        }

        PatientDelta delta = new PatientDelta();
        String id = "" + (System.currentTimeMillis()%100000);
        delta.id = Optional.of(id);
        delta.givenName = Optional.of("Given" + id);
        delta.familyName = Optional.of("Family" + id);
        delta.firstSymptomDate = Optional.of(LocalDate.now().minusMonths(7));
        delta.gender = Optional.of(JsonPatient.GENDER_FEMALE);
        delta.birthdate = Optional.of(LocalDate.now().minusYears(12).minusMonths(3));

        // Setting location within the PatientDelta is not yet supported.
        // delta.assignedLocationUuid = Optional.of(Zones.TRIAGE_ZONE_UUID);

        inUserLoginGoToLocationSelection();
        inLocationSelectionAddNewPatient(delta, LOCATION_NAME); // add the patient
        sDemoPatientId = id; // record ID so future tests can reuse the patient
        pressBack(); // return to location list
        pressBack(); // return to user login activity
    }

    /**
     * Prevents the current demo patient from being reused for the next test.
     * The default behaviour is to reuse the same demo patient for each test;
     * if a test modifies patient data, it should call this method so that the
     * next test will use a fresh demo patient.
     */
    protected void invalidateDemoPatient() {
        sDemoPatientId = null;
    }

    /**
     * Navigates to the location selection activity with a list of all the
     * patients opened (from tapping the search button).  Assumes that the UI is
     * in the user login activity.  Note: this function will not work during
     * {@link #setUp()} as it uses {@link #waitForProgressFragment()}.
     */
    protected void inUserLoginGoToPatientList() {
        inUserLoginGoToLocationSelection();
        // There may be a small delay before the search button becomes visible;
        // the button is not displayed while locations are loading.
        expectVisibleWithin(3000, viewThat(hasId(R.id.action_search)));

        // Tap the search button to open the list of all patients.
        click(viewWithId(R.id.action_search));
    }

    /**
     * Navigates to the patient chart for the shared demo patient, creating the
     * demo patient if it doesn't exist yet.  Assumes that the UI is in the
     * user login activity.  Note: this function will not work during
     * {@link #setUp()} as it uses {@link #waitForProgressFragment()}.
     */
    protected void inUserLoginGoToDemoPatientChart() {
        inUserLoginInitDemoPatient();
        inUserLoginGoToPatientList();
        inPatientListClickPatientWithId(sDemoPatientId);
    }

    /**
     * Navigates to the patient creation activity.  Assumes that the UI is
     * in the user login activity.  Note: this function will not work during
     * {@link #setUp()} as it uses {@link #waitForProgressFragment()}.
     */
    protected void inUserLoginGoToPatientCreation() {
        inUserLoginGoToLocationSelection();
        click(viewWithId(R.id.action_new_patient));
        expectVisible(viewWithText("New patient"));
    }

    /**
     * Navigates to the location selection activity from the user login
     * activity.  Note: this function will not work during {@link #setUp()}
     * as it uses {@link #waitForProgressFragment()}.
     */
    protected void inUserLoginGoToLocationSelection() {
        click(viewWithText("Guest User"));
        waitForProgressFragment(); // wait for locations to load
    }

    /**
     * Instructs espresso to wait for the {@link ProgressFragment} contained in the current
     * activity to finish loading, if such a fragment is present. Espresso will also wait every
     * subsequent time the {@link ProgressFragment} returns to the busy state, and
     * will period check whether or not the fragment is currently idle.
     * <p/>
     * <p>If the current activity does not contain a progress fragment, then this function will
     * throw an {@link IllegalArgumentException}.
     * <p/>
     * <p>Warning: This function will not work properly in setUp() as the current activity won't
     * be available. If you need to call this function during setUp(), use
     * {@link #waitForProgressFragment(ProgressFragment)}.
     * TODO/robustness: Investigate why the current activity isn't available during setUp().
     */
    protected void waitForProgressFragment() {
        Activity activity;
        try {
            activity = getCurrentActivity();
        } catch (Throwable throwable) {
            throw new IllegalStateException("Error retrieving current activity", throwable);
        }

        if (!(activity instanceof FragmentActivity)) {
            throw new IllegalStateException("Activity is not a FragmentActivity");
        }

        FragmentActivity fragmentActivity = (FragmentActivity) activity;
        try {
            for (Fragment fragment : fragmentActivity.getSupportFragmentManager().getFragments()) {
                if (fragment instanceof ProgressFragment) {
                    waitForProgressFragment((ProgressFragment) fragment);
                    return;
                }
            }
        } catch (NullPointerException e) {
            LOG.w("Unable to wait for ProgressFragment to initialize.");
            return;
        }

        throw new IllegalStateException("Could not find a progress fragment to wait on.");
    }

    /**
     * Instructs espresso to wait for a {@link ProgressFragment} to finish loading. Espresso will
     * also wait every subsequent time the {@link ProgressFragment} returns to the busy state, and
     * will period check whether or not the fragment is currently idle.
     */
    protected void waitForProgressFragment(ProgressFragment progressFragment) {
        // Use the ProgressFragment hashCode as the identifier so that multiple ProgressFragments
        // can be tracked, but only one resource will be registered to each fragment.
        ProgressFragmentIdlingResource idlingResource = new ProgressFragmentIdlingResource(
            Integer.toString(progressFragment.hashCode()), progressFragment);
        Espresso.registerIdlingResources(idlingResource);
    }

    /** Checks that the expected zones and tents are shown. */
    protected void inLocationSelectionCheckZonesAndTentsDisplayed() {
        // Should be at location selection screen
        expectVisibleSoon(viewWithText("ALL PRESENT PATIENTS"));

        // Zones and tents should be visible
        expectVisible(viewWithText("Triage"));
        expectVisible(viewWithText(LOCATION_NAME));
        expectVisible(viewWithText("Discharged"));
    }

    /** In the location selection activity, click a location tile. */
    protected void inLocationSelectionClickLocation(String name) {
        click(viewThat(hasText(name)));
        waitForProgressFragment(); // Wait for search fragment to load.
    }

    /** In a patient list, click the first patient. */
    protected void inPatientListClickFirstPatient() {
        click(dataThat(is(Patient.class))
            .inAdapterView(hasId(R.id.fragment_patient_list))
            .atPosition(0));
    }

    /** In a patient list, click the patient with a specified ID. */
    protected void inPatientListClickPatientWithId(String id) {
        click(dataThat(isPatientWithId(id))
            .inAdapterView(hasId(R.id.fragment_patient_list))
            .atPosition(0));
    }

    /** Generates IDs to identify the newly created patient. */
    protected String generateId() {
        return "" + (new Date().getTime() % 100000);
    }

    /** Populates all the fields on the New Patient screen. */
    protected void populateNewPatientFields(String id) {
        screenshot("Before Patient Populated");
        String given = "Given" + id;
        String family = "Family" + id;
        type(id, viewWithId(R.id.patient_id));
        type(given, viewWithId(R.id.patient_given_name));
        type(family, viewWithId(R.id.patient_family_name));
        type(id.substring(id.length() - 2), viewWithId(R.id.patient_age_years));
        type(id.substring(id.length() - 2), viewWithId(R.id.patient_age_months));
        int sex = Integer.parseInt(id) % 2 == 0 ? R.id.patient_sex_female : R.id.patient_sex_male;
        click(viewWithId(sex));
        screenshot("After Patient Populated");
    }
}
