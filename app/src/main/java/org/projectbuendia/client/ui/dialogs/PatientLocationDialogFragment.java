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
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.WindowManager;

import com.google.android.flexbox.FlexboxLayout;

import org.projectbuendia.client.App;
import org.projectbuendia.client.AppSettings;
import org.projectbuendia.client.R;
import org.projectbuendia.client.events.CrudEventBus;
import org.projectbuendia.client.models.AppModel;
import org.projectbuendia.client.models.NewLocation;
import org.projectbuendia.client.models.NewLocationTree;
import org.projectbuendia.client.models.Patient;
import org.projectbuendia.client.models.Zones;
import org.projectbuendia.client.ui.lists.LocationOption;
import org.projectbuendia.client.ui.lists.LocationOptionList;
import org.projectbuendia.client.utils.ContextUtils;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import static org.projectbuendia.client.utils.Utils.eq;

/** A {@link DialogFragment} for updating a patient's location and bed number. */
public class PatientLocationDialogFragment extends DialogFragment {
    @Inject AppModel mModel;
    @Inject AppSettings mSettings;
    @Inject CrudEventBus mCrudEventBus;
    private ContextUtils c;

    private FlexboxLayout mContainer;
    private LocationOptionList mList;

    /** Creates a new instance and registers the given UI, if specified. */
    public static PatientLocationDialogFragment newInstance(Patient patient) {
        PatientLocationDialogFragment fragment = new PatientLocationDialogFragment();
        Bundle args = new Bundle();
        args.putString("uuid", patient.uuid);
        args.putString("locationUuid", patient.locationUuid);
        fragment.setArguments(args);
        return fragment;
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.getInstance().inject(this);
        c = ContextUtils.from(getActivity());
    }

    @Override public @NonNull Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog dialog = c.buildDialog(R.layout.patient_location_dialog_fragment)
            .setCancelable(false) // Disable auto-cancel.
            .setTitle(R.string.action_assign_location)
            .setPositiveButton(getResources().getString(R.string.ok), (dialogInterface, i) -> onSubmit())
            .setNegativeButton(getResources().getString(R.string.cancel), null)
            .create();
        dialog.getWindow().setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        mContainer = c.findView(R.id.list_container);
        mList = new LocationOptionList(mContainer);
        mModel.getLocationTree(mSettings.getLocaleTag(), tree -> {
            mList.setOptions(getLocationOptions(tree));
            mList.setSelectedUuid(getArguments().getString("locationUuid"));
        });
        return dialog;
    }

    private List<LocationOption> getLocationOptions(NewLocationTree tree) {
        NewLocation discharged = tree.get(Zones.DISCHARGED_ZONE_UUID);
        int numDischarged = discharged != null ? tree.countPatientsIn(discharged) : 0;
        int numPatients = tree.countAllPatients();

        int fg = c.color(R.color.vital_fg_light);
        int bg = c.color(R.color.zone_confirmed);

        List<LocationOption> options = new ArrayList<>();
        options.add(new LocationOption(
            null, c.str(R.string.all_present_patients), numPatients - numDischarged, fg, bg, 1));
        for (NewLocation location : tree.getDescendants(null)) {
            if (!eq(location, discharged)) {
                // A parenthesized number can be included at the front of the location name
                // to determine its sorting order; the number will not be displayed.
                String displayName = location.name.replaceAll("^\\(.*?\\)\\s*", "");
                options.add(new LocationOption(
                    location.uuid, displayName, tree.countPatientsIn(location), fg, bg, 0.5));
            }
        }
        if (discharged != null) {
            options.add(new LocationOption(
                Zones.DISCHARGED_ZONE_UUID, c.str(R.string.discharged), numDischarged, fg, bg, 1));
        }
        return options;
    }

    public void onSubmit() {
    }
}
