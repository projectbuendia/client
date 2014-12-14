package org.msf.records.utils;

import android.content.Context;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/** A Toast with a large text size. */
public class BigToast {
    public static void show(Context context, String message) {
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_LONG);
        LinearLayout layout = (LinearLayout) toast.getView();
        TextView view = (TextView) layout.getChildAt(0);
        view.setTextSize(48);
        toast.show();
    }
}