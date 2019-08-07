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

package org.projectbuendia.client.ui.lists;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import com.google.common.base.Optional;

import org.projectbuendia.client.R;
import org.projectbuendia.client.models.Location;
import org.projectbuendia.client.models.LocationForest;
import org.projectbuendia.client.ui.ReadyState;
import org.projectbuendia.client.ui.ProgressFragment;
import org.projectbuendia.client.utils.Logger;
import org.projectbuendia.client.widgets.SubtitledButtonView;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.OnItemClick;

/** Displays a list of all locations. */
public final class LocationListFragment extends ProgressFragment {
    @InjectView(R.id.location_selection_locations) GridView mLocationGrid;
    @InjectView(R.id.location_selection_all_patients) SubtitledButtonView mAllPatientsButton;
    @InjectView(R.id.location_selection_triage) SubtitledButtonView mTriageButton;
    @InjectView(R.id.location_selection_discharged) SubtitledButtonView mDischargedButton;

    private static final Logger LOG = Logger.create();
    private LocationListController mController;
    private final Ui mUi = new Ui();
    private LocationListAdapter mAdapter;

    public LocationListFragment() {
        // Required empty public constructor
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_location_selection);
    }

    @Override public View onCreateView(
        LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        ButterKnife.inject(this, view);
        return view;
    }

    @Override public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        mController = ((LocationListActivity) getActivity()).getController();
        if (mController == null) {
            LOG.w("No controller for " + getActivity().getClass().getSimpleName());
        } else {
            mController.attachFragmentUi(mUi);
        }
    }

    @Override public void onDestroyView() {
        if (mController == null) {
            LOG.w("No controller for " + getActivity().getClass().getSimpleName());
        } else {
            mController.detachFragmentUi(mUi);
        }
        super.onDestroyView();
    }

    public void setPatientCount(SubtitledButtonView button, long count) {
        button.setSubtitle("" + count);
        button.setTextColor(0xff000000);
        button.setSubtitleColor(count == 0 ? 0x40000000 : 0xff000000);
    }

    @OnItemClick(R.id.location_selection_locations)
    void onLocationGridClicked(int position) {
        mController.onLocationSelected(mAdapter.getItem(position));
    }

    @OnClick(R.id.location_selection_all_patients)
    void onAllPatientsClicked(View v) {
        FilteredPatientListActivity.start(getActivity());
    }

    @OnClick(R.id.location_selection_discharged)
    void onDischargedClicked(View v) {
        mController.onDischargedPressed();
    }

    @OnClick(R.id.location_selection_triage) void onTriageClicked(View v) {
        mController.onTriagePressed();
    }

    private final class Ui implements LocationListController.LocationFragmentUi {
        @Override public void setDischargedPatientCount(long patientCount) {
            setPatientCount(mDischargedButton, patientCount);
        }

        @Override public void setTriagePatientCount(long patientCount) {
            setPatientCount(mTriageButton, patientCount);
        }

        @Override public void setPresentPatientCount(long patientCount) {
            setPatientCount(mAllPatientsButton, patientCount);
        }

        @Override public void setLocations(LocationForest forest, List<Location> locations) {
            mAdapter = new LocationListAdapter(
                getActivity(), locations, forest, Optional.absent());
            mLocationGrid.setAdapter(mAdapter);
        }

        @Override public void setReadyState(ReadyState state) {
            LocationListFragment.this.setReadyState(state);
        }

        @Override public void setSyncProgress(int numerator, int denominator, Integer messageId) {
            setProgress(numerator, denominator);
            if (messageId != null) {
                setProgressMessage(messageId);
            }
        }

        @Override public void showSyncCancelRequested() {
            setProgressMessage(R.string.cancelling_sync);
        }
    }
}
