package org.msf.records.ui;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.test.ActivityInstrumentationTestCase2;
import android.view.View;

import com.google.android.apps.common.testing.testrunner.ActivityLifecycleMonitorRegistry;
import com.google.android.apps.common.testing.testrunner.Stage;
import com.google.android.apps.common.testing.ui.espresso.IdlingPolicies;
import com.google.android.apps.common.testing.ui.espresso.NoActivityResumedException;
import com.google.common.collect.Iterables;
import com.squareup.spoon.Spoon;
import com.google.android.apps.common.testing.ui.espresso.Espresso;

import org.hamcrest.Matcher;
import org.msf.records.events.sync.SyncFinishedEvent;
import org.msf.records.events.sync.SyncStartedEvent;
import org.msf.records.events.sync.SyncSucceededEvent;
import org.msf.records.events.user.KnownUsersLoadedEvent;
import org.msf.records.sync.GenericAccountService;
import org.msf.records.ui.sync.EventBusIdlingResource;
import org.msf.records.ui.userlogin.UserLoginActivity;
import org.msf.records.utils.Logger;
import org.msf.records.utils.EventBusRegistrationInterface;
import org.msf.records.utils.EventBusWrapper;

import java.util.UUID;

import de.greenrobot.event.EventBus;

import java.util.concurrent.TimeUnit;

import static com.google.android.apps.common.testing.ui.espresso.Espresso.onView;
import static com.google.android.apps.common.testing.ui.espresso.Espresso.pressBack;
import static com.google.android.apps.common.testing.ui.espresso.assertion.ViewAssertions.matches;
import static com.google.android.apps.common.testing.ui.espresso.matcher.ViewMatchers.isDisplayed;

// All tests have to launch the UserLoginActivity first because the app expects a user to log in.
public class FunctionalTestCase extends ActivityInstrumentationTestCase2<UserLoginActivity> {
    private static final Logger LOG = Logger.create();
    private static final int DEFAULT_VIEW_CHECKER_TIMEOUT = 30000;

    private boolean mWaitForUserSync = true;

    protected EventBusRegistrationInterface mEventBus;

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


    private class SyncCounter {
        public int inProgressSyncCount = 0;

        public void onEventMainThread(SyncStartedEvent event) {
            inProgressSyncCount++;
        }

        public void onEventMainThread(SyncFinishedEvent event) {
            inProgressSyncCount--;
        }
    }

    /**
     * Closes all activities on the stack.
     */
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
