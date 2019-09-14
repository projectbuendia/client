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
import org.projectbuendia.client.models.Obs;
import org.projectbuendia.client.utils.Utils;

import butterknife.InjectView;

import static org.projectbuendia.client.utils.Utils.eq;

/** A DialogFragment for editing a text observation. */
public class TextObsDialogFragment extends BaseDialogFragment<TextObsDialogFragment> {
    @InjectView(R.id.text) EditText text;

    private Obs obs;

    public static TextObsDialogFragment create(String title, Obs obs) {
        return new TextObsDialogFragment().withArgs(Utils.bundle("title", title, "obs", obs));
    }

    @Override public AlertDialog onCreateDialog(Bundle state) {
        return createAlertDialog(R.layout.text_obs_dialog_fragment);
    }

    @Override protected void onOpen(Bundle args) {
        dialog.setTitle((String) args.get("title"));
        obs = (Obs) args.get("obs");
        text.setText(obs.value);
        showKeyboard();
    }

    @Override protected void onSubmit() {
        String newValue = text.getText().toString();
        if (eq(newValue, obs.value)) return;

        Utils.logUserAction("text_obs_submitted",
            "patient_uuid", obs.patientUuid,
            "concept_uuid", obs.conceptUuid,
            "text", newValue);

        App.getModel().addObservationEncounter(
            App.getCrudEventBus(), obs.patientUuid, new Obs(
                null, null, obs.patientUuid, Utils.getProviderUuid(),
                obs.conceptUuid, Datatype.TEXT, DateTime.now(), null, newValue, null
            )
        );
        dialog.dismiss();
    }
}
