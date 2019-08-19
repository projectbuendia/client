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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.EditText;

import org.projectbuendia.client.App;
import org.projectbuendia.client.R;
import org.projectbuendia.client.json.JsonNewUser;
import org.projectbuendia.client.utils.Utils;

import butterknife.ButterKnife;
import butterknife.InjectView;

/** A {@link android.support.v4.app.DialogFragment} for adding a new user. */
public class NewUserDialogFragment extends DialogFragment {
    @InjectView(R.id.add_user_given_name_tv) EditText mGivenName;
    @InjectView(R.id.add_user_family_name_tv) EditText mFamilyName;
    private LayoutInflater mInflater;
    @Nullable private ActivityUi mActivityUi;  // optional UI for showing a spinner

    /** An interface to show a spinner while the new user is being saved. */
    public interface ActivityUi {
        void showSpinner(boolean show);
    }

    /** Creates a new instance and registers the given UI, if specified. */
    public static NewUserDialogFragment newInstance(ActivityUi activityUi) {
        NewUserDialogFragment fragment = new NewUserDialogFragment();
        fragment.setUi(activityUi);
        return fragment;
    }

    public void setUi(ActivityUi activityUi) {
        mActivityUi = activityUi;
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mInflater = LayoutInflater.from(getActivity());
    }

    @Override public @NonNull Dialog onCreateDialog(Bundle savedInstanceState) {
        View fragment = mInflater.inflate(R.layout.new_user_dialog_fragment, null);
        ButterKnife.inject(this, fragment);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity())
            .setCancelable(false) // Disable auto-cancel.
            .setTitle(getString(R.string.title_new_user))
            .setPositiveButton(getString(R.string.ok), null)
            .setNegativeButton(getString(R.string.cancel), null)
            .setView(fragment);

        final AlertDialog dialog = dialogBuilder.create();
        dialog.setOnShowListener(di ->
            dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                .setOnClickListener(view -> onSubmit(dialog))
        );

        // Open the keyboard, ready to type into the given name field.
        dialog.getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        return dialog;
    }

    private void onSubmit(DialogInterface dialog) {
        // Validate the user.
        String givenName = Utils.toNonnullString(mGivenName.getText()).trim();
        String familyName = Utils.toNonnullString(mFamilyName.getText()).trim();
        boolean valid = true;
        if (givenName.isEmpty()) {
            setError(mGivenName, R.string.given_name_cannot_be_null);
            valid = false;
        }
        if (familyName.isEmpty()) {
            setError(mFamilyName, R.string.family_name_cannot_be_null);
            valid = false;
        }
        Utils.logUserAction("add_user_submitted",
            "valid", "" + valid,
            "given_name", givenName,
            "family_name", familyName);
        if (!valid) return;

        App.getUserManager().addUser(new JsonNewUser(
            givenName, familyName
        ));
        if (mActivityUi != null) {
            mActivityUi.showSpinner(true);
        }
        dialog.dismiss();
    }

    private void setError(EditText field, int resourceId) {
        field.setError(getString(resourceId));
        field.invalidate();
        field.requestFocus();
    }
}
