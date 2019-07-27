package org.projectbuendia.client.ui.lists;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import org.projectbuendia.client.R;
import org.projectbuendia.client.models.ObsRow;
import org.projectbuendia.client.ui.dialogs.ObsDetailDialogFragment.Section;
import org.projectbuendia.client.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class ExpandableObsRowAdapter extends BaseExpandableListAdapter {
    private final Context mContext;
    private final List<Section> mSections;
    private final List<List<ObsRow>> mSectionRows;

    private static String EM_DASH = "\u2014";
    private static String BULLET = "\u2022";

    public ExpandableObsRowAdapter(Context context, Map<Section, List<ObsRow>> rowsBySection) {
        mContext = context;
        mSections = new ArrayList<>(rowsBySection.keySet());
        mSectionRows = new ArrayList<>();

        Comparator<ObsRow> orderByTime = (a, b) -> a.time.compareTo(b.time);

        for (int i = 0; i < mSections.size(); i++) {
            Section section = mSections.get(i);
            List<ObsRow> rows = new ArrayList<>(rowsBySection.get(section));
            Collections.sort(rows, orderByTime);
            mSectionRows.add(rows);
        }
    }

    private View inflate(@LayoutRes int layoutId) {
        return ((LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
            .inflate(layoutId, null);
    }

    @Override public View getGroupView(int groupIndex, boolean isExpanded, View view, ViewGroup parent) {
        if (view == null) {
            view = inflate(R.layout.obs_section);
        }
        TextView heading = view.findViewById(R.id.section_heading);
        heading.setText((String) getGroup(groupIndex));
        return view;
    }

    @Override public View getChildView(int groupIndex, int childIndex, boolean isLastChild, View view, ViewGroup parent) {
        if (view == null) {
            view = inflate(R.layout.obs_item);
        }
        TextView item = view.findViewById(R.id.item_text);
        item.setText((String) getChild(groupIndex, childIndex));
        return view;
    }

    @Override public Object getGroup(int groupIndex) {
        Section section = mSections.get(groupIndex);
        return Utils.format("%s " + BULLET + " %s",
            section.conceptName, Utils.formatShortDate(section.date));
    }

    @Override public Object getChild(int groupIndex, int childIndex) {
        ObsRow row = mSectionRows.get(groupIndex).get(childIndex);
        return Utils.format("%s " + EM_DASH + " %s",
            Utils.formatTimeOfDay(row.time), row.valueName);
    }

    @Override public int getGroupCount() {
        return mSections.size();
    }

    @Override public int getChildrenCount(int groupPosition) {
        return mSectionRows.get(groupPosition).size();
    }

    @Override public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    @Override public boolean hasStableIds() {
        return false;
    }
}

