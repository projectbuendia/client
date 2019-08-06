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
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.WindowManager;

import com.google.android.flexbox.FlexboxLayout;
import com.google.common.collect.ImmutableList;

import org.projectbuendia.client.App;
import org.projectbuendia.client.R;
import org.projectbuendia.client.events.CrudEventBus;
import org.projectbuendia.client.models.AppModel;
import org.projectbuendia.client.models.CursorLoader;
import org.projectbuendia.client.models.Location;
import org.projectbuendia.client.models.Patient;
import org.projectbuendia.client.models.TypedCursorWithLoader;
import org.projectbuendia.client.models.Zones;
import org.projectbuendia.client.providers.Contracts;
import org.projectbuendia.client.ui.lists.LocationOption;
import org.projectbuendia.client.ui.lists.LocationOptionList;
import org.projectbuendia.client.utils.ContextUtils;
import org.projectbuendia.client.utils.LocaleSelector;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import static org.projectbuendia.client.utils.Utils.eq;

/** A {@link DialogFragment} for updating a patient's location and bed number. */
public class PatientLocationDialogFragment extends DialogFragment {
    @Inject AppModel mModel;
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
        mList.setOptions(getLocationOptions());
        mList.setSelectedUuid(getArguments().getString("locationUuid"));
        return dialog;
    }

    private List<LocationOption> getLocationOptions() {
        List<Location> locations = getLocations();

        int numPresentPatients = 0;
        int numDischarged = 0;
        for (Location location : locations) {
            if (eq(location.uuid, Zones.DISCHARGED_ZONE_UUID)) {
                numDischarged += location.patientCount;
            } else {
                numPresentPatients += location.patientCount;
            }
        }

        int fg = c.color(R.color.vital_fg_light);
        int bg = c.color(R.color.zone_confirmed);

        List<LocationOption> options = new ArrayList<>();
        options.add(new LocationOption(
            null, getString(R.string.all_present_patients), numPresentPatients, fg, bg, 1));
        for (Location location : locations) {
            if (!eq(location.uuid, Zones.DISCHARGED_ZONE_UUID)) {
                options.add(new LocationOption(
                    location.uuid, location.name, location.patientCount, fg, bg, 0.5));
            }
        }
        options.add(new LocationOption(
            Zones.DISCHARGED_ZONE_UUID, getString(R.string.discharged), numDischarged, fg, bg, 1));
        return options;
    }

    private List<Location> getLocations() {
        CursorLoader<Location> loader = new Location.Loader();
        String locale = LocaleSelector.getCurrentLocaleTag();
        try (Cursor cursor = getActivity().getContentResolver().query(
            Contracts.getLocalizedLocationsUri(locale), null, null, null, null)) {
            return ImmutableList.copyOf(new TypedCursorWithLoader<>(cursor, loader));
        }
    }

    public void onSubmit() {
    }
}
