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
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.EditText;

import org.projectbuendia.client.App;
import org.projectbuendia.client.R;
import org.projectbuendia.client.json.JsonNewUser;
import org.projectbuendia.client.utils.Utils;

import java.io.Serializable;

/** A DialogFragment for adding a new user. */
public class NewUserDialogFragment extends BaseDialogFragment<NewUserDialogFragment, Serializable> {
    class Views {
        EditText givenName = u.findView(R.id.given_name_field);
        EditText familyName = u.findView(R.id.family_name_field);
    }
    @Nullable private ActivityUi mActivityUi;  // optional UI for showing a spinner
    private Views v;

    /** Creates a new instance and registers the given UI, if specified. */
    public static NewUserDialogFragment create(ActivityUi activityUi) {
        return new NewUserDialogFragment().setUi(activityUi);
    }

    public NewUserDialogFragment setUi(ActivityUi activityUi) {
        mActivityUi = activityUi;
        return this;
    }

    @Override public AlertDialog onCreateDialog(Bundle state) {
        return createAlertDialog(R.layout.new_user_dialog_fragment);
    }

    @Override protected void onOpen() {
        v = new Views();

        dialog.setTitle(R.string.title_new_user);
        v.givenName.requestFocus();
        Utils.showKeyboard(dialog.getWindow());
    }

    @Override protected void onSubmit() {
        String givenName = Utils.toNonnullString(v.givenName.getText()).trim();
        String familyName = Utils.toNonnullString(v.familyName.getText()).trim();
        boolean valid = true;
        if (givenName.isEmpty()) {
            setError(v.givenName, R.string.given_name_cannot_be_null);
            valid = false;
        }
        if (familyName.isEmpty()) {
            setError(v.familyName, R.string.family_name_cannot_be_null);
            valid = false;
        }
        Utils.logUserAction("add_user_submitted",
            "valid", "" + valid,
            "given_name", givenName,
            "family_name", familyName);
        if (!valid) return;

        App.getUserManager().addUser(new JsonNewUser(givenName, familyName));
        if (mActivityUi != null) {
            mActivityUi.showSpinner(true);
        }
        dialog.dismiss();
    }

    /** An interface to show a spinner while the new user is being saved. */
    public interface ActivityUi {
        void showSpinner(boolean show);
    }
}
