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

import java.util.List;

import org.projectbuendia.client.R;
import org.projectbuendia.client.data.app.AppLocation;
import org.projectbuendia.client.data.app.AppLocationTree;
import org.projectbuendia.client.ui.ProgressFragment;
import org.projectbuendia.client.utils.PatientCountDisplay;
import org.projectbuendia.client.widget.SubtitledButtonView;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import com.google.common.base.Optional;

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

    private LocationListController mController;
    private final Ui mUi = new Ui();
    private LocationListAdapter mAdapter;

    public LocationListFragment() {
        // Required empty public constructor
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

    @OnClick(R.id.location_selection_triage)
    void onTriageClicked(View v) {
        mController.onTriagePressed();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_location_selection);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        ButterKnife.inject(this, view);
        return view;
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        mController = ((LocationListActivity) getActivity()).getController();
        mController.attachFragmentUi(mUi);
    }

    @Override
    public void onDestroyView() {
        mController.detachFragmentUi(mUi);
        super.onDestroyView();
    }

    public void setPatientCount(SubtitledButtonView button, int count) {
        button.setSubtitle("" + count);
        button.setTextColor(0xff000000);
        button.setSubtitleColor(count == 0 ? 0x40000000 : 0xff000000);
    }

    private final class Ui implements LocationListController.LocationFragmentUi {
        @Override
        public void setDischargedPatientCount(int patientCount) {
            setPatientCount(mDischargedButton, patientCount);
        }

        @Override
        public void setTriagePatientCount(int patientCount) {
            setPatientCount(mTriageButton, patientCount);
        }

        @Override
        public void setPresentPatientCount(int patientCount) {
            setPatientCount(mAllPatientsButton, patientCount);
        }

        @Override
        public void setLocations(AppLocationTree locationTree, List<AppLocation> locations) {
            mAdapter = new LocationListAdapter(
                    getActivity(), locations, locationTree, Optional.<String>absent());
            mLocationGrid.setAdapter(mAdapter);
        }

        @Override
        public void setBusyLoading(boolean busy) {
            changeState(busy ? State.LOADING : State.LOADED);
        }

        @Override
        public void showIncrementalSyncProgress(int progress, @Nullable String label) {
            incrementProgressBy(progress);
            if (label != null) {
                setProgressLabel(label);
            }
        }

        @Override
        public void resetSyncProgress() {
            switchToCircularProgressBar();
        }

        @Override
        public void showSyncCancelRequested() {
            setProgressLabel(getString(R.string.cancelling_sync));
        }
    }
}
