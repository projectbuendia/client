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

import org.projectbuendia.client.R;

/** A {@link Toast} with a large text size. */
public final class BigToast {
    /**
     * Displays a toast with the given message resource.
     * @param context         the Application or Activity context to use
     * @param messageResource the message to display
     */
    public static void show(Context context, int messageResource) {
        show(context, context.getResources().getString(messageResource));
    }

    /**
     * Displays a toast with the given message.
     * @param context the Application or Activity context to use
     * @param message the message to display
     */
    public static void show(Context context, String message) {
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
        LinearLayout layout = (LinearLayout) toast.getView();
        TextView view = (TextView) layout.getChildAt(0);
        view.setTextAppearance(context, R.style.text_large_white);
        toast.show();
    }

    /**
     * Displays a toast with the given formatted string resource.
     * @param context         the Application or Activity context to use
     * @param messageResource the message to display, with placeholders for substitution
     * @param args            arguments to substitute into the message
     */
    public static void show(Context context, int messageResource, Object... args) {
        show(context, context.getResources().getString(messageResource), args);
    }

    /**
     * Displays a toast with the given formatted string.
     * @param context the Application or Activity context to use
     * @param message the message to display, with placeholders for substitution
     * @param args    arguments to substitute into the message
     */
    public static void show(Context context, String message, Object... args) {
        show(context,
            String.format(context.getResources().getConfiguration().locale, message, args));
    }

    private BigToast() {
        // Prevent instantiation.
    }
}
