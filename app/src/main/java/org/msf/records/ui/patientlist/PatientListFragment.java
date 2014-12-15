package org.msf.records.ui.patientlist;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.Toast;

import butterknife.ButterKnife;
import butterknife.InjectView;

import javax.inject.Inject;

import org.msf.records.App;
import org.msf.records.R;
import org.msf.records.data.app.AppModel;
import org.msf.records.filter.FilterManager;
import org.msf.records.location.LocationManager;
import org.msf.records.location.LocationTree;
import org.msf.records.net.Constants;
import org.msf.records.sync.GenericAccountService;
import org.msf.records.sync.PatientProjection;
import org.msf.records.sync.SyncManager;
import org.msf.records.ui.ExpandablePatientListAdapter;
import org.msf.records.ui.PatientSearchActivity;
import org.msf.records.ui.ProgressFragment;

import de.greenrobot.event.EventBus;

/**
 * A list fragment representing a list of Patients.
 *
 * <p>Activities containing this fragment MUST implement the {@link ActivityCallbacks}
 * interface.
 */
public class PatientListFragment extends ProgressFragment {

    private static final String TAG = PatientListFragment.class.getSimpleName();

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface ActivityCallbacks {
        /**
         * Callback for when an item has been selected.
         */
        public void onItemSelected(String uuid, String givenName, String familyName, String id);
    }

    /**
     * The serialization (saved instance state) Bundle key representing the
     * activated item position. Only used on tablets.
     */
    private static final String STATE_ACTIVATED_POSITION = "activated_position";

    @Inject LocationManager mLocationManager;
    @Inject SyncManager mSyncManager;
    @Inject AppModel mAppModel;

    private ActivityCallbacks mCallbacks;

    /** The current activated item position. */
    private int mActivatedPosition = ListView.INVALID_POSITION;

    private ExpandablePatientListAdapter mPatientAdapter;

    @InjectView(R.id.fragment_patient_list) private ExpandableListView mListView;
    @InjectView(R.id.fragment_patient_list_swipe_to_refresh) private SwipeRefreshLayout mSwipeToRefresh;

    private PatientListController mController;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.getInstance().inject(this);
        // TODO(rjlothian): Request the location tree from LocationManager instead of using the singleton.
        mController = new PatientListController(
        		mSyncManager,
        		mAppModel,
        		mLocationManager,
        		EventBus.getDefault(),
        		new MyUi(),
        		FilterManager.getDefaultFilter(),
        		LocationTree.SINGLETON_INSTANCE);
        setContentView(R.layout.fragment_patient_list);
    }

    @Override
    public void onStart() {
    	super.onStart();
        mController.init();
    }

    @Override
    public void onStop() {
    	mController.suspend();
    	super.onStop();
    }

    private final class MySwipeToRefreshListener implements SwipeRefreshLayout.OnRefreshListener {
	    @Override
	    public void onRefresh() {
	    	mController.onRefreshRequested();
	    }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.inject(this, view);

        mListView.setOnChildClickListener(new MyListChildClickListener());
        mSwipeToRefresh.setOnRefreshListener(new MySwipeToRefreshListener());


        mPatientAdapter = new ExpandablePatientListAdapter(
                null, getActivity(), mFilterQueryTerm, mFilter);
        mListView.setAdapter(mPatientAdapter);
        setFilter(mFilter);

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

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mCallbacks = (ActivityCallbacks) activity;
        if (Constants.OFFLINE_SUPPORT) {
            // Create account, if needed
            GenericAccountService.registerSyncAccount(activity);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mActivatedPosition != ListView.INVALID_POSITION) {
            // Serialize and persist the activated item position.
            outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
        }
    }

    private final class MyListChildClickListener implements ExpandableListView.OnChildClickListener {
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
    }

    private void setActivatedPosition(int position) {
        if (position == ListView.INVALID_POSITION) {
            mListView.setItemChecked(mActivatedPosition, false);
        } else {
            mListView.setItemChecked(position, true);
        }

        mActivatedPosition = position;
    }

    private final class MyUi implements PatientListController.Ui {

    	@Override
		public void showRefreshSpinner(boolean show) {
            mSwipeToRefresh.setRefreshing(show);
	    }

    	@Override
    	public void showErrorToast(int stringResourceId) {
    		Toast.makeText(getActivity(), getString(stringResourceId), Toast.LENGTH_SHORT).show();
    	}
    }

}
