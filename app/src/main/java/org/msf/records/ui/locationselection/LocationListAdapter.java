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

package org.msf.records.ui.locationselection;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

import org.msf.records.R;
import org.msf.records.data.app.AppLocation;
import org.msf.records.data.app.AppLocationTree;
import org.msf.records.data.res.ResZone;
import org.msf.records.model.Zone;
import org.msf.records.utils.PatientCountDisplay;
import org.msf.records.widget.SubtitledButtonView;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

/** {@link ArrayAdapter} for displaying a list of locations. */
final class LocationListAdapter extends ArrayAdapter<AppLocation> {

    private final Context mContext;
    private final AppLocationTree mLocationTree;
    private Optional<String> mSelectedLocationUuid;

    public LocationListAdapter(
            Context context,
            List<AppLocation> locations,
            AppLocationTree locationTree,
            Optional<String> selectedLocation) {
        super(context, R.layout.listview_cell_location_selection, locations);
        mContext = context;
        mLocationTree = locationTree;
        mSelectedLocationUuid = Preconditions.checkNotNull(selectedLocation);
    }

    public Optional<String> getSelectedLocationUuid() {
        return mSelectedLocationUuid;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view;
        ViewHolder holder;
        if (convertView != null) {
            view = convertView;
            holder = (ViewHolder) convertView.getTag();
        } else {
            view = inflater.inflate(
                R.layout.listview_cell_location_selection, parent, false);
            holder = new ViewHolder(view);
            view.setTag(holder);
        }

        AppLocation location = getItem(position);
        // TODO/generalize: Make this more robust. Currently, this line only works if 'location' is
        // a tent; otherwise zone is ResZone.UNKNOWN
        ResZone.Resolved zone = Zone.getResZone(
                location.parentUuid).resolve(mContext.getResources());

        holder.mButton.setTitle(location.toString());
        holder.mButton.setSubtitle(
                PatientCountDisplay.getPatientCountSubtitle(
                        mContext, mLocationTree.getTotalPatientCount(location)));
        holder.mButton.setBackgroundColor(zone.getBackgroundColor());
        holder.mButton.setTextColor(zone.getForegroundColor());

        if (mSelectedLocationUuid.isPresent()
                && mSelectedLocationUuid.get().equals(location.uuid)) {
            view.setBackgroundResource(R.color.zone_location_selected_padding);
        } else {
            view.setBackgroundResource(R.drawable.location_selector);
        }

        return view;
    }

    public void setSelectedLocationUuid(Optional<String> locationUuid) {
        mSelectedLocationUuid = locationUuid;

        notifyDataSetChanged();
    }

    static class ViewHolder {

        @InjectView(R.id.location_selection_location) SubtitledButtonView mButton;

        public ViewHolder(View view) {
            ButterKnife.inject(this, view);
        }
    }
}
