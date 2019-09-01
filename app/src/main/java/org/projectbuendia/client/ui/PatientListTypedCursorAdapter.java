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

import org.projectbuendia.client.App;
import org.projectbuendia.client.R;
import org.projectbuendia.client.models.ConceptUuids;
import org.projectbuendia.client.models.Location;
import org.projectbuendia.client.models.LocationForest;
import org.projectbuendia.client.models.Obs;
import org.projectbuendia.client.models.Patient;
import org.projectbuendia.client.models.Sex;
import org.projectbuendia.client.models.TypedCursor;
import org.projectbuendia.client.resolvables.ResStatus;
import org.projectbuendia.client.sync.ChartDataHelper;
import org.projectbuendia.client.utils.Logger;
import org.projectbuendia.client.utils.PatientCountDisplay;
import org.projectbuendia.client.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import butterknife.ButterKnife;
import butterknife.InjectView;

import static org.projectbuendia.client.utils.Utils.eq;

/**
 * A {@link BaseExpandableListAdapter} that wraps a {@link TypedCursor} of {@link Patient}'s,
 * displaying these patients grouped by location and filtered by a specified
 * {@link org.projectbuendia.client.filter.db.SimpleSelectionFilter}.
 */
public class PatientListTypedCursorAdapter extends BaseExpandableListAdapter {
    protected final Context mContext;

    private final HashMap<Location, List<Patient>> mPatientsByLocation;
    private final ChartDataHelper mChartDataHelper;
    private static final Logger LOG = Logger.create();
    private static final String EN_DASH = "\u2013";

    private Location[] mLocations;
    private Map<String, Obs> mConditionObs = new HashMap<>();

    /**
     * Creates a {@link PatientListTypedCursorAdapter}.
     * @param context an activity context
     */
    public PatientListTypedCursorAdapter(Context context) {
        mContext = context;
        mPatientsByLocation = new HashMap<>();
        mChartDataHelper = new ChartDataHelper(
            App.getSettings(), context.getContentResolver());
    }

    @Override public int getGroupCount() {
        if (mPatientsByLocation == null) {
            return 0;
        }

        return mPatientsByLocation.size();
    }

    @Override public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override public boolean hasStableIds() {
        return false;
    }

    @Override public View getGroupView(
        int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        Location location = getGroup(groupPosition);
        View view = convertView != null ? convertView : newGroupView();

        ExpandableListView expandableListView = (ExpandableListView) parent;
        expandableListView.expandGroup(groupPosition);
        TextView item = view.findViewById(R.id.patient_list_tent_tv);
        item.setText(PatientCountDisplay.getPatientCountTitle(
            mContext, getChildrenCount(groupPosition), location.name));
        return view;
    }

    @Override public Location getGroup(int groupPosition) {
        if (mLocations == null) {
            LOG.e("getGroup: mLocations is null! (see issue #352)");
            return null;
        }
        return mLocations[groupPosition];
    }

    @Override public int getChildrenCount(int groupPosition) {
        LOG.d("getChildrenCount: mLocations = %s (%d), groupPosition = %d", mLocations, mLocations != null ? mLocations.length : -1, groupPosition);
        Location location = getGroup(groupPosition);
        if (mPatientsByLocation == null || location == null) {
            return 0;
        }

        return mPatientsByLocation.get(location).size();
    }

    protected View newGroupView() {
        return LayoutInflater.from(mContext).inflate(R.layout.listview_tent_header, null);
    }

    @Override public View getChildView(
        int groupPosition, int childPosition, boolean isLastChild, View convertView,
        ViewGroup parent) {
        Patient patient = (Patient) getChild(groupPosition, childPosition);

        // Show condition, if the data for this has been loaded.
        Obs obs = mConditionObs.get(patient.uuid);
        String condition = obs != null ? obs.value : null;

        if (convertView == null) {
            convertView = newChildView();
        }

        ResStatus.Resolved status =
            ConceptUuids.getResStatus(condition).resolve(mContext.getResources());

        ViewHolder holder = (ViewHolder) convertView.getTag();
        String givenName = Utils.orDefault(patient.givenName, EN_DASH);
        String familyName = Utils.orDefault(patient.familyName, EN_DASH);
        holder.mName.setText(givenName + " " + familyName);
        holder.mBedNumber.setText(Utils.toNonnull(patient.bedNumber));
        holder.mId.setText(patient.id);
        holder.mId.setTextColor(status.getForegroundColor());
        holder.mId.setBackgroundColor(status.getBackgroundColor());

        holder.mAge.setText(patient.birthdate == null ? ""
            : Utils.birthdateToAge(patient.birthdate));

        boolean isChild = Utils.isChild(patient.birthdate);

        int drawableId = 0;

        // Java switch is not safe to use because it stupidly crashes on null.
        if (patient.sex == Sex.MALE) {
            drawableId = isChild ? R.drawable.ic_male_child : R.drawable.ic_male;
        }
        if (patient.sex == Sex.FEMALE) {
            drawableId = patient.pregnancy ? R.drawable.ic_female_pregnant :
                isChild ? R.drawable.ic_female_child : R.drawable.ic_female;
        }

        if (drawableId > 0) {
            holder.mSex.setVisibility(View.VISIBLE);
            holder.mSex.setImageDrawable(mContext.getResources().getDrawable(drawableId));
        } else {
            holder.mSex.setVisibility(View.GONE);
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

    @Override public Object getChild(int groupPosition, int childPosition) {
        return mPatientsByLocation.get(getGroup(groupPosition)).get(childPosition);
    }

    private View newChildView() {
        View view = LayoutInflater.from(mContext).inflate(
            R.layout.listview_cell_search_results, null, false);
        ViewHolder holder = new ViewHolder(view);
        view.setTag(holder);
        return view;
    }

    @Override public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    /**
     * Updates the adapter to show all patients from the given cursor.  (Does not
     * take ownership; the original owner remains responsible for closing it.)
     */
    public void setPatients(TypedCursor<Patient> cursor, LocationForest forest) {
        mPatientsByLocation.clear();

        // Add all patients from cursor.
        int count = cursor.getCount();
        for (int i = 0; i < count; i++) {
            addPatient(cursor.get(i), forest);
        }

        // Produce a sorted list of all the locations that have patients.
        mLocations = new Location[mPatientsByLocation.size()];
        mPatientsByLocation.keySet().toArray(mLocations);
        forest.sort(mLocations);

        // Sort the patient lists within each location by bed number, then ID.
        for (List<Patient> patients : mPatientsByLocation.values()) {
            Collections.sort(patients, (a, b) ->
                !eq(a.bedNumber, b.bedNumber) ?
                    Utils.ALPHANUMERIC_COMPARATOR.compare(a.bedNumber, b.bedNumber) :
                    Utils.ALPHANUMERIC_COMPARATOR.compare(a.id, b.id)
            );
        }

        new FetchObservationsTask().execute();
        notifyDataSetChanged();
    }

    // Add a single patient to relevant data structures.
    private void addPatient(Patient patient, LocationForest forest) {
        Location location = forest.get(patient.locationUuid);
        if (location != null) {  // shouldn't be null, but better to be safe
            if (!mPatientsByLocation.containsKey(location)) {
                mPatientsByLocation.put(location, new ArrayList<>());
            }
            mPatientsByLocation.get(location).add(patient);
        }
    }

    private class FetchObservationsTask extends AsyncTask<String, Void, Void> {
        @Override protected Void doInBackground(String... params) {
            Locale locale = App.getSettings().getLocale();
            mConditionObs = mChartDataHelper.getLatestObservationsForConcept(ConceptUuids.GENERAL_CONDITION_UUID);
            return null;
        }

        @Override protected void onPostExecute(Void result) {
            notifyDataSetChanged();
        }
    }

    static class ViewHolder {
        @InjectView(R.id.listview_cell_search_results_name) TextView mName;
        @InjectView(R.id.listview_cell_search_results_bed_number) TextView mBedNumber;
        @InjectView(R.id.listview_cell_search_results_id) TextView mId;
        @InjectView(R.id.listview_cell_search_results_sex) ImageView mSex;
        @InjectView(R.id.listview_cell_search_results_age) TextView mAge;

        public ViewHolder(View view) {
            ButterKnife.inject(this, view);
        }
    }
}
