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
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import org.projectbuendia.client.R;
import org.projectbuendia.client.ui.BaseActivity;
import org.projectbuendia.client.utils.ContextUtils;

import javax.annotation.Nonnull;

import butterknife.ButterKnife;

import static android.util.TypedValue.COMPLEX_UNIT_SP;

/** Common behaviour for all dialogs. */
public abstract class BaseDialogFragment<T extends BaseDialogFragment> extends DialogFragment {
    public static int BUTTON_NEGATIVE = DialogInterface.BUTTON_NEGATIVE;
    public static int BUTTON_NEUTRAL = DialogInterface.BUTTON_NEUTRAL;
    public static int BUTTON_POSITIVE = DialogInterface.BUTTON_POSITIVE;
    public static int[] BUTTONS = {BUTTON_NEGATIVE, BUTTON_NEUTRAL, BUTTON_POSITIVE};

    protected ContextUtils u;
    protected AlertDialog dialog;

    public T withArgs(Bundle args) {
        setArguments(args);
        return (T) this;
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        u = ContextUtils.from(getActivity());
    }

    /** Subclasses should implement onCreateDialog() by calling createAlertDialog(). */
    protected @Nonnull AlertDialog createAlertDialog(int layoutId) {
        Activity activity = getActivity();
        View view = u.inflateForDialog(layoutId);
        ButterKnife.inject(this, view);

        dialog = new AlertDialog.Builder(activity)
            .setTitle("Dialog")  // without this, there is no title bar
            .setView(view)
            .setPositiveButton(getString(R.string.ok), null)
            .setNegativeButton(getString(R.string.cancel), null)
            .create();

        dialog.setCanceledOnTouchOutside(false);

        dialog.setOnShowListener(di -> {
            // Make the "Cancel" and "OK" button text match the rest of the UI text.
            for (int which : BUTTONS) {
                Button button = dialog.getButton(which);
                if (button != null) button.setTextSize(COMPLEX_UNIT_SP, 19);
            }

            // To prevent automatic dismissal of the dialog, we have to override
            // the listener instead of passing it in to setPositiveButton.
            dialog.getButton(BUTTON_POSITIVE).setOnClickListener(v -> onSubmit());

            if (activity instanceof BaseActivity) {
                ((BaseActivity) activity).onDialogOpened(BaseDialogFragment.this);
            }
            onOpen(getArguments());
        });
        return dialog;
    }

    @Override public void onDismiss(DialogInterface di) {
        Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            ((BaseActivity) activity).onDialogClosed(this);
        }
        onClose();
    }

    /** Invoked just after the dialog opens; use this to populate the dialog. */
    protected void onOpen(Bundle args) { }

    /** Invoked when the user taps the "OK" button; should call .dismiss() if needed. */
    protected void onSubmit() { }

    /** Invoked when the dialog is closed, either by the "Cancel" button or by .dismiss(). */
    protected void onClose() { }

    /** Attaches a validation error message to a text field. */
    protected void setError(TextView field, int messageId, Object... args) {
        field.setError(getString(messageId, args));
        field.invalidate();
        field.requestFocus();
    }

    protected void showKeyboard() {
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }
}
