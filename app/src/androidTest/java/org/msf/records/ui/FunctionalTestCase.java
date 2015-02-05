package org.msf.records.ui;

import android.app.Activity;
import android.test.ActivityInstrumentationTestCase2;

import com.google.android.apps.common.testing.testrunner.ActivityLifecycleMonitorRegistry;
import com.google.android.apps.common.testing.testrunner.Stage;
import com.google.android.apps.common.testing.ui.espresso.NoActivityResumedException;
import com.google.common.collect.Iterables;
import com.squareup.spoon.Spoon;

import org.msf.records.ui.userlogin.UserLoginActivity;
import org.msf.records.utils.Logger;

import static com.google.android.apps.common.testing.ui.espresso.Espresso.pressBack;

// All tests have to launch the UserLoginActivity first because the app expects a user to log in.
public class FunctionalTestCase extends ActivityInstrumentationTestCase2<UserLoginActivity> {
    private final Logger LOG = Logger.create();

    public FunctionalTestCase() {
        super(UserLoginActivity.class);
    }

    @Override
    public void setUp() throws Exception {
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

    protected void screenshot(String tag) {
        try {
            Spoon.screenshot(getCurrentActivity(), tag.replace(" ", ""));
        } catch (Throwable throwable) {
            LOG.w("Could not create screenshot with tag %s", tag);
        }
    }
}