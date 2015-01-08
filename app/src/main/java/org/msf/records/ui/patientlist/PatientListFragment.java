package org.msf.records.ui.patientlist;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.Toast;

import javax.inject.Inject;
import javax.inject.Provider;

import org.msf.records.App;
import org.msf.records.R;
import org.msf.records.data.app.AppModel;
import org.msf.records.data.app.AppPatient;
import org.msf.records.events.CrudEventBus;
import org.msf.records.events.data.FilterCompletedEvent;
import org.msf.records.events.data.SingleItemCreatedEvent;
import org.msf.records.events.location.LocationsLoadFailedEvent;
import org.msf.records.events.location.LocationsLoadedEvent;
import org.msf.records.events.sync.SyncFinishedEvent;
import org.msf.records.filter.FilterGroup;
import org.msf.records.filter.PatientFilters;
import org.msf.records.filter.LocationUuidFilter;
import org.msf.records.filter.SimpleSelectionFilter;
import org.msf.records.location.LocationManager;
import org.msf.records.location.LocationTree;
import org.msf.records.net.Constants;
import org.msf.records.sync.GenericAccountService;
import org.msf.records.sync.SyncManager;
import org.msf.records.ui.PatientListTypedCursorAdapter;
import org.msf.records.ui.ProgressFragment;

import de.greenrobot.event.EventBus;

/**
 * A list fragment representing a list of Patients.
 * <p>
 * Activities containing this fragment MUST implement the {@link Callbacks}
 * interface.
 */
public class PatientListFragment extends ProgressFragment implements
        ExpandableListView.OnChildClickListener,
        SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = PatientListFragment.class.getSimpleName();

    /**
     * The serialization (saved instance state) Bundle key representing the
     * activated item position. Only used on tablets.
     */
    private static final String STATE_ACTIVATED_POSITION = "activated_position";

    @Inject AppModel mAppModel;
    @Inject LocationManager mLocationManager;
    @Inject Provider<CrudEventBus> mCrudEventBusProvider;
    @Inject SyncManager mSyncManager;

    /**
     * The fragment's current callback object, which is notified of list item
     * clicks.
     */
    private Callbacks mCallbacks = sDummyCallbacks;

    /**
     * The current activated item position. Only used on tablets.
     */
    private int mActivatedPosition = ListView.INVALID_POSITION;

    private PatientListTypedCursorAdapter mPatientAdapter;

    private ExpandableListView mListView;

    private SwipeRefreshLayout mSwipeToRefresh;

    private boolean isRefreshing;

    String mFilterQueryTerm = "";

    SimpleSelectionFilter mFilter;
    private LocationTree mLocationTree;

    public void filterBy(SimpleSelectionFilter filter) {
        // Tack on a location filter to the filter to show only known locations.
        if (mLocationTree == null || mLocationTree.getRoot().getLocation() == null) {
            mFilter = filter;
        } else {
            // Tack on a location filter to the filter to show only known locations.
            mFilter = new FilterGroup(new LocationUuidFilter(mLocationTree.getRoot()), filter);
        }

        loadSearchResults();
    }

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callbacks {
        /**
         * Callback for when an item has been selected.
         */
        public void onItemSelected(String uuid, String givenName, String familyName, String id);
    }

    /**
     * A dummy implementation of the {@link Callbacks} interface that does
     * nothing. Used only when this fragment is not attached to an activity.
     */
    private static final Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onItemSelected(String uuid, String givenName, String familyName, String id) {
        }
    };

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public PatientListFragment() {

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

        changeState(State.LOADING);
        EventBus.getDefault().register(this);
        mLocationManager.loadLocations();
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);

        super.onPause();
    }

    @Override
    public void onRefresh() {
        if(!isRefreshing){
            Log.d(TAG, "onRefresh");

            //triggers app wide data refresh
            mSyncManager.forceSync();
            isRefreshing = true;
        }
    }

    public synchronized void onEvent(LocationsLoadedEvent event) {
    	mLocationTree = event.locationTree;
        mPatientAdapter.notifyDataSetChanged();
        changeState(State.LOADED);
    }

    public synchronized void onEvent(LocationsLoadFailedEvent event) {
        Toast.makeText(getActivity(), R.string.location_load_error, Toast.LENGTH_SHORT).show();
        mPatientAdapter.notifyDataSetChanged();
        changeState(State.LOADED);
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

    private void loadSearchResults(){
        // TODO(akalachman): Sub-filter on query term rather than re-filtering with each keypress.
        changeState(State.LOADING);
        mPatientAdapter.filter(mFilter, mFilterQueryTerm);
        mPatientAdapter.getEventBus().register(new FilterSubscriber());
    }

    private final class FilterSubscriber {
        public void onEventMainThread(FilterCompletedEvent event) {
            changeState(State.LOADED);
            mPatientAdapter.getEventBus().unregister(this);
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mListView = (ExpandableListView) view.findViewById(R.id.fragment_patient_list);
        mListView.setOnChildClickListener(this);

        mSwipeToRefresh = (SwipeRefreshLayout) view.findViewById(R.id.fragment_patient_list_swipe_to_refresh);
        mSwipeToRefresh.setOnRefreshListener(this);
        mFilter = PatientFilters.getDefaultFilter();
        mPatientAdapter = getAdapterInstance();
        mListView.setAdapter(mPatientAdapter);
        filterBy(mFilter);

        // Restore the previously serialized activated item position.
        if (savedInstanceState != null
                && savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
            setActivatedPosition(savedInstanceState.getInt(STATE_ACTIVATED_POSITION));
        }

        ((PatientSearchActivity)getActivity()).setOnSearchListener(new PatientSearchActivity.OnSearchListener() {
            @Override
            public void setQuerySubmitted(String q) {
                App.getServer().cancelPendingRequests(TAG);
                mFilterQueryTerm = q;
                loadSearchResults();
            }
        });
    }

    public PatientListTypedCursorAdapter getAdapterInstance() {
        return new PatientListTypedCursorAdapter(
                mAppModel, getActivity(), mCrudEventBusProvider.get());
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // Activities containing this fragment must implement its callbacks.
        if (!(activity instanceof Callbacks)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (Callbacks) activity;

        if(Constants.OFFLINE_SUPPORT){
            // Create account, if needed
            GenericAccountService.registerSyncAccount(activity);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // Reset the active callbacks interface to the dummy implementation.
        mCallbacks = sDummyCallbacks;
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
        mCallbacks.onItemSelected(patient.uuid, patient.givenName, patient.familyName, patient.id);

        return true;
    }

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
}
