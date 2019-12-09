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
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ListView;

import org.projectbuendia.client.App;
import org.projectbuendia.client.R;
import org.projectbuendia.models.AppModel;
import org.projectbuendia.models.Location;
import org.projectbuendia.models.LocationForest;
import org.projectbuendia.models.Patient;
import org.projectbuendia.models.TypedCursor;
import org.projectbuendia.client.sync.SyncManager;
import org.projectbuendia.client.ui.BigToast;
import org.projectbuendia.client.ui.PatientListAdapter;
import org.projectbuendia.client.ui.ProgressFragment;
import org.projectbuendia.client.ui.ReadyState;
import org.projectbuendia.client.utils.EventBusWrapper;
import org.projectbuendia.client.utils.Logger;
import org.projectbuendia.client.utils.Utils;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

/** A fragment showing a filterable list of patients. */
public class PatientListFragment extends ProgressFragment implements
    ExpandableListView.OnChildClickListener {
    private static final Logger LOG = Logger.create();

    /**
     * The serialization (saved instance state) Bundle key representing the
     * activated item position. Only used on tablets.
     */
    private static final String STATE_ACTIVATED_POSITION = "activated_position";
    private PatientSearchController mController;
    private PatientListController mListController;
    private PatientListAdapter mPatientAdapter;
    private FragmentUi mFragmentUi;
    private ListUi mListUi;

    @Inject AppModel mModel;
    @Inject SyncManager mSyncManager;

    /** The current activated item position. Only used on tablets. */
    private int mActivatedPosition = ListView.INVALID_POSITION;

    private ExpandableListView mListView;

    private SwipeRefreshLayout mSwipeToRefresh;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public PatientListFragment() {
        mFragmentUi = new FragmentUi();
        mListUi = new ListUi();
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.inject(this);
        mListController = new PatientListController(
            mListUi, mSyncManager, new EventBusWrapper(EventBus.getDefault()));
        setContentLayout(R.layout.fragment_patient_list);
    }

    @Override public void onResume() {
        super.onResume();
        mListController.init();
    }

    @Override public void onPause() {
        mListController.suspend();
        super.onPause();
    }

    @Override public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        mController = ((PatientListActivity) getActivity()).getSearchController();
        mController.attachFragmentUi(mFragmentUi);
    }

    @Override public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mListView = view.findViewById(R.id.fragment_patient_list);
        mListView.setEmptyView(view.findViewById(R.id.empty));
        mListView.setOnChildClickListener(this);
        mPatientAdapter = new PatientListAdapter(getActivity());
        mListView.setAdapter(mPatientAdapter);

        mSwipeToRefresh =
            view.findViewById(R.id.fragment_patient_list_swipe_to_refresh);
        mSwipeToRefresh.setOnRefreshListener(() -> {
            Utils.logUserAction("refresh_requested");
            mListController.onRefreshRequested();
        });

        // Restore the previously serialized activated item position.
        if (savedInstanceState != null
            && savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
            setActivatedPosition(savedInstanceState.getInt(STATE_ACTIVATED_POSITION));
        }
    }

    private void setActivatedPosition(int position) {
        if (position == ListView.INVALID_POSITION) {
            mListView.setItemChecked(mActivatedPosition, false);
        } else {
            mListView.setItemChecked(position, true);
        }

        mActivatedPosition = position;
    }

    @Override public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mActivatedPosition != ListView.INVALID_POSITION) {
            // Serialize and persist the activated item position.
            outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
        }
    }

    @Override public boolean onChildClick(
        ExpandableListView parent, View v, int groupIndex, int childIndex, long id) {
        Patient patient = (Patient) mPatientAdapter.getChild(groupIndex, childIndex);
        Utils.logUserAction("patient_pressed", "patient_uuid", patient.uuid);
        mController.onPatientSelected(patient);
        return true;
    }

    @Override public void onDestroyView() {
        mController.detachFragmentUi(mFragmentUi);
        super.onDestroyView();
    }

    private class FragmentUi implements PatientSearchController.FragmentUi {
        @Override public void setPatients(
            TypedCursor<Patient> patients, LocationForest forest, String rootLocationUuid) {
            if (mPatientAdapter != null) {
                Location root = forest.get(rootLocationUuid);
                mPatientAdapter.setPatients(patients, forest, !forest.isLeaf(root));
            }
        }

        @Override public void showSpinner(boolean show) {
            LOG.w("showSpinner %s", show);
            setReadyState(show ? ReadyState.LOADING : ReadyState.READY);
        }
    }

    private class ListUi implements PatientListController.Ui {

        @Override public void stopRefreshAnimation() {
            mSwipeToRefresh.setRefreshing(false);
        }

        @Override public void showRefreshError() {
            BigToast.show(R.string.patient_list_fragment_sync_error);
        }

        @Override public void showApiHealthProblem() {
            BigToast.show(R.string.api_health_problem);
        }
    }
}
