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
import android.widget.EditText;

import org.joda.time.DateTime;
import org.projectbuendia.client.App;
import org.projectbuendia.client.R;
import org.projectbuendia.client.json.Datatype;
import org.projectbuendia.models.Obs;
import org.projectbuendia.client.utils.Utils;

import java.io.Serializable;

import static org.projectbuendia.client.utils.Utils.eq;

/** A DialogFragment for editing a text observation. */
public class TextObsDialogFragment extends BaseDialogFragment<TextObsDialogFragment, TextObsDialogFragment.Args> {
    static class Args implements Serializable {
        String title;
        Obs obs;
    }

    public static TextObsDialogFragment create(String title, Obs obs) {
        Args args = new Args();
        args.title = title;
        args.obs = obs;
        return new TextObsDialogFragment().withArgs(args);
    }

    @Override public AlertDialog onCreateDialog(Bundle state) {
        return createAlertDialog(R.layout.text_obs_dialog_fragment);
    }

    @Override protected void onOpen() {
        dialog.setTitle(args.title);
        u.setText(R.id.text, args.obs.value);
        Utils.showKeyboard(dialog.getWindow());
    }

    @Override protected void onSubmit() {
        String newValue = ((EditText) u.findView(R.id.text)).getText().toString();
        if (eq(newValue, args.obs.value)) return;

        Utils.logUserAction("text_obs_submitted",
            "patient_uuid", args.obs.patientUuid,
            "concept_uuid", args.obs.conceptUuid,
            "text", newValue);

        App.getModel().addObservationEncounter(
            App.getCrudEventBus(), args.obs.patientUuid, new Obs(
                null, null, args.obs.patientUuid, Utils.getProviderUuid(),
                args.obs.conceptUuid, Datatype.TEXT, DateTime.now(), null, newValue, null
            )
        );
        dialog.dismiss();
    }
}
