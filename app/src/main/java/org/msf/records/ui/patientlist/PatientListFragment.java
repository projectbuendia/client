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
import org.msf.records.events.data.SingleItemCreatedEvent;
import org.msf.records.events.sync.SyncFinishedEvent;
import org.msf.records.location.LocationManager;
import org.msf.records.net.Constants;
import org.msf.records.sync.GenericAccountService;
import org.msf.records.sync.SyncManager;
import org.msf.records.ui.PatientListTypedCursorAdapter;
import org.msf.records.ui.ProgressFragment;
import org.msf.records.utils.Logger;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

/**
 * A list fragment representing a filterable list of Patients.
 */
public class PatientListFragment extends ProgressFragment implements
        ExpandableListView.OnChildClickListener,
        SwipeRefreshLayout.OnRefreshListener {

    private static final Logger LOG = Logger.create();

    private PatientSearchController mController;
    private PatientListTypedCursorAdapter mPatientAdapter;
    private FragmentUi mFragmentUi;

    /**
     * The serialization (saved instance state) Bundle key representing the
     * activated item position. Only used on tablets.
     */
    private static final String STATE_ACTIVATED_POSITION = "activated_position";

    @Inject LocationManager mLocationManager;
    @Inject SyncManager mSyncManager;

    /**
     * The current activated item position. Only used on tablets.
     */
    private int mActivatedPosition = ListView.INVALID_POSITION;

    private ExpandableListView mListView;

    private SwipeRefreshLayout mSwipeToRefresh;

    private boolean isRefreshing;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public PatientListFragment() {
        mFragmentUi = new FragmentUi();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.getInstance().inject(this);
        setContentView(R.layout.fragment_patient_list);
    }

    @Override
    public void onResume() {
        super.onResume();

        // TODO(akalachman): Deal with this.
        // changeState(State.LOADING);
        EventBus.getDefault().register(this);
        mLocationManager.loadLocations();
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);

        super.onPause();
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        mController = ((PatientSearchActivity) getActivity()).getSearchController();
        mController.attachFragmentUi(mFragmentUi);
    }

    @Override
    public void onRefresh() {
        if(!isRefreshing){
            LOG.d("onRefresh");

            //triggers app wide data refresh
            mSyncManager.forceSync();
            isRefreshing = true;
        }
    }

    public synchronized void onEvent(SyncFinishedEvent event) {
        stopRefreshing();
    }

    private void stopRefreshing(){
        if (isRefreshing) {
            mSwipeToRefresh.setRefreshing(false);
            isRefreshing = false;
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mPatientAdapter = getAdapterInstance();

        mListView = (ExpandableListView) view.findViewById(R.id.fragment_patient_list);
        mListView.setOnChildClickListener(this);
        mListView.setAdapter(mPatientAdapter);

        mSwipeToRefresh = (SwipeRefreshLayout) view.findViewById(R.id.fragment_patient_list_swipe_to_refresh);
        mSwipeToRefresh.setOnRefreshListener(this);

        // Restore the previously serialized activated item position.
        if (savedInstanceState != null
                && savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
            setActivatedPosition(savedInstanceState.getInt(STATE_ACTIVATED_POSITION));
        }
    }

    public PatientListTypedCursorAdapter getAdapterInstance() {
        return new PatientListTypedCursorAdapter(getActivity());
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

       if(Constants.OFFLINE_SUPPORT){
            // Create account, if needed
            GenericAccountService.registerSyncAccount(activity);
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
    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
        AppPatient patient = (AppPatient)mPatientAdapter.getChild(groupPosition, childPosition);
        mController.onPatientSelected(patient);

        return true;
    }

    // TODO(akalachman): Move to controller.
    public void onEvent(SingleItemCreatedEvent<AppPatient> event) {
        onRefresh();
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
        public void notifyDataSetChanged() {
            mPatientAdapter.notifyDataSetChanged();
        }

        @Override
        public void setLocations(AppLocationTree locationTree) {
            // TODO(akalachman): Implement.
        }

        @Override
        public void setPatients(TypedCursor<AppPatient> patients) {
            mPatientAdapter.setPatients(patients);
        }

        @Override
        public void showSpinner(boolean show) {
            changeState(show ? State.LOADING : State.LOADED);
        }

        @Override
        public void showRefreshIndicator(boolean show) {
            // TODO(akalachman): Implement.
        }
    }
}
