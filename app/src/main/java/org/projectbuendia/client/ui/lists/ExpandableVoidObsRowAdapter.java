package org.projectbuendia.client.ui.lists;

        import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import org.projectbuendia.client.R;
import org.projectbuendia.client.models.ObsRow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ExpandableVoidObsRowAdapter extends BaseExpandableListAdapter {

    private Context _context;
    private List<String> _listDataHeader;
    private HashMap<String, ArrayList<ObsRow>> _listDataChild;
    public  ArrayList<String> mCheckedItems = new ArrayList<>();

    public ExpandableVoidObsRowAdapter(Context context, List<String> listDataHeader,
                                   HashMap<String, ArrayList<ObsRow>> listChildData) {
        this._context = context;
        this._listDataHeader = listDataHeader;
        this._listDataChild = listChildData;
    }


    @Override
    public View getChildView(int groupPosition, final int childPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {

        final ObsRow childRow = (ObsRow) getChild(groupPosition, childPosition);

        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) this._context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.item_void_observation, null);
        }

        TextView txtListChild = (TextView) convertView
                .findViewById(R.id.tvVoidValue);

        txtListChild.setText(childRow.time + " " + childRow.valueName);

        CheckBox cbVoid = (CheckBox) convertView.findViewById(R.id.cbVoid);
        cbVoid.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                mCheckedItems.add(childRow.uuid);
            } else {
                mCheckedItems.remove(childRow.uuid);
            }
        });


        return convertView;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded,
                             View convertView, ViewGroup parent) {
        String headerTitle = (String) getGroup(groupPosition);
        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) this._context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.obs_section, null);
        }

        TextView lblListHeader = (TextView) convertView
                .findViewById(R.id.section_heading);
        lblListHeader.setTypeface(null, Typeface.BOLD);
        lblListHeader.setText(headerTitle);

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return this._listDataChild.get(this._listDataHeader.get(groupPosition))
                .size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return this._listDataHeader.get(groupPosition);
    }

    @Override
    public int getGroupCount() {
        return this._listDataHeader.size();
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public Object getChild(int groupPosition, int childPosititon) {
        return this._listDataChild.get(this._listDataHeader.get(groupPosition))
                .get(childPosititon);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

}

