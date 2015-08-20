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

package org.projectbuendia.client.ui;

import android.content.Context;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;

import org.projectbuendia.client.R;
import org.projectbuendia.client.data.app.AppLocation;
import org.projectbuendia.client.data.app.AppLocationComparator;
import org.projectbuendia.client.data.app.AppLocationTree;
import org.projectbuendia.client.data.app.AppPatient;
import org.projectbuendia.client.data.app.TypedCursor;
import org.projectbuendia.client.data.res.ResStatus;
import org.projectbuendia.client.model.Concepts;
import org.projectbuendia.client.sync.LocalizedChartHelper;
import org.projectbuendia.client.sync.LocalizedObs;
import org.projectbuendia.client.utils.Utils;
import org.projectbuendia.client.utils.PatientCountDisplay;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * A {@link BaseExpandableListAdapter} that wraps a {@link TypedCursor} of {@link AppPatient}'s,
 * displaying these patients grouped by location and filtered by a specified
 * {@link org.projectbuendia.client.filter.db.SimpleSelectionFilter}.
 */
public class PatientListTypedCursorAdapter extends BaseExpandableListAdapter {
    protected final Context mContext;

    private final HashMap<AppLocation, List<AppPatient>> mPatientsByLocation;
    private final AppLocationTree mLocationTree;
    private final LocalizedChartHelper mLocalizedChartHelper;

    private AppLocation[] mLocations;
    private Map<String, Map<String, LocalizedObs>> mObservations;

    /**
     * Creates a {@link PatientListTypedCursorAdapter}.
     *
     * @param context an activity context
     */
    public PatientListTypedCursorAdapter(Context context, AppLocationTree locationTree) {
        mContext = context;

        mPatientsByLocation = new HashMap<AppLocation, List<AppPatient>>();

        mLocationTree = locationTree;
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
        AppLocation location = (AppLocation)getGroup(groupPosition);

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
        AppPatient patient = (AppPatient) getChild(groupPosition, childPosition);

        // Show pregnancy status and condition if present.
        boolean pregnant = false;
        String condition = null;
        if (mObservations != null) {
            Map<String, LocalizedObs> obsMap = mObservations.get(patient.uuid);
            if (obsMap != null) {
                LocalizedObs pregObs = obsMap.get(Concepts.PREGNANCY_UUID);
                pregnant = pregObs == null ? false : Concepts.YES_UUID.equals(pregObs.value);
                LocalizedObs condObs = obsMap.get(Concepts.GENERAL_CONDITION_UUID);
                condition = condObs == null ? null : condObs.value;
            }
        }

        if (convertView == null) {
            convertView = newChildView();
        }

        ResStatus.Resolved status =
                Concepts.getResStatus(condition).resolve(mContext.getResources());

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
     * Updates the adapter to show all patients from the given cursor.  (Does not
     * take ownership; the original owner remains responsible for closing it.)
     */
    public void setPatients(TypedCursor<AppPatient> cursor) {
        mPatientsByLocation.clear();
        if (mObservations != null) {
            mObservations.clear();
        }

        int count = cursor.getCount();
        String[] patientUuids = new String[count];

        // Add all patients from cursor.
        for (int i = 0; i < count; i++) {
            AppPatient patient = cursor.get(i);
            addPatient(patient);
            patientUuids[i] = patient.uuid;
        }

        // Produce a sorted list of all the locations that have patients.
        mLocations = new AppLocation[mPatientsByLocation.size()];
        mPatientsByLocation.keySet().toArray(mLocations);
        Arrays.sort(mLocations, new AppLocationComparator(mLocationTree));

        // Sort the patient lists within each location using the default comparator.
        for (List<AppPatient> patients : mPatientsByLocation.values()) {
            Collections.sort(patients);
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
        AppLocation location = mLocationTree.findByUuid(patient.locationUuid);
        if (location != null) {  // shouldn't be null, but better to be safe
            if (!mPatientsByLocation.containsKey(location)) {
                mPatientsByLocation.put(location, new ArrayList<AppPatient>());
            }
            mPatientsByLocation.get(location).add(patient);
        }
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
