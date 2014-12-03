package org.msf.records.ui;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.Filter;
import android.widget.ListView;

import org.msf.records.App;
import org.msf.records.R;
import org.msf.records.events.CreatePatientSucceededEvent;
import org.msf.records.filter.FilterManager;
import org.msf.records.filter.FilterQueryProviderFactory;
import org.msf.records.filter.SimpleSelectionFilter;
import org.msf.records.model.Location;
import org.msf.records.net.Constants;
import org.msf.records.sync.GenericAccountService;
import org.msf.records.sync.PatientProjection;
import org.msf.records.sync.PatientProviderContract;

import de.greenrobot.event.EventBus;

/**
 * A list fragment representing a list of Patients. This fragment
 * also supports tablet devices by allowing list items to be given an
 * 'activated' state upon selection. This helps indicate which item is
 * currently being viewed in a {@link PatientDetailFragment}.
 * <p>
 * Activities containing this fragment MUST implement the {@link Callbacks}
 * interface.
 */
public class PatientListFragment extends ProgressFragment implements
        ExpandableListView.OnChildClickListener, LoaderManager.LoaderCallbacks<Cursor>,
        SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = PatientListFragment.class.getSimpleName();
    private static final String ITEM_LIST_KEY = "ITEM_LIST_KEY";

    /**
     * The serialization (saved instance state) Bundle key representing the
     * activated item position. Only used on tablets.
     */
    private static final String STATE_ACTIVATED_POSITION = "activated_position";

    /**
     * The id to identify which Cursor is returned
     */
    private static final int LOADER_LIST_ID = 0;

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

    private FilterQueryProviderFactory mFactory =
            new FilterQueryProviderFactory().setUri(
                    PatientProviderContract.CONTENT_URI_PATIENT_TENTS);


    private boolean isRefreshing;

    String mFilterQueryTerm = "";

    SimpleSelectionFilter mFilter;

    public void filterBy(SimpleSelectionFilter filter) {
        mFilter = filter;
        mPatientAdapter.setSelectionFilter(mFilter);
        mPatientAdapter.setFilterQueryProvider(
                mFactory.getFilterQueryProvider(getActivity(), filter));
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

        setContentView(R.layout.fragment_patient_list);

    }

    @Override
    public void onResume() {
        super.onResume();

        EventBus.getDefault().register(this);
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
            getLoaderManager().restartLoader(LOADER_LIST_ID, null, this);
            //triggers app wide data refresh
            GenericAccountService.triggerRefresh();
            isRefreshing = true;
        }
    }

    private void stopRefreshing(){
        if (isRefreshing) {
            mSwipeToRefresh.setRefreshing(false);
            isRefreshing = false;
        }
    }

    private void loadSearchResults(){
        final Cursor oldCursor = mPatientAdapter.getCursor();
        mPatientAdapter.filter(mFilterQueryTerm, new Filter.FilterListener() {
            @Override
            public void onFilterComplete(int count) {
                changeState(State.LOADED);
                stopRefreshing();

                // TODO(akalachman): Necessary?
                mPatientAdapter.notifyDataSetChanged();

                if (oldCursor != null && !oldCursor.isClosed()) {
                    oldCursor.close();
                }
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

        Button allLocationsButton = (Button) view.findViewById(R.id.patient_list_all_locations);
        allLocationsButton.setOnClickListener(onClickListener);

        mFilter = FilterManager.getDefaultFilter();
        mPatientAdapter = new ExpandablePatientListAdapter(
                null, getActivity(), mFilterQueryTerm, mFilter);
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
                isRefreshing = false;
                mFilterQueryTerm = q;
                changeState(State.LOADING);
                loadSearchResults();
            }
        });
    }

    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            switch(v.getId()){
                case R.id.patient_list_all_locations:
                    FragmentManager fm = getChildFragmentManager();
                    ListDialogFragment dialogListFragment = new ListDialogFragment();
                    Bundle bundle = new Bundle();
                    bundle.putParcelableArray(ITEM_LIST_KEY, Location.getLocation());
                    dialogListFragment.setArguments(bundle);
                    dialogListFragment.show(fm, null);
                    break;
            }
        }
    };

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

    public void onEvent(CreatePatientSucceededEvent event) {
        onRefresh();
    }

    /**
     * Turns on activate-on-click mode. When this mode is on, list items will be
     * given the 'activated' state when touched.
     */
    public void setActivateOnItemClick(boolean activateOnItemClick) {
        // When setting CHOICE_MODE_SINGLE, ListView will automatically
        // give items the 'activated' state when touched.
        mListView.setChoiceMode(activateOnItemClick
                ? ListView.CHOICE_MODE_SINGLE
                : ListView.CHOICE_MODE_NONE);
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
        return mFactory.getCursorLoader(getActivity(), mFilter, "");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        // Swap the new cursor in.
        int id = cursorLoader.getId();

        Log.d(TAG, "onLoadFinished id: " + id);
        if (id == LOADER_LIST_ID) {
            mPatientAdapter.setGroupCursor(cursor);
            changeState(State.LOADED);
            stopRefreshing();
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
