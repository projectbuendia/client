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

package org.msf.records.ui.patientlist;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ListView;

import org.msf.records.App;
import org.msf.records.R;
import org.msf.records.data.app.AppLocationTree;
import org.msf.records.data.app.AppPatient;
import org.msf.records.data.app.TypedCursor;
import org.msf.records.net.Common;
import org.msf.records.sync.SyncAccountService;
import org.msf.records.sync.SyncManager;
import org.msf.records.ui.BigToast;
import org.msf.records.ui.PatientListTypedCursorAdapter;
import org.msf.records.ui.ProgressFragment;
import org.msf.records.utils.EventBusWrapper;
import org.msf.records.utils.Utils;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

/** A fragment showing a filterable list of patients. */
public class PatientListFragment extends ProgressFragment implements
        ExpandableListView.OnChildClickListener {

    private PatientSearchController mController;
    private PatientListController mListController;
    private PatientListTypedCursorAdapter mPatientAdapter;
    private FragmentUi mFragmentUi;
    private ListUi mListUi;

    /**
     * The serialization (saved instance state) Bundle key representing the
     * activated item position. Only used on tablets.
     */
    private static final String STATE_ACTIVATED_POSITION = "activated_position";

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.getInstance().inject(this);
        mListController = new PatientListController(
                mListUi, mSyncManager, new EventBusWrapper(EventBus.getDefault()));
        setContentView(R.layout.fragment_patient_list);
    }

    @Override
    public void onResume() {
        super.onResume();
        mListController.init();
    }

    @Override
    public void onPause() {
        mListController.suspend();
        super.onPause();
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        mController = ((PatientSearchActivity) getActivity()).getSearchController();
        mController.attachFragmentUi(mFragmentUi);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mListView = (ExpandableListView) view.findViewById(R.id.fragment_patient_list);
        mListView.setOnChildClickListener(this);
        // The list view adapter will be set once locations are available.

        mSwipeToRefresh =
                (SwipeRefreshLayout) view.findViewById(R.id.fragment_patient_list_swipe_to_refresh);
        mSwipeToRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Utils.logUserAction("refresh_requested");
                mListController.onRefreshRequested();
            }
        });

        // Restore the previously serialized activated item position.
        if (savedInstanceState != null
                && savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
            setActivatedPosition(savedInstanceState.getInt(STATE_ACTIVATED_POSITION));
        }
    }

    public PatientListTypedCursorAdapter getAdapterInstance(AppLocationTree locationTree) {
        return new PatientListTypedCursorAdapter(getActivity(), locationTree);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (Common.OFFLINE_SUPPORT) {
            // Create account, if needed
            SyncAccountService.initialize(activity);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mActivatedPosition != ListView.INVALID_POSITION) {
            // Serialize and persist the activated item position.
            outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
        }
    }

    @Override
    public boolean onChildClick(
            ExpandableListView parent,
            View v,
            int groupPosition,
            int childPosition,
            long id) {
        AppPatient patient = (AppPatient)mPatientAdapter.getChild(groupPosition, childPosition);
        Utils.logUserAction("patient_pressed", "patient_uuid", patient.uuid);
        mController.onPatientSelected(patient);
        return true;
    }

    private void setActivatedPosition(int position) {
        if (position == ListView.INVALID_POSITION) {
            mListView.setItemChecked(mActivatedPosition, false);
        } else {
            mListView.setItemChecked(position, true);
        }

        mActivatedPosition = position;
    }

    @Override
    public void onDestroyView() {
        mController.detachFragmentUi(mFragmentUi);
        super.onDestroyView();
    }

    private class FragmentUi implements PatientSearchController.FragmentUi {
        @Override
        public void setLocationTree(AppLocationTree locationTree) {
            mPatientAdapter = getAdapterInstance(locationTree);
            mListView.setAdapter(mPatientAdapter);
        }

        @Override
        public void setPatients(TypedCursor<AppPatient> patients) {
            if (mPatientAdapter != null) {
                mPatientAdapter.setPatients(patients);
            }
        }

        @Override
        public void showSpinner(boolean show) {
            changeState(show ? State.LOADING : State.LOADED);
        }
    }

    private class ListUi implements PatientListController.Ui {

        @Override
        public void stopRefreshAnimation() {
            mSwipeToRefresh.setRefreshing(false);
        }

        @Override
        public void showRefreshError() {
            BigToast.show(getActivity(), R.string.patient_list_fragment_sync_error);
        }

        @Override
        public void showApiHealthProblem() {
            BigToast.show(getActivity(), R.string.api_health_problem);
        }
    }
}
