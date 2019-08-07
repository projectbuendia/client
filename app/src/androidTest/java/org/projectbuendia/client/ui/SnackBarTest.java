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

import org.junit.Assert;
import org.junit.Test;
import org.projectbuendia.client.R;

import androidx.test.annotation.UiThreadTest;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static org.mockito.Mockito.mock;

public class SnackBarTest extends FunctionalTestCase {
    public static final String WIFI_DISABLED_MESSAGE = "Wi-Fi is disabled";

    @Test
    @UiThreadTest
    public void testSimpleMessageSnackBar() {
        final BaseActivity activity = getActivity();
        activity.runOnUiThread(() -> activity.snackBar(R.string.troubleshoot_wifi_disabled));
        expectVisibleSoon(viewWithText(WIFI_DISABLED_MESSAGE));
    }

    @Test
    public void testSnackBarWithAction() {
        final View.OnClickListener mockListener = mock(View.OnClickListener.class);
        final BaseActivity activity = getActivity();
        getInstrumentation().runOnMainSync(() -> activity.snackBar(R.string.troubleshoot_wifi_disabled, R.string.troubleshoot_wifi_disabled_action_enable, mockListener));
        expectVisibleSoon(viewWithText(WIFI_DISABLED_MESSAGE));
        expectVisible(viewWithId(R.id.snackbar_action));
        expectVisible(viewThat(hasText("Enable")));
        click(viewWithText("Enable"));
//        TODO(sdspikes): figure out why the mock listener isn't getting triggered by the click
//        verify(mockListener).onClick(any(View.class));
    }

    @Test
    @UiThreadTest
    public void testSnackBarDismiss() {
        final BaseActivity activity = getActivity();
        activity.runOnUiThread(() -> activity.snackBar(R.string.troubleshoot_wifi_disabled, 0, null, 1, true, 0));
        expectVisibleSoon(viewWithText(WIFI_DISABLED_MESSAGE));
        expectVisible(viewWithId(R.id.snackbar_dismiss));
        click(viewWithId(R.id.snackbar_dismiss));

        try {
            viewWithText(WIFI_DISABLED_MESSAGE).check(matches(isDisplayed()));
            Assert.fail("Should have thrown NoMatchingViewException.");
        } catch(NoMatchingViewException e) {}


    }
}
