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

import android.content.Context;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.projectbuendia.client.App;
import org.projectbuendia.client.R;

import java.util.Locale;

/** A {@link Toast} with a large text size. */
public final class BigToast {
    /** Displays a toast with the given message. */
    public static void show(String message) {
        Context context = App.getContext();
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
        LinearLayout layout = (LinearLayout) toast.getView();
        TextView view = (TextView) layout.getChildAt(0);
        view.setTextAppearance(context, R.style.text_large_white);
        toast.show();
    }

    /**  Displays a toast with the given formatted string resource. */
    public static void show(int messageId, Object... args) {
        show(App.str(messageId), args);
    }

    /** Displays a toast with the given formatted string. */
    public static void show(String message, Object... args) {
        Context context = App.getContext();
        Locale locale = context.getResources().getConfiguration().locale;
        show(String.format(locale, message, args));
    }

    private BigToast() {
        // Prevent instantiation.
    }
}
