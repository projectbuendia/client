package org.msf.records.ui;

import android.test.ActivityInstrumentationTestCase2;

import com.google.android.apps.common.testing.ui.espresso.NoActivityResumedException;

import org.msf.records.ui.userlogin.UserLoginActivity;

import static com.google.android.apps.common.testing.ui.espresso.Espresso.pressBack;

// All tests have to launch the UserLoginActivity first because the app expects a user to log in.
public class FunctionalTestCase extends ActivityInstrumentationTestCase2<UserLoginActivity> {
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
}