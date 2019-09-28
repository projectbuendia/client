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
import android.graphics.PorterDuff;
import android.os.AsyncTask;
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
import org.projectbuendia.client.models.TypedCursor;
import org.projectbuendia.client.resolvables.ResStatus;
import org.projectbuendia.client.sync.ChartDataHelper;
import org.projectbuendia.client.utils.ContextUtils;
import org.projectbuendia.client.utils.Logger;
import org.projectbuendia.client.utils.Utils;
import org.projectbuendia.client.widgets.ShrinkFitTextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import butterknife.ButterKnife;
import butterknife.InjectView;

import static org.projectbuendia.client.utils.ContextUtils.FormatStyle.NONE;
import static org.projectbuendia.client.utils.ContextUtils.FormatStyle.SHORT;
import static org.projectbuendia.client.utils.Utils.eq;

/**
 * A ListAdapter that displays Patients from a TypedCursor, grouped by location
 * and filtered by an optional SimpleSelectionFilter.
 */
public class PatientListAdapter extends BaseExpandableListAdapter {
    protected final ContextUtils u;
    private final HashMap<Location, List<Patient>> mPatientsByLocation;
    private final ChartDataHelper mChartDataHelper;

    private static final Logger LOG = Logger.create();

    private Location[] mLocations;
    private Map<String, Obs> mConditionObs = new HashMap<>();

    public PatientListAdapter(Context context) {
        u = ContextUtils.from(context);
        mPatientsByLocation = new HashMap<>();
        mChartDataHelper = App.getChartDataHelper();
    }

    @Override public int getGroupCount() {
        return mPatientsByLocation != null ? mPatientsByLocation.size() : 0;
    }

    @Override public long getGroupId(int groupIndex) {
        return groupIndex;
    }

    @Override public long getChildId(int groupIndex, int childIndex) {
        return childIndex;
    }

    @Override public boolean hasStableIds() {
        return false;
    }

    @Override public View getGroupView(
        int groupIndex, boolean isExpanded, View view, ViewGroup parent) {
        Location location = getGroup(groupIndex);
        view = u.reuseOrInflate(view, R.layout.patient_list_group_heading, parent);

        ((ExpandableListView) parent).expandGroup(groupIndex);
        TextView item = view.findViewById(R.id.heading);
        item.setText(u.formatLocationHeading(
            location.uuid, getChildrenCount(groupIndex)));
        return view;
    }

    @Override public Location getGroup(int groupIndex) {
        if (mLocations == null) {
            LOG.e("getGroup: mLocations is null! (see issue #352)");
            return null;
        }
        return mLocations[groupIndex];
    }

    @Override public int getChildrenCount(int groupIndex) {
        LOG.d("getChildrenCount: mLocations = %s (%d), groupIndex = %d", mLocations, mLocations != null ? mLocations.length : -1, groupIndex);
        Location location = getGroup(groupIndex);
        if (mPatientsByLocation == null || location == null) {
            return 0;
        }

        return mPatientsByLocation.get(location).size();
    }

    @Override public View getChildView(int groupIndex, int childIndex,
        boolean isLastChild, View view, ViewGroup parent) {
        Patient patient = (Patient) getChild(groupIndex, childIndex);
        view = u.reuseOrInflate(view, R.layout.patient_list_item, parent);

        // Show condition, if the data for this has been loaded.
        Obs obs = mConditionObs.get(patient.uuid);
        String condition = obs != null ? obs.value : null;
        ResStatus.Resolved status =
            ConceptUuids.getResStatus(condition).resolve(u.getResources());

        u.setContainer(view);
        u.setText(R.id.name, u.formatPatientName(patient));
        u.setText(R.id.bed_number, patient.bedNumber);
        u.show(R.id.bed_bar, !Utils.isEmpty(patient.bedNumber));
        ShrinkFitTextView idView = u.findView(R.id.id);
        idView.setTextAndResize(patient.id);
        idView.setTextColor(status.getForegroundColor());
        idView.getBackground().setColorFilter(status.getBackgroundColor(), PorterDuff.Mode.SRC_ATOP);
        u.setText(R.id.sex, u.formatPatientDetails(patient, SHORT, SHORT, NONE));
        u.setText(R.id.age, u.formatPatientDetails(patient, NONE, NONE, SHORT));

        ((ExpandableListView) parent).expandGroup(groupIndex);
        return view;
    }

    @Override public Object getChild(int groupIndex, int childIndex) {
        return mPatientsByLocation.get(getGroup(groupIndex)).get(childIndex);
    }

    @Override public boolean isChildSelectable(int groupIndex, int childIndex) {
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
        @InjectView(R.id.name) TextView mName;
        @InjectView(R.id.bed_number) TextView mBedNumber;
        @InjectView(R.id.id) TextView mId;
        @InjectView(R.id.sex) ImageView mSex;
        @InjectView(R.id.age) TextView mAge;

        public ViewHolder(View view) {
            ButterKnife.inject(this, view);
        }
    }
}
