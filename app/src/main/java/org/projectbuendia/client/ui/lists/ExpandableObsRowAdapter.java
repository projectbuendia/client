package org.projectbuendia.client.ui.lists;

import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.projectbuendia.client.R;
import org.projectbuendia.client.models.Obs;
import org.projectbuendia.client.utils.Utils;

import java.util.List;

public class ExpandableObsRowAdapter extends BaseExpandableListAdapter {
    private final List<Pair<LocalDate, List<Obs>>> mData;

    public ExpandableObsRowAdapter(List<Pair<LocalDate, List<Obs>>> data) {
        mData = data;
    }

    @Override
    public View getChildView(int groupPosition, final int childPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {
        Obs obs = (Obs) getChild(groupPosition, childPosition);
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            convertView = inflater.inflate(R.layout.observation_list_item, parent, false);
        }

        TextView obsTime = (TextView) convertView.findViewById(R.id.obs_time);
        TextView obsValue = (TextView) convertView.findViewById(R.id.obs_value);

        obsTime.setText(obs.time.toString(DateTimeFormat.shortTime()));
        // TODO: factor out and reuse the same formatting mechanism as the patient chart.
        if (obs.valueName != null) {
            obsValue.setText(obs.valueName);
        } else {
            obsValue.setText(obs.value);
        }

        return convertView;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded,
                             View convertView, ViewGroup parent) {
        LocalDate date = (LocalDate) getGroup(groupPosition);
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            convertView = inflater.inflate(R.layout.observation_group_header, parent, false);
        }

        TextView time = (TextView) convertView;
        time.setText(Utils.toShortString(date));

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    @Override
    public boolean hasStableIds() {
        // We're not expecting our dataset to change, so this isn't a huge deal.
        return false;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return this.mData.get(groupPosition).second.size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return this.mData.get(groupPosition).first;
    }

    @Override
    public int getGroupCount() {
        return this.mData.size();
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return mData.get(groupPosition).second.get(childPosition);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

}

