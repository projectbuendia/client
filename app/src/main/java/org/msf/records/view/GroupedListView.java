package org.msf.records.view;

import android.content.Context;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;

/**
 * GroupedListView is a list view split up by category, where each category has a heading.
 *
 * It acts like an expandable list view that:
 * (A) Always shows groups as expanded
 * (B) Has no expand/collapse chevron
 * (C) Disables group collapse
 */
public class GroupedListView extends ExpandableListView {
    public GroupedListView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Disable expand/collapse chevron.
        setGroupIndicator(null);

        // Ignore clicks on groups by default.
        setOnGroupClickListener(new OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView parent, View v,
                                        int groupPosition, long id) {
                return true;
            }
        });
    }

    // Everything is expanded all the time.
    @Override
    public boolean isGroupExpanded(int groupPosition) {
        return true;
    }

    // Expand all groups when an adapter is set or when any data changes (which may involve adding
    // a group).
    @Override
    public void setAdapter(final ExpandableListAdapter adapter) {
        super.setAdapter(adapter);
        expandAllGroups(adapter);
        adapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                expandAllGroups(adapter);
            }
        });
    }

    private void expandAllGroups(ExpandableListAdapter adapter) {
        for (int i = 0; i < adapter.getGroupCount(); i++) {
            expandGroup(i);
        }
    }
}
