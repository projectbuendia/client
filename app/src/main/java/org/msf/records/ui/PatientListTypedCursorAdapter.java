package org.msf.records.ui;

import android.content.Context;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;

import org.msf.records.R;
import org.msf.records.data.app.AppPatient;
import org.msf.records.data.app.TypedCursor;
import org.msf.records.data.res.ResStatus;
import org.msf.records.location.LocationTree;
import org.msf.records.model.Concept;
import org.msf.records.sync.LocalizedChartHelper;
import org.msf.records.utils.PatientCountDisplay;
import org.msf.records.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * A {@link BaseExpandableListAdapter} that wraps a {@link TypedCursor} of {@link AppPatient}'s,
 * displaying these patients grouped by location and filtered by a specified
 * {@link org.msf.records.filter.SimpleSelectionFilter}.
 */
public class PatientListTypedCursorAdapter extends BaseExpandableListAdapter {
    protected final Context mContext;

    private final TreeMap<LocationTree.LocationSubtree, List<AppPatient>> mPatientsByLocation;
    private final LocationTree mLocationTree;
    private final LocalizedChartHelper mLocalizedChartHelper;

    private LocationTree.LocationSubtree[] mLocations;
    private Map<String, Map<String, LocalizedChartHelper.LocalizedObservation>> mObservations;

    /**
     * Creates a {@link PatientListTypedCursorAdapter}.
     *
     * @param context an activity context
     */
    public PatientListTypedCursorAdapter(Context context) {
        mContext = context;

        mPatientsByLocation = new TreeMap<LocationTree.LocationSubtree, List<AppPatient>>();

        // TODO(dxchen): Use injected location tree instead of singleton.
        mLocationTree = LocationTree.singletonInstance;
        mLocalizedChartHelper = new LocalizedChartHelper(context.getContentResolver());
    }

    @Override
    public int getGroupCount() {
        if (mPatientsByLocation == null) {
            return 0;
        }

        return mPatientsByLocation.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        Object patientsForLocation = getGroup(groupPosition);
        if (mPatientsByLocation == null || patientsForLocation == null) {
            return 0;
        }

        return mPatientsByLocation.get(patientsForLocation).size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return mLocations[groupPosition];
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return mPatientsByLocation.get(getGroup(groupPosition)).get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(
            int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        LocationTree.LocationSubtree location =
                (LocationTree.LocationSubtree)getGroup(groupPosition);

        int patientCount = getChildrenCount(groupPosition);
        String tentName = location.toString();

        if (convertView == null) {
            convertView = newGroupView();
        }

        ExpandableListView expandableListView = (ExpandableListView) parent;
        expandableListView.expandGroup(groupPosition);

        TextView item = (TextView) convertView.findViewById(R.id.patient_list_tent_tv);
        item.setText(PatientCountDisplay.getPatientCountTitle(mContext, patientCount, tentName));

        return convertView;
    }

    protected View newGroupView() {
        return LayoutInflater.from(mContext).inflate(R.layout.listview_tent_header, null);
    }

    @Override
    public View getChildView(
            int groupPosition, int childPosition, boolean isLastChild, View convertView,
            ViewGroup parent) {
        AppPatient patient = (AppPatient)getChild(groupPosition, childPosition);

        // Show pregnancy status and condition if present.
        boolean pregnant = false;
        String condition = null;
        if (mObservations != null) {
            Map<String, LocalizedChartHelper.LocalizedObservation> observationMap =
                    mObservations.get(patient.uuid);
            if (observationMap != null) {
                pregnant = observationMap.containsKey(Concept.PREGNANCY_UUID)
                        && observationMap.get(Concept.PREGNANCY_UUID).value.equals(
                        Concept.YES_UUID);
                if (observationMap.containsKey(Concept.GENERAL_CONDITION_UUID)) {
                    condition = observationMap.get(Concept.GENERAL_CONDITION_UUID).value;
                }
            }
        }

        if (convertView == null) {
            convertView = newChildView();
        }

        ResStatus.Resolved status =
                Concept.getResStatus(condition).resolve(mContext.getResources());

        ViewHolder holder = (ViewHolder) convertView.getTag();
        holder.mPatientName.setText(patient.givenName + " " + patient.familyName);
        holder.mPatientId.setText(patient.id);
        holder.mPatientId.setTextColor(status.getForegroundColor());
        holder.mPatientId.setBackgroundColor(status.getBackgroundColor());

        holder.mPatientAge.setText(
                patient.birthdate == null ? "" : Utils.birthdateToAge(patient.birthdate));

        holder.mPatientGender.setVisibility(
                patient.gender == AppPatient.GENDER_UNKNOWN ? View.GONE : View.VISIBLE);

        if (patient.gender != AppPatient.GENDER_UNKNOWN) {
            holder.mPatientGender.setImageDrawable(mContext.getResources().getDrawable(
                    patient.gender == AppPatient.GENDER_MALE ? R.drawable.ic_gender_male
                            : pregnant ? R.drawable.ic_gender_female_pregnant
                            : R.drawable.ic_gender_female
            ));
        }

        // Add a bottom border and extra padding to the last item in each group.
        if (isLastChild) {
            convertView.setBackgroundResource(R.drawable.bottom_border_1dp);
            convertView.setPadding(
                    convertView.getPaddingLeft(), convertView.getPaddingTop(),
                    convertView.getPaddingRight(), 40);
        } else {
            convertView.setBackgroundResource(0);
            convertView.setPadding(
                    convertView.getPaddingLeft(), convertView.getPaddingTop(),
                    convertView.getPaddingRight(), 20);
        }

        ExpandableListView expandableListView = (ExpandableListView) parent;
        expandableListView.expandGroup(groupPosition);

        return convertView;
    }

    private View newChildView() {
        View view = LayoutInflater.from(mContext).inflate(
                R.layout.listview_cell_search_results, null, false);
        ViewHolder holder = new ViewHolder(view);
        view.setTag(holder);
        return view;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    /**
     * Updates the adapter to show all patients from the given cursor.
     *
     * @param cursor a {@link TypedCursor} containing all patients to show
     */
    public void setPatients(TypedCursor<AppPatient> cursor) {
        mPatientsByLocation.clear();
        if (mObservations != null) {
            mObservations.clear();
        }

        String[] patientUuids = new String[cursor.getCount()];

        // Add all patients from cursor.
        for (int i = 0; i < cursor.getCount(); i++) {
            AppPatient patient = cursor.get(i);
            addPatient(patient);
            patientUuids[i] = patient.uuid;
        }

        // Populate locations list accordingly.
        mLocations = new LocationTree.LocationSubtree[mPatientsByLocation.size()];
        mPatientsByLocation.keySet().toArray(mLocations);

        // Finalize cursor.
        cursor.close();

        // Sort each of the patient lists by the default patient comparator.
        for (Map.Entry<LocationTree.LocationSubtree, List<AppPatient>> entry :
                mPatientsByLocation.entrySet()) {
            Collections.sort(entry.getValue());
        }

        new FetchObservationsTask().execute(patientUuids);

        notifyDataSetChanged();
    }

    private class FetchObservationsTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {
            mObservations = mLocalizedChartHelper.getMostRecentObservationsBatch(params, "en");
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            notifyDataSetChanged();
        }
    }

    // Add a single patient to relevant data structures.
    private void addPatient(AppPatient patient) {
        String locationUuid = patient.locationUuid;
        LocationTree.LocationSubtree location = mLocationTree.getLocationByUuid(locationUuid);
        // Skip any patients with no location. This case shouldn't happen, but better to hide
        // the patient than cause a crash.
        if (location == null) {
            return;
        }

        if (!mPatientsByLocation.containsKey(location)) {
            mPatientsByLocation.put(location, new ArrayList<AppPatient>());
        }
        mPatientsByLocation.get(location).add(patient);

        return;
    }

    static class ViewHolder {
        @InjectView(R.id.listview_cell_search_results_name) TextView mPatientName;
        @InjectView(R.id.listview_cell_search_results_id) TextView mPatientId;
        @InjectView(R.id.listview_cell_search_results_gender) ImageView mPatientGender;
        @InjectView(R.id.listview_cell_search_results_age) TextView mPatientAge;

        public ViewHolder(View view) {
            ButterKnife.inject(this, view);
        }
    }
}