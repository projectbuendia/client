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

package org.projectbuendia.client.widgets;

import android.content.Context;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;

import org.projectbuendia.client.utils.Logger;

/**
 * GroupedListView is a list view split up by category, where each category has a heading.
 * <p/>
 * <p>It acts like an expandable list view that:
 * <ul>
 * <li>Always shows groups as expanded
 * <li>Has no expand/collapse chevron
 * <li>Disables group collapse
 * </ul>
 */
public class GroupedListView extends ExpandableListView {
    private static final Logger LOG = Logger.create();

    /**
     * Instantiates a {@link GroupedListView}.
     * {@see ExpandableListView(Context, AttributeSet)}
     */
    public GroupedListView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Disable expand/collapse chevron.
        setGroupIndicator(null);

        // Ignore clicks on groups by default.
        setOnGroupClickListener(new OnGroupClickListener() {
            @Override public boolean onGroupClick(ExpandableListView parent, View v,
                                        int groupPosition, long id) {
                return true;
            }
        });
    }

    // Everything is expanded all the time.
    @Override public boolean isGroupExpanded(int groupPosition) {
        return true;
    }

    // Expand all groups when an adapter is set or when any data changes (which may involve adding
    // a group).
    @Override public void setAdapter(final ExpandableListAdapter adapter) {
        super.setAdapter(adapter);
        expandAllGroups(adapter);
        adapter.registerDataSetObserver(new DataSetObserver() {
            @Override public void onChanged() {
                expandAllGroups(adapter);
            }
        });
    }

    private void expandAllGroups(ExpandableListAdapter adapter) {
        LOG.i("expandAllGroups(%s) with group count = %d", adapter, adapter.getGroupCount());
        for (int i = 0; i < adapter.getGroupCount(); i++) {
            LOG.i("expandGroup(%d)", i);
            expandGroup(i);
        }
    }
}
