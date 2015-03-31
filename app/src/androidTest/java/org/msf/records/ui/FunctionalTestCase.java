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

package org.msf.records.ui;

import android.app.Activity;
import android.content.res.Resources;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.test.ActivityInstrumentationTestCase2;
import android.view.View;

import com.google.android.apps.common.testing.testrunner.ActivityLifecycleMonitorRegistry;
import com.google.android.apps.common.testing.testrunner.Stage;
import com.google.android.apps.common.testing.ui.espresso.Espresso;
import com.google.android.apps.common.testing.ui.espresso.IdlingPolicies;
import com.google.android.apps.common.testing.ui.espresso.NoActivityResumedException;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.squareup.spoon.Spoon;

import org.hamcrest.Matcher;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.msf.records.R;
import org.msf.records.data.app.AppPatient;
import org.msf.records.data.app.AppPatientDelta;
import org.msf.records.events.data.SingleItemCreatedEvent;
import org.msf.records.events.sync.SyncFinishedEvent;
import org.msf.records.events.sync.SyncStartedEvent;
import org.msf.records.events.sync.SyncSucceededEvent;
import org.msf.records.events.user.KnownUsersLoadedEvent;
import org.msf.records.net.model.Patient;
import org.msf.records.ui.sync.EventBusIdlingResource;
import org.msf.records.ui.userlogin.UserLoginActivity;
import org.msf.records.utils.EventBusRegistrationInterface;
import org.msf.records.utils.EventBusWrapper;
import org.msf.records.utils.Logger;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import de.greenrobot.event.EventBus;

import static com.google.android.apps.common.testing.ui.espresso.Espresso.onData;
import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.Espresso.pressBack;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.click;
import static com.google.android.apps.common.testing.ui.espresso.action.ViewActions.typeText;
import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.matches;
import static com.google.android.apps.common.testing.ui.espresso.matcher.RootMatchers.isDialog;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isDisplayed;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withId;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withParent;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.msf.records.ui.matchers.AppPatientMatchers.isPatientWithId;

/**
 * Base class for functional tests that sets timeouts to be permissive, optionally logs in as a
 * user before continuing, and provides some utility functions for convenience.
 */
public class FunctionalTestCase extends ActivityInstrumentationTestCase2<UserLoginActivity> {
    private static final Logger LOG = Logger.create();
    private static final int DEFAULT_VIEW_CHECKER_TIMEOUT = 30000;

    private boolean mWaitForUserSync = true;

    protected EventBusRegistrationInterface mEventBus;
    // For now, we create a new demo patient for tests using the real patient
    // creation UI on each test run (see {@link #inUserLoginInitDemoPatient()}).
    // TODO/robustness: Use externally preloaded demo data instead.
    protected static String sDemoPatientId = null;

    public FunctionalTestCase() {
        super(UserLoginActivity.class);
    }

    @Override
    public void setUp() throws Exception {
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

    @Override
    public void tearDown() {
        // Remove activities from the stack until the app is closed.  If we don't do this, the test
        // runner sometimes has trouble launching the activity to start the next test.
        try {
            closeAllActivities();
        } catch (Exception e) {
            LOG.e("Error tearing down test case, test isolation may be broken.", e);
        }
    }

    /**
     * Determines the currently loaded activity, rather than {@link #getActivity()}, which will
     * always return {@link UserLoginActivity}.
     */
    protected Activity getCurrentActivity() throws Throwable {
        getInstrumentation().waitForIdleSync();
        final Activity[] activity = new Activity[1];
        runTestOnUiThread(new Runnable() {
            @Override
            public void run() {
                java.util.Collection<Activity> activities =
                        ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(
                                Stage.RESUMED);
                activity[0] = Iterables.getOnlyElement(activities);
            }});
        return activity[0];
    }

    protected void screenshot(String tag) {
        try {
            Spoon.screenshot(getCurrentActivity(), tag.replace(" ", ""));
        } catch (Throwable throwable) {
            LOG.w("Could not create screenshot with tag %s", tag);
        }
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

    /**
     * Instructs espresso to wait for the {@link ProgressFragment} contained in the current
     * activity to finish loading, if such a fragment is present. Espresso will also wait every
     * subsequent time the {@link ProgressFragment} returns to the busy state, and
     * will period check whether or not the fragment is currently idle.
     *
     * <p>If the current activity does not contain a progress fragment, then this function will
     * throw an {@link IllegalArgumentException}.
     *
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
            throw new IllegalStateException("Error retrieving current activity.", throwable);
        }

        if (!(activity instanceof FragmentActivity)) {
            throw new IllegalStateException("Activity is not a FragmentActivity.");
        }

        FragmentActivity fragmentActivity = (FragmentActivity)activity;
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

    /** Idles until sync has completed. */
    protected void waitForInitialSync() {
        // Use a UUID as a tag so that we can wait for an arbitrary number of events, since
        // EventBusIdlingResource<> only works for a single event.
        LOG.i("Registering resource to wait for initial sync.");
        EventBusIdlingResource<SyncSucceededEvent> syncSucceededResource =
                new EventBusIdlingResource<>(UUID.randomUUID().toString(), mEventBus);
        Espresso.registerIdlingResources(syncSucceededResource);
    }

    protected void checkViewDisplayedSoon(Matcher<View> matcher) {
        checkViewDisplayedWithin(matcher, DEFAULT_VIEW_CHECKER_TIMEOUT);
    }

    protected void checkViewDisplayedWithin(Matcher<View> matcher, int timeoutMs) {
        long timeoutTime = System.currentTimeMillis() + timeoutMs;
        boolean viewFound = false;
        Throwable viewAssertionError = null;
        while (timeoutTime > System.currentTimeMillis() && !viewFound) {
            try {
                onView(matcher).check(matches(isDisplayed()));
                viewFound = true;
            } catch (Throwable t) {
                viewAssertionError = t;
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e1) {
                    LOG.w("Sleep interrupted, yielding instead.");
                    Thread.yield();
                }
            }
        }

        if (!viewFound) {
            throw new RuntimeException(viewAssertionError);
        }
    }

    /**
     * Adds a new patient using the new patient form.  Assumes that the UI is
     * in the location selection activity, and leaves the UI in the same
     * activity.  Note: this function will not work during {@link #setUp()}
     * as it relies on {@link #waitForProgressFragment()}.
     * @param delta an AppPatientDelta containing the data for the new patient;
     *     use Optional.absent() to leave fields unset
     * @param locationName the name of a location to assign to the new patient,
     *     or null to leave unset (assumes this name is unique among locations)
     */
    protected void inLocationSelectionAddNewPatient(AppPatientDelta delta, String locationName) {
        LOG.i("Adding patient: %s (location %s)",
                delta.toContentValues().toString(), locationName);

        onView(withId(R.id.action_add)).perform(click());
        onView(withText("New Patient")).check(matches(isDisplayed()));
        if (delta.id.isPresent()) {
            onView(withId(R.id.patient_creation_text_patient_id))
                    .perform(typeText(delta.id.get()));
        }
        if (delta.givenName.isPresent()) {
            onView(withId(R.id.patient_creation_text_patient_given_name))
                    .perform(typeText(delta.givenName.get()));
        }
        if (delta.familyName.isPresent()) {
            onView(withId(R.id.patient_creation_text_patient_family_name))
                    .perform(typeText(delta.familyName.get()));
        }
        if (delta.birthdate.isPresent()) {
            Period age = new Period(delta.birthdate.get().toLocalDate(), LocalDate.now());
            if (age.getYears() < 1) {
                onView(withId(R.id.patient_creation_text_age))
                        .perform(typeText(Integer.toString(age.getMonths())));
                onView(withId(R.id.patient_creation_radiogroup_age_units_months)).perform(click());
            } else {
                onView(withId(R.id.patient_creation_text_age))
                        .perform(typeText(Integer.toString(age.getYears())));
                onView(withId(R.id.patient_creation_radiogroup_age_units_years)).perform(click());
            }
        }
        if (delta.gender.isPresent()) {
            if (delta.gender.get() == AppPatient.GENDER_MALE) {
                onView(withId(R.id.patient_creation_radiogroup_age_sex_male)).perform(click());
            } else if (delta.gender.get() == AppPatient.GENDER_FEMALE) {
                onView(withId(R.id.patient_creation_radiogroup_age_sex_female)).perform(click());
            }
        }
        if (delta.admissionDate.isPresent()) {
            // TODO/completeness: Support admission date in addNewPatient().
            // The following code is broken -- hopefully fixed by Espresso 2.0.
            // onView(withId(R.id.patient_creation_admission_date)).perform(click());
            // selectDateFromDatePickerDialog(mDemoPatient.admissionDate.get());
        }
        if (delta.firstSymptomDate.isPresent()) {
            // TODO/completeness: Support first symptoms date in addNewPatient().
            // The following code is broken -- hopefully fixed by Espresso 2.0.
            // onView(withId(R.id.patient_creation_symptoms_onset_date)).perform(click());
            // selectDateFromDatePickerDialog(mDemoPatient.firstSymptomDate.get());
        }
        if (delta.assignedLocationUuid.isPresent()) {
            // TODO/completeness: Support assigned location in addNewPatient().
            // A little tricky as we need to select by UUID.
            // onView(withId(R.id.patient_creation_button_change_location)).perform(click());
        }
        if (locationName != null) {
            onView(withId(R.id.patient_creation_button_change_location)).perform(click());
            onView(withText(locationName)).perform(click());
        }

        EventBusIdlingResource<SingleItemCreatedEvent<AppPatient>> resource =
                new EventBusIdlingResource<>(UUID.randomUUID().toString(), mEventBus);

        onView(withId(R.id.patient_creation_button_create)).perform(click());
        Espresso.registerIdlingResources(resource); // wait for patient to be created
    }

    // Broken, but hopefully fixed in Espresso 2.0.
    private void selectDateFromDatePickerDialog(DateTime dateTime) {
        selectDateFromDatePicker(dateTime);
        onView(withText("Set"))
                .inRoot(isDialog())
                .perform(click());
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

    protected void selectDateFromDatePicker(DateTime dateTime) {
        String year = dateTime.toString("yyyy");
        String monthOfYear = dateTime.toString("MMM");
        String dayOfMonth = dateTime.toString("dd");

        selectDateFromDatePicker(year, monthOfYear, dayOfMonth);
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
        onView(allOf(withId(numberPickerId), withParent(withId(spinnerId))))
                .check(matches(isDisplayed()))
                .perform(typeText(value));
    }

    /**
     * Ensures that a demo patient exists, creating one if necessary.  Assumes
     * that the UI is in the user login activity, and leaves the UI back in
     * the user login activity.  Note: this function will not work during
     * {@link #setUp()} as it relies on {@link #waitForProgressFragment()}.
     */
    protected void inUserLoginInitDemoPatient() {
        if (sDemoPatientId != null) { // demo patient exists and is reusable
            return;
        }

        AppPatientDelta delta = new AppPatientDelta();
        String id = "" + (System.currentTimeMillis() % 100000);
        delta.id = Optional.of(id);
        delta.givenName = Optional.of("Given" + id);
        delta.familyName = Optional.of("Family" + id);
        delta.firstSymptomDate = Optional.of(LocalDate.now().minusMonths(7));
        delta.gender = Optional.of(Patient.GENDER_FEMALE);
        delta.birthdate = Optional.of(DateTime.now().minusYears(12).minusMonths(3));
        // Setting location within the AppPatientDelta is not yet supported.
        // delta.assignedLocationUuid = Optional.of(Zone.TRIAGE_ZONE_UUID);

        inUserLoginGoToLocationSelection();
        inLocationSelectionAddNewPatient(delta, "S1"); // add the patient
        sDemoPatientId = id; // record ID so future tests can reuse the patient
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
     * Navigates to the location selection activity from the user login
     * activity.  Note: this function will not work during {@link #setUp()}
     * as it uses {@link #waitForProgressFragment()}.
     */
    protected void inUserLoginGoToLocationSelection() {
        onView(withText("Guest User")).perform(click());
        waitForProgressFragment(); // wait for locations to load
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
        checkViewDisplayedWithin(withId(R.id.action_search), 3000);

        // Tap the search button to open the list of all patients.
        onView(withId(R.id.action_search)).perform(click());
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
        onView(withId(R.id.action_add)).perform(click());
        onView(withText("New Patient")).check(matches(isDisplayed()));
    }


    /** Checks that the expected zones and tents are shown. */
    protected void inLocationSelectionCheckZonesAndTentsDisplayed() {
        // Should be at location selection screen
        checkViewDisplayedSoon(withText("ALL PRESENT PATIENTS"));

        // Zones and tents should be visible
        onView(withText("Triage")).check(matches(isDisplayed()));
        onView(withText("S1")).check(matches(isDisplayed()));
        onView(withText("S2")).check(matches(isDisplayed()));
        onView(withText("P1")).check(matches(isDisplayed()));
        onView(withText("P2")).check(matches(isDisplayed()));
        onView(withText("C1")).check(matches(isDisplayed()));
        onView(withText("C2")).check(matches(isDisplayed()));
        onView(withText("Discharged")).check(matches(isDisplayed()));
    }

    /** In the location selection activity, click a location tile. */
    protected void inLocationSelectionClickLocation(String name) {
        onView(withText(name)).perform(click());
        waitForProgressFragment(); // Wait for search fragment to load.
    }

    /** In a patient list, click the first patient. */
    protected void inPatientListClickFirstPatient() {
        onData(is(AppPatient.class))
                .inAdapterView(withId(R.id.fragment_patient_list))
                .atPosition(0)
                .perform(click());
    }

    /** In a patient list, click the patient with a specified ID. */
    protected void inPatientListClickPatientWithId(String id) {
        onData(isPatientWithId(equalTo(id)))
                .inAdapterView(withId(R.id.fragment_patient_list))
                .atPosition(0)
                .perform(click());
    }

    private class SyncCounter {
        public int inProgressSyncCount = 0;

        public void onEventMainThread(SyncStartedEvent event) {
            inProgressSyncCount++;
        }

        public void onEventMainThread(SyncFinishedEvent event) {
            inProgressSyncCount--;
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
}
