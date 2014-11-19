package org.msf.records.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.msf.records.App;
import org.msf.records.R;
import org.msf.records.model.Location;
import org.msf.records.model.Patient;
import org.msf.records.model.Status;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

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
        AdapterView.OnItemClickListener, Response.Listener<List<Patient>>,
        SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = PatientListFragment.class.getSimpleName();
    private static final String ITEM_LIST_KEY = "ITEM_LIST_KEY";

    /**
     * The serialization (saved instance state) Bundle key representing the
     * activated item position. Only used on tablets.
     */
    private static final String STATE_ACTIVATED_POSITION = "activated_position";

    /**
     * The fragment's current callback object, which is notified of list item
     * clicks.
     */
    private Callbacks mCallbacks = sDummyCallbacks;

    /**
     * The current activated item position. Only used on tablets.
     */
    private int mActivatedPosition = ListView.INVALID_POSITION;

    private PatientAdapter mPatientAdapter;

    private ListView mListView;

    private SwipeRefreshLayout mSwipeToRefresh;

    private boolean isRefreshing;


    String mFilterLocation;

    String mFilterQueryTerm;

    String mFilterState;



    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callbacks {
        /**
         * Callback for when an item has been selected.
         */
        public void onItemSelected(String id);
    }

    /**
     * A dummy implementation of the {@link Callbacks} interface that does
     * nothing. Used only when this fragment is not attached to an activity.
     */
    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onItemSelected(String id) {
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
    public void onRefresh() {
        if(!isRefreshing){
            isRefreshing = true;
            loadSearchResults();
        }
    }

    @Override
    public void onResponse(List<Patient> patients) {
        Log.d(TAG, "onResponse ");
        if(isRefreshing){
            Log.d(TAG, "onResponse refresh");

            mPatientAdapter.clear();
            mPatientAdapter.notifyDataSetChanged();
        }
        mPatientAdapter.addAll(patients);
        mPatientAdapter.notifyDataSetChanged();
        changeState(State.LOADED);
        stopRefreshing();
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        stopRefreshing();
    }

    private void stopRefreshing(){
        if(isRefreshing){
            mSwipeToRefresh.setRefreshing(false);
            isRefreshing = false;
        }
    }

    private void loadSearchResults(){
        App.getServer().listPatients(mFilterState, mFilterLocation, mFilterQueryTerm,
                this, this, TAG);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mListView = (ListView) view.findViewById(R.id.fragment_patient_list);
        mListView.setOnItemClickListener(this);

        mSwipeToRefresh = (SwipeRefreshLayout) view.findViewById(R.id.fragment_patient_list_swipe_to_refresh);
        mSwipeToRefresh.setOnRefreshListener(this);

        Button allLocationsButton = (Button) view.findViewById(R.id.patient_list_all_locations);
        allLocationsButton.setOnClickListener(onClickListener);

        mPatientAdapter = new PatientAdapter(getActivity(), 0);
        mListView.setAdapter(mPatientAdapter);

        loadSearchResults();

        // Restore the previously serialized activated item position.
        if (savedInstanceState != null
                && savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
            setActivatedPosition(savedInstanceState.getInt(STATE_ACTIVATED_POSITION));
        }

        ((PatientListActivity)getActivity()).setOnSearchListener(new PatientListActivity.OnSearchListener() {
            @Override
            public void setQuerySubmitted(String q) {
                App.getServer().cancelPendingRequests(TAG);
                isRefreshing = false;
                mFilterQueryTerm = q;
                changeState(State.LOADING);
                onRefresh();
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
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // Reset the active callbacks interface to the dummy implementation.
        mCallbacks = sDummyCallbacks;
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mCallbacks.onItemSelected(mPatientAdapter.getItem(position).uuid);

    }
    /**
     @Override
     public void onListItemClick(int position, int type) {
     Log.d(TAG, "position: " + position + " type: " + type);
     if(position == 0)
     mFilterLocation = null;
     else
     mFilterLocation = "" + (position - 1);
     App.getInstance().cancelPendingRequests(TAG);
     isRefreshing = false;
     changeState(State.LOADING);
     onRefresh();
     }
     **/

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mActivatedPosition != ListView.INVALID_POSITION) {
            // Serialize and persist the activated item position.
            outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
        }
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

    class PatientAdapter extends ArrayAdapter<Patient> {

        public PatientAdapter(Context context, int resource) {
            super(context, resource);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView != null) {
                holder = (ViewHolder) convertView.getTag();
            } else {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.listview_cell_search_results, parent, false);
                holder = new ViewHolder(convertView);
                convertView.setTag(holder);
            }


            Patient patient = getItem(position);

            holder.mPatientName.setText(patient.given_name + " " + patient.family_name);
            holder.mPatientId.setText(patient.id);

            if (patient.age.type != null && patient.age.type.equals("months")) {
                holder.mPatientAge.setText("<1");
            }

            if (patient.age.type != null && patient.age.type.equals("years")) {
                holder.mPatientAge.setText("" + patient.age.years);
            }

            if (patient.age.type == null) {
                holder.mPatientAge.setText("99");
                holder.mPatientAge.setTextColor(getResources().getColor(R.color.transparent));
            }

            if (patient.gender != null && patient.gender.equals("M")) {
                holder.mPatientGender.setImageDrawable(getResources().getDrawable(R.drawable.gender_man));
            }

            if (patient.gender != null && patient.gender.equals("F") && patient.pregnant) {
                holder.mPatientGender.setImageDrawable(getResources().getDrawable(R.drawable.gender_woman));
            }

            if (patient.pregnant) {
                holder.mPatientGender.setImageDrawable(getResources().getDrawable(R.drawable.gender_pregnant));
            }

            if (patient.gender == null) {
                holder.mPatientGender.setVisibility(View.GONE);
            }

            if (patient.status == null) {
                holder.mPatientListStatusColorIndicator.setBackgroundColor(getResources().getColor(R.color.transparent));
            }

            if (patient.status != null){
                holder.mPatientListStatusColorIndicator.setBackgroundColor(getResources().getColor(Status.getStatus(patient.status).colorId));
            }

            return convertView;
        }
    }

    static class ViewHolder {
        @InjectView(R.id.listview_cell_search_results_color_indicator) ImageView mPatientListStatusColorIndicator;
        @InjectView(R.id.listview_cell_search_results_name) TextView mPatientName;
        @InjectView(R.id.listview_cell_search_results_id) TextView mPatientId;
        @InjectView(R.id.listview_cell_search_results_gender) ImageView mPatientGender;
        @InjectView(R.id.listview_cell_search_results_age) TextView mPatientAge;

        public ViewHolder(View view) {
            ButterKnife.inject(this, view);
        }
    }
}
