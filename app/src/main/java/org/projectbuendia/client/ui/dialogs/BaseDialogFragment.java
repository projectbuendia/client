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

package org.projectbuendia.client.ui.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.projectbuendia.client.App;
import org.projectbuendia.client.R;
import org.projectbuendia.client.ui.BaseActivity;
import org.projectbuendia.client.utils.ContextUtils;

import javax.annotation.Nonnull;

import butterknife.ButterKnife;

import static android.content.DialogInterface.BUTTON_NEGATIVE;
import static android.content.DialogInterface.BUTTON_NEUTRAL;
import static android.content.DialogInterface.BUTTON_POSITIVE;
import static android.util.TypedValue.COMPLEX_UNIT_SP;

/** Common behaviour for all dialogs. */
public abstract class BaseDialogFragment extends DialogFragment {
    protected ContextUtils u;
    protected Bundle args;
    protected AlertDialog dialog;

    private static int[] BUTTONS = {BUTTON_NEGATIVE, BUTTON_NEUTRAL, BUTTON_POSITIVE};

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        u = ContextUtils.from(getActivity());
        App.inject(this);
    }

    @Override public @Nonnull Dialog onCreateDialog(Bundle state) {
        Activity activity = getActivity();
        View view = u.inflateForDialog(getLayoutId());
        ButterKnife.inject(this, view);

        dialog = new AlertDialog.Builder(activity)
            .setView(view)
            .setCancelable(false)
            .setPositiveButton(getString(R.string.ok), null)
            .setNegativeButton(getString(R.string.cancel), null)
            .create();

        dialog.setOnShowListener(di -> {
            // Make the "Cancel" and "OK" button text match the rest of the UI text.
            for (int which : BUTTONS) {
                Button button = dialog.getButton(which);
                if (button != null) button.setTextSize(COMPLEX_UNIT_SP, 19);
            }

            // To prevent automatic dismissal of the dialog, we have to override
            // the listener instead of passing it in to setPositiveButton.
            dialog.getButton(BUTTON_POSITIVE).setOnClickListener(v -> onSubmit(dialog));

            if (activity instanceof BaseActivity) {
                ((BaseActivity) activity).onDialogOpened(BaseDialogFragment.this);
            }
            onOpen(dialog, getArguments());
        });
        return dialog;
    }

    @Override public void onDismiss(DialogInterface di) {
        Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            ((BaseActivity) activity).onDialogClosed(this);
        }
        onClose(dialog);
    }

    /** Attaches a validation error message to a text field. */
    protected void setError(TextView field, int resourceId, Object... args) {
        String message = getString(resourceId, args);
        field.setError(message);
        field.invalidate();
        field.requestFocus();
    }

    /** Returns the ID of the layout for the contents of the dialog. */
    protected abstract int getLayoutId();

    /** Invoked just after the dialog opens; use this to populate the dialog. */
    protected void onOpen(Dialog dialog, Bundle args) { }

    /** Invoked when the user taps the "OK" button; should call .dismiss() if needed. */
    protected void onSubmit(Dialog dialog) { }

    /** Invoked when the dialog is closed, either by the "Cancel" button or by .dismiss(). */
    protected void onClose(Dialog dialog) { }
}
