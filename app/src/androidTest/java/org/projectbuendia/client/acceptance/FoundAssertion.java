package org.projectbuendia.client.acceptance;

import android.support.test.espresso.NoMatchingViewException;
import android.support.test.espresso.ViewAssertion;
import android.view.View;

public class FoundAssertion implements ViewAssertion {
    public static ViewAssertion exists() {
        return new FoundAssertion();
    }

    @Override public void check(View view, NoMatchingViewException noViewFoundException) {
        if (noViewFoundException != null) {
            throw noViewFoundException;
        }
    }
}
