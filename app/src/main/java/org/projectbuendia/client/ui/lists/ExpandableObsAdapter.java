package org.projectbuendia.client.ui.lists;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;

import org.projectbuendia.client.R;
import org.projectbuendia.client.models.Obs;
import org.projectbuendia.client.ui.dialogs.ObsDetailDialogFragment.Section;
import org.projectbuendia.client.utils.ContextUtils;
import org.projectbuendia.client.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.projectbuendia.client.utils.Utils.DateStyle.HOUR_MINUTE;
import static org.projectbuendia.client.utils.Utils.DateStyle.MONTH_DAY;

public class ExpandableObsAdapter extends BaseExpandableListAdapter {
    private final ContextUtils u;
    private final List<Section> mSections;
    private final List<List<Obs>> mSectionObservations;

    private static String EM_DASH = "\u2014";
    private static String BULLET = "\u2022";

    public ExpandableObsAdapter(Context context, Map<Section, List<Obs>> observationsBySection) {
        u = ContextUtils.from(context);
        mSections = new ArrayList<>(observationsBySection.keySet());
        mSectionObservations = new ArrayList<>();

        Comparator<Obs> orderByTime = (a, b) -> a.time.compareTo(b.time);

        for (int i = 0; i < mSections.size(); i++) {
            Section section = mSections.get(i);
            List<Obs> observations = new ArrayList<>(observationsBySection.get(section));
            Collections.sort(observations, orderByTime);
            mSectionObservations.add(observations);
        }
    }

    @Override public View getGroupView(
        int groupIndex, boolean isExpanded, View view, ViewGroup parent) {
        view = u.reuseOrInflate(view, R.layout.obs_heading, parent);
        u.setText(R.id.heading_text, (String) getGroup(groupIndex));
        return view;
    }

    @Override public View getChildView(
        int groupIndex, int childIndex, boolean isLastChild, View view, ViewGroup parent) {
        view = u.reuseOrInflate(view, R.layout.obs_item, parent);
        u.setText(R.id.item_text, (String) getChild(groupIndex, childIndex));
        return view;
    }

    @Override public Object getGroup(int groupIndex) {
        Section section = mSections.get(groupIndex);
        return Utils.format("%s " + BULLET + " %s",
            section.conceptName, Utils.format(section.date, MONTH_DAY));
    }

    @Override public Object getChild(int groupIndex, int childIndex) {
        Obs obs = mSectionObservations.get(groupIndex).get(childIndex);
        return Utils.format("%s " + EM_DASH + " %s",
            Utils.format(obs.time, HOUR_MINUTE), obs.valueName);
    }

    @Override public int getGroupCount() {
        return mSections.size();
    }

    @Override public int getChildrenCount(int groupIndex) {
        return mSectionObservations.get(groupIndex).size();
    }

    @Override public long getGroupId(int groupIndex) {
        return groupIndex;
    }

    @Override public long getChildId(int groupIndex, int childIndex) {
        return childIndex;
    }

    @Override public boolean isChildSelectable(int groupIndex, int childIndex) {
        return true;
    }

    @Override public boolean hasStableIds() {
        return false;
    }
}
