package org.msf.records.ui;

import android.content.Context;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/** A Toast with a large text size. */
public final class BigToast {
    public static void show(Context context, String message) {
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
        LinearLayout layout = (LinearLayout) toast.getView();
        TextView view = (TextView) layout.getChildAt(0);
        view.setTextSize(48);
        toast.show();
    }

    public static void show(Context context, String message, Object... args) {
        show(context,
                String.format(context.getResources().getConfiguration().locale, message, args));
    }

    private BigToast() {
        // Prevent instantiation.
    }
}