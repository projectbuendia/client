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

import android.support.test.espresso.NoMatchingViewException;
import android.view.View;
import android.widget.Toast;

import junit.framework.Assert;

import org.projectbuendia.client.R;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.RootMatchers.withDecorView;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNot.not;

public class SnackBarTest extends FunctionalTestCase {
    public void testSimpleMessageSnackBar() {
        final BaseActivity activity = getActivity();
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.snackBar("This is a SnackBar Test!");
            }
        });
        expectVisibleSoon(viewWithText("This is a SnackBar Test!"));
    }

    public void testSnackBarWithAction() {
        final BaseActivity activity = getActivity();
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.snackBar("This is a SnackBar with action Test!", "Action", new View
                    .OnClickListener() {
                    @Override public void onClick(View v) {
                        Toast.makeText(
                            activity,
                            "What?",
                            Toast.LENGTH_LONG
                        ).show();
                    }
                });
            }
        });
        expectVisibleSoon(viewWithText("This is a SnackBar with action Test!"));
        expectVisible(viewWithId(R.id.snackbar_action));
        expectVisible(viewThat(hasText("Action")));
        click(viewWithText("Action"));
        onView(withText("What?"))
            .inRoot(withDecorView(not(is(
                getActivity().getWindow().getDecorView())))
            ).check(matches(isDisplayed()));
    }

    public void testSnackBarDismiss() {
        final BaseActivity activity = getActivity();
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.snackBar("This is a Dismissible SnackBar Test!", null, null, 1, true, 0);
            }
        });
        expectVisibleSoon(viewWithText("This is a Dismissible SnackBar Test!"));
        expectVisible(viewWithId(R.id.snackbar_dismiss));
        click(viewWithId(R.id.snackbar_dismiss));

        try {
            viewWithText("This is a Dismissible SnackBar Test!").check(matches(isDisplayed()));
            Assert.fail("Should have thrown NoMatchingViewException.");
        } catch(NoMatchingViewException e) {}

    }
}