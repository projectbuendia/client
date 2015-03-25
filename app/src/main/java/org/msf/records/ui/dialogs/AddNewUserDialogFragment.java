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

package org.msf.records.ui.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import org.msf.records.App;
import org.msf.records.R;
import org.msf.records.net.model.NewUser;
import org.msf.records.utils.Utils;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * A {@link android.support.v4.app.DialogFragment} for adding a new user.
 */
public class AddNewUserDialogFragment extends DialogFragment {
    /**
     * Creates a new instance and registers the given UI, if specified.
     */
    public static AddNewUserDialogFragment newInstance(ActivityUi activityUi) {
        AddNewUserDialogFragment fragment = new AddNewUserDialogFragment();
        fragment.setUi(activityUi);
        return fragment;
    }

    @InjectView(R.id.add_user_given_name_tv) EditText mGivenName;
    @InjectView(R.id.add_user_family_name_tv) EditText mFamilyName;

    private LayoutInflater mInflater;
    // Optional UI for exposing a spinner.
    @Nullable private ActivityUi mActivityUi;

    /**
     * Delegate for the UI that will be shown when the dialog is closed, so that a spinner can be
     * shown until the user has loaded.
     */
    public interface ActivityUi {

        void showSpinner(boolean show);
    }

    public void setUi(ActivityUi activityUi) {
        mActivityUi = activityUi;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mInflater = LayoutInflater.from(getActivity());
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View fragment = mInflater.inflate(R.layout.dialog_fragment_add_new_user, null);
        ButterKnife.inject(this, fragment);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity())
                .setCancelable(false) // Disable auto-cancel.
                .setTitle(getResources().getString(R.string.title_add_new_user))
                .setPositiveButton(getResources().getString(R.string.ok), null)
                .setNegativeButton(getResources().getString(R.string.cancel), null)
                .setView(fragment);

        final AlertDialog dialog = dialogBuilder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
            public void onShow(DialogInterface dialogInterface) {
                dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                        .setOnClickListener(
                                new View.OnClickListener() {

                                    @Override
                                    public void onClick(View view) {
                                        // Validate the user.
                                        String givenName = mGivenName.getText() == null ? ""
                                                : mGivenName.getText().toString().trim();
                                        String familyName = mFamilyName.getText() == null ? ""
                                                : mFamilyName.getText().toString().trim();
                                        boolean valid = true;
                                        if (givenName.isEmpty()) {
                                            setError(mGivenName,
                                                    R.string.given_name_cannot_be_null);
                                            valid = false;
                                        }
                                        if (familyName.isEmpty()) {
                                            setError(mFamilyName,
                                                    R.string.family_name_cannot_be_null);
                                            valid = false;
                                        }
                                        Utils.logUserAction("add_user_submitted",
                                                "valid", "" + valid,
                                                "given_name", givenName,
                                                "family_name", familyName);
                                        if (!valid) {
                                            return;
                                        }

                                        App.getUserManager().addUser(new NewUser(
                                                givenName, familyName
                                        ));
                                        if (mActivityUi != null) {
                                            mActivityUi.showSpinner(true);
                                        }
                                        dialog.dismiss();
                                    }
                                });
            }
        });

        return dialog;
    }

    private void setError(EditText field, int resourceId) {
        field.setError(getResources().getString(resourceId));
        field.invalidate();
        field.requestFocus();
    }
}
