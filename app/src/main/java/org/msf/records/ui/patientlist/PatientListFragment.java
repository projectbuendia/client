package org.msf.records.ui.patientlist;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.Filter;
import android.widget.ListView;
import android.widget.Toast;

import javax.inject.Inject;

import org.msf.records.App;
import org.msf.records.R;
import org.msf.records.data.app.AppPatient;
import org.msf.records.events.data.SingleItemCreatedEvent;
import org.msf.records.events.location.LocationsLoadFailedEvent;
import org.msf.records.events.location.LocationsLoadedEvent;
import org.msf.records.events.sync.SyncFinishedEvent;
import org.msf.records.filter.FilterGroup;
import org.msf.records.filter.FilterManager;
import org.msf.records.filter.FilterQueryProviderFactory;
import org.msf.records.filter.LocationUuidFilter;
import org.msf.records.filter.SimpleSelectionFilter;
import org.msf.records.location.LocationManager;
import org.msf.records.location.LocationTree;
import org.msf.records.net.Constants;
import org.msf.records.sync.GenericAccountService;
import org.msf.records.sync.PatientProjection;
import org.msf.records.sync.PatientProviderContract;
import org.msf.records.sync.SyncManager;
import org.msf.records.ui.ExpandablePatientListAdapter;
import org.msf.records.ui.ProgressFragment;

import de.greenrobot.event.EventBus;

/**
 * A list fragment representing a list of Patients.
 * <p>
 * Activities containing this fragment MUST implement the {@link Callbacks}
 * interface.
 */
public class PatientListFragment extends ProgressFragment implements
        ExpandableListView.OnChildClickListener, LoaderManager.LoaderCallbacks<Cursor>,
        SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = PatientListFragment.class.getSimpleName();

    /**
     * The serialization (saved instance state) Bundle key representing the
     * activated item position. Only used on tablets.
     */
    private static final String STATE_ACTIVATED_POSITION = "activated_position";

    /**
     * The id to identify which Cursor is returned
     */
    private static final int LOADER_LIST_ID = 0;

    @Inject LocationManager mLocationManager;
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

    private ExpandablePatientListAdapter mPatientAdapter;

    private ExpandableListView mListView;

    private SwipeRefreshLayout mSwipeToRefresh;

    // TODO(akalachman): Figure out how to break reliance on this cursor--we already have the info.
    private FilterQueryProviderFactory mFactory;

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

        mPatientAdapter.setSelectionFilter(mFilter);
        mPatientAdapter.setFilterQueryProvider(mFactory.getFilterQueryProvider(filter));
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
    private static Callbacks sDummyCallbacks = new Callbacks() {
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
        mFactory = new FilterQueryProviderFactory(getActivity()).setUri(
        		PatientProviderContract.CONTENT_URI_TENT_PATIENT_COUNTS);
		LocationTree locationTree = LocationTree.SINGLETON_INSTANCE;
		if (locationTree != null) {
			mFactory.setSortClause(LocationTree.SINGLETON_INSTANCE.getLocationSortClause(
		                PatientProviderContract.PatientColumns.COLUMN_NAME_LOCATION_UUID));
		} else {
			Log.e(TAG, "Location tree does not exist yet");
		}
        setContentView(R.layout.fragment_patient_list);
    }

    @Override
    public void onResume() {
        super.onResume();

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
    }

    public synchronized void onEvent(LocationsLoadFailedEvent event) {
        Toast.makeText(getActivity(), R.string.location_load_error, Toast.LENGTH_SHORT).show();
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
        changeState(State.LOADING);
        mPatientAdapter.filter(mFilterQueryTerm, new Filter.FilterListener() {
            @Override
            public void onFilterComplete(int count) {
                mPatientAdapter.notifyDataSetChanged();
                changeState(State.LOADED);
                // TODO(akalachman): Investigate "Cursor finalized without prior close()"
            }
        });
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mListView = (ExpandableListView) view.findViewById(R.id.fragment_patient_list);
        mListView.setOnChildClickListener(this);

        mSwipeToRefresh = (SwipeRefreshLayout) view.findViewById(R.id.fragment_patient_list_swipe_to_refresh);
        mSwipeToRefresh.setOnRefreshListener(this);
        mFilter = FilterManager.getDefaultFilter();
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
                // isRefreshing = false;
                mFilterQueryTerm = q;
                changeState(State.LOADING);
                loadSearchResults();
            }
        });
    }

    public ExpandablePatientListAdapter getAdapterInstance() {
        return new ExpandablePatientListAdapter(
                null, getActivity(), mFilterQueryTerm, mFilter);
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
        Cursor childCursor = mPatientAdapter.getChild(groupPosition, childPosition);

        mCallbacks.onItemSelected(
                childCursor.getString(PatientProjection.COLUMN_UUID),
                childCursor.getString(PatientProjection.COLUMN_GIVEN_NAME),
                childCursor.getString(PatientProjection.COLUMN_FAMILY_NAME),
                childCursor.getString(PatientProjection.COLUMN_ID));
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

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return mFactory.getCursorLoader(mFilter, "");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        // Swap the new cursor in.
        int id = cursorLoader.getId();

        Log.d(TAG, "onLoadFinished id: " + id);
        if (id == LOADER_LIST_ID) {
            mPatientAdapter.setGroupCursor(cursor);
            changeState(State.LOADED);
            // stopRefreshing();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // is about to be closed.
        int id = cursorLoader.getId();
        if (id != LOADER_LIST_ID) {
            // child cursor
            try {
                mPatientAdapter.setChildrenCursor(id, null);
            } catch (NullPointerException e) {
                Log.w(TAG, "Adapter expired, try again on the next query: " + e.getMessage());
            }
        } else {
            mPatientAdapter.setGroupCursor(null);
        }
    }
}
