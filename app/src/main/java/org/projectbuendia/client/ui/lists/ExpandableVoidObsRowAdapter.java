package org.projectbuendia.client.ui.lists;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;

import org.projectbuendia.client.R;
import org.projectbuendia.client.models.ObsRow;
import org.projectbuendia.client.utils.ContextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ExpandableVoidObsRowAdapter extends BaseExpandableListAdapter {

    private ContextUtils u;
    private List<String> _listDataHeader;
    private HashMap<String, ArrayList<ObsRow>> _listDataChild;
    public  ArrayList<String> mCheckedItems = new ArrayList<>();

    public ExpandableVoidObsRowAdapter(Context context, List<String> listDataHeader,
                                   HashMap<String, ArrayList<ObsRow>> listChildData) {
        u = ContextUtils.from(context);
        this._listDataHeader = listDataHeader;
        this._listDataChild = listChildData;
    }

    @Override
    public View getChildView(int groupIndex, final int childIndex,
                             boolean isLastChild, View view, ViewGroup parent) {
        view = u.reuseOrInflate(view, R.layout.item_void_observation, parent);
        final ObsRow childRow = (ObsRow) getChild(groupIndex, childIndex);
        u.setText(R.id.tvVoidValue, childRow.time + " " + childRow.valueName);

        CheckBox cbVoid = u.findView(R.id.cbVoid);
        cbVoid.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                mCheckedItems.add(childRow.uuid);
            } else {
                mCheckedItems.remove(childRow.uuid);
            }
        });
        return view;
    }

    @Override
    public View getGroupView(int groupIndex, boolean isExpanded,
                             View view, ViewGroup parent) {
        view = u.reuseOrInflate(view, R.layout.obs_heading, parent);
        u.setText(R.id.heading_text, (String) getGroup(groupIndex));
        return view;
    }

    @Override
    public boolean isChildSelectable(int groupIndex, int childIndex) {
        return true;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public int getChildrenCount(int groupIndex) {
        return this._listDataChild.get(this._listDataHeader.get(groupIndex))
                .size();
    }

    @Override
    public Object getGroup(int groupIndex) {
        return this._listDataHeader.get(groupIndex);
    }

    @Override
    public int getGroupCount() {
        return this._listDataHeader.size();
    }

    @Override
    public long getGroupId(int groupIndex) {
        return groupIndex;
    }

    @Override
    public Object getChild(int groupIndex, int childPosititon) {
        return this._listDataChild.get(this._listDataHeader.get(groupIndex))
                .get(childPosititon);
    }

    @Override
    public long getChildId(int groupIndex, int childIndex) {
        return childIndex;
    }

}

