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

import androidx.test.annotation.UiThreadTest;

import org.junit.Assert;
import org.projectbuendia.client.R;

import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class SnackBarTest extends FunctionalTestCase {
    @UiThreadTest
    public void testSimpleMessageSnackBar() {
        final BaseActivity activity = (BaseActivity) getActivity();
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.snackBar(R.string.troubleshoot_wifi_disabled);
            }
        });
        expectVisibleSoon(viewWithText("Wifi is disabled"));
    }

    @UiThreadTest
    public void testSnackBarWithAction() {
        final View.OnClickListener mockListener = mock(View.OnClickListener.class);
        final BaseActivity activity = (BaseActivity) getActivity();
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.snackBar(R.string.troubleshoot_wifi_disabled, R.string.troubleshoot_wifi_disabled_action_enable, mockListener);
            }
        });
        expectVisibleSoon(viewWithText("Wifi is disabled"));
        expectVisible(viewWithId(R.id.snackbar_action));
        expectVisible(viewThat(hasText("Enable")));
        click(viewWithText("Enable"));
        verify(mockListener).onClick(any(View.class));
    }

    @UiThreadTest
    public void testSnackBarDismiss() {
        final BaseActivity activity = (BaseActivity) getActivity();
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.snackBar(R.string.troubleshoot_wifi_disabled, 0, null, 1, true, 0);
            }
        });
        expectVisibleSoon(viewWithText("Wifi is disabled"));
        expectVisible(viewWithId(R.id.snackbar_dismiss));
        click(viewWithId(R.id.snackbar_dismiss));

        try {
            viewWithText("Wifi is disabled").check(matches(isDisplayed()));
            Assert.fail("Should have thrown NoMatchingViewException.");
        } catch(NoMatchingViewException e) {}

    }
}
