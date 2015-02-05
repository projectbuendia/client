package org.msf.records.ui;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.test.ActivityInstrumentationTestCase2;

import com.google.android.apps.common.testing.testrunner.ActivityLifecycleMonitorRegistry;
import com.google.android.apps.common.testing.testrunner.Stage;
import com.google.android.apps.common.testing.ui.espresso.Espresso;
import com.google.android.apps.common.testing.ui.espresso.NoActivityResumedException;
import com.google.common.collect.Iterables;

import org.msf.records.events.user.KnownUsersLoadedEvent;
import org.msf.records.ui.sync.EventBusIdlingResource;
import org.msf.records.ui.userlogin.UserLoginActivity;
import org.msf.records.utils.EventBusRegistrationInterface;
import org.msf.records.utils.EventBusWrapper;

import de.greenrobot.event.EventBus;

import static com.google.android.apps.common.testing.ui.espresso.Espresso.pressBack;

// All tests have to launch the UserLoginActivity first because the app expects a user to log in.
public class FunctionalTestCase extends ActivityInstrumentationTestCase2<UserLoginActivity> {
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
        try {
            // Keep pressing back until the app is closed.  If we don't do this, the test
            // runner sometimes has trouble launching the activity to start the next test.
            for (int i = 0; i < 10; i++) {
                Thread.sleep(100, 0);
                pressBack();
            }
        } catch (NoActivityResumedException | InterruptedException e) {
            // app closed
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

    /** Instructs espresso to wait for a {@link ProgressFragment} to finish loading. */
    protected void waitForProgressFragment(ProgressFragment progressFragment) {
        ProgressFragmentIdlingResource idlingResource = new ProgressFragmentIdlingResource(
                "progress_fragment", progressFragment);
        Espresso.registerIdlingResources(idlingResource);
    }

    /** Instructs espresso to wait for the first {@link ProgressFragment} found for an activity. */
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