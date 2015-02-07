package org.msf.records.ui;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.test.ActivityInstrumentationTestCase2;

import com.google.android.apps.common.testing.testrunner.ActivityLifecycleMonitorRegistry;
import com.google.android.apps.common.testing.testrunner.Stage;
import com.google.common.collect.Iterables;
import com.squareup.spoon.Spoon;
import com.google.android.apps.common.testing.ui.espresso.Espresso;

import org.msf.records.TestCleanupHelper;
import org.msf.records.events.user.KnownUsersLoadedEvent;
import org.msf.records.ui.sync.EventBusIdlingResource;
import org.msf.records.ui.userlogin.UserLoginActivity;
import org.msf.records.utils.Logger;
import org.msf.records.utils.EventBusRegistrationInterface;
import org.msf.records.utils.EventBusWrapper;

import de.greenrobot.event.EventBus;

// All tests have to launch the UserLoginActivity first because the app expects a user to log in.
public class FunctionalTestCase extends ActivityInstrumentationTestCase2<UserLoginActivity> {
    private static final Logger LOG = Logger.create();

    protected EventBusRegistrationInterface mEventBus;

    public FunctionalTestCase() {
        super(UserLoginActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        mEventBus = new EventBusWrapper(EventBus.getDefault());

        // Wait for users to sync.
        EventBusIdlingResource<KnownUsersLoadedEvent> resource =
                new EventBusIdlingResource<>("USERS", mEventBus);
        Espresso.registerIdlingResources(resource);

        super.setUp();
        getActivity();
    }

    @Override
    public void tearDown() {
        // Remove activities from the stack until the app is closed.  If we don't do this, the test
        // runner sometimes has trouble launching the activity to start the next test.
        try {
            TestCleanupHelper.closeAllActivities(getInstrumentation());
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
        ProgressFragmentIdlingResource idlingResource = new ProgressFragmentIdlingResource(
                "progress_fragment", progressFragment);
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

        for (Fragment fragment :
                ((FragmentActivity)activity).getSupportFragmentManager().getFragments()) {
            if (fragment instanceof ProgressFragment) {
                waitForProgressFragment((ProgressFragment)fragment);
                return;
            }
        }

        throw new IllegalStateException("Could not find a progress fragment to wait on.");
    }
}