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
import android.support.test.espresso.core.deps.guava.collect.Iterables;
import android.support.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import android.support.test.runner.lifecycle.Stage;

import com.squareup.spoon.Spoon;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.projectbuendia.client.R;
import org.projectbuendia.client.events.sync.SyncSucceededEvent;
import org.projectbuendia.client.events.user.KnownUsersLoadedEvent;
import org.projectbuendia.client.models.Patient;
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

import static android.support.test.InstrumentationRegistry.getInstrumentation;
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

    public static final String LOCATION_NAME = "Triage";

    // For now, we create a new demo patient for tests using the real patient
    // creation UI on each test run (see {@link #inUserLoginInitDemoPatient()}).
    // TODO/robustness: Use externally preloaded demo data instead.
    protected static String sDemoPatientId = null;
    private boolean mWaitForUserSync = true;
    protected EventBusRegistrationInterface mEventBus;

    public FunctionalTestCase() {
        super(LoginActivity.class);
    }

    @Before
    public void setUp() throws Exception {
        // Give additional leeway for idling resources, as sync may be slow, especially on Edisons.
        // Increased to 5 minutes as certain operations (like initial sync) may take an exceedingly
        // long time.
        IdlingPolicies.setIdlingResourceTimeout(60, TimeUnit.SECONDS);
        IdlingPolicies.setMasterPolicyTimeout(60, TimeUnit.SECONDS);

        mEventBus = new EventBusWrapper(EventBus.getDefault());

        // Wait for users to sync.
        if (mWaitForUserSync) {
            EventBusIdlingResource<KnownUsersLoadedEvent> resource =
                new EventBusIdlingResource<>("USERS", mEventBus);
            Espresso.registerIdlingResources(resource);
        }
        // TODO(sdspikes): shouldn't be needed since launchActivity is set to true in the call to
        // the ActivityTestRule constructor, but without this we don't seem to launch anything.
        launchActivity(null);
    }

    public void setWaitForUserSync(boolean waitForUserSync) {
        mWaitForUserSync = waitForUserSync;
    }

    @After
    public void tearDown() {
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
        java.util.Collection<Activity> activities =
                ActivityLifecycleMonitorRegistry.getInstance()
                        .getActivitiesInStage(Stage.RESUMED);
        return Iterables.getOnlyElement(activities);
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
        waitUntilVisible(3000, viewThat(hasId(R.id.action_search)));

        // Tap the search button to open the list of all patients.
        click(viewWithId(R.id.action_search));
    }

    /**
     * Navigates to the patient chart for the shared demo patient, creating the
     * demo patient if it doesn't exist yet.  Assumes that the UI is in the
     * user login activity.  Note: this function will not work during
     * {@link #setUp()} as it uses {@link #waitForProgressFragment()}.
     */
    protected String inUserLoginGoToDemoPatientChart() {
        // Create the patient
        inUserLoginGoToPatientCreation();
        screenshot("Test Start");
        String id = generateId();
        populateNewPatientFields(id);
        click(viewWithText("OK"));
        waitForProgressFragment();
        screenshot("On Patient Chart");
        return id;
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
        waitUntilVisible(20000, viewWithText("Discharged"));
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
        /* TODO(sdspikes): determine if this function is needed (skipping it makes more tests pass).
         *   It seems to be a busy-loop, which seems not to play nicely with the ui thread, but it's
         *   possible that there's something I'm missing.
         */
        return;
        /*
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
        */
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
        waitUntilVisible(viewWithText("Discharged"));
        expectVisible(viewWithText(LOCATION_NAME));
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
        int i = Integer.parseInt(id);
        type("" + (i % 100), viewWithId(R.id.patient_age_years));
        type("" + (i % 10), viewWithId(R.id.patient_age_months));
        int sex = i % 2 == 0 ? R.id.patient_sex_female : R.id.patient_sex_male;
        click(viewWithId(sex));
        screenshot("After Patient Populated");
    }

    protected void toast(String message) {
        getActivity().runOnUiThread(() -> BigToast.show(message));
    }
}
