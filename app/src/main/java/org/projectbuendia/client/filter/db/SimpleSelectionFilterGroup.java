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

package org.projectbuendia.client.filter.db;

import com.google.common.collect.ImmutableList;

import org.projectbuendia.client.App;
import org.projectbuendia.client.R;
import org.projectbuendia.models.Model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A database filter that acts as a container for multiple filters or filter groups, with
 * an optional name used for string representations.
 */
public final class SimpleSelectionFilterGroup<T extends Model>
    extends SimpleSelectionFilter<T> {

    private final FilterType mFilterType;
    private final ImmutableList<SimpleSelectionFilter> mFilters;
    private String mName = App.str(R.string.filter_group_default_name);

    /**
     * Specifies whether filters in this group will be AND'd or OR'd in the
     * selection.
     */
    public enum FilterType {
        OR,
        AND
    }

    /** Assume AND by default. */
    public SimpleSelectionFilterGroup(SimpleSelectionFilter... filters) {
        this(FilterType.AND, filters);
    }

    public SimpleSelectionFilterGroup(FilterType filterType, SimpleSelectionFilter... filters) {
        mFilters = ImmutableList.copyOf(filters);
        mFilterType = filterType;
    }

    public SimpleSelectionFilterGroup(FilterType filterType, List<SimpleSelectionFilter> filters) {
        mFilters = ImmutableList.copyOf(filters);
        mFilterType = filterType;
    }

    public List<SimpleSelectionFilter> getFilters() {
        return mFilters;
    }

    /**
     * Dynamically build the selection string by combining the selection strings
     * of the filters in this group.
     */
    @Override public String getSelectionString() {
        StringBuilder sb = new StringBuilder();

        String prefix = "";
        sb.append(" (");
        for (SimpleSelectionFilter filter : mFilters) {
            // Ignore empty or null selection strings, which could
            // otherwise result in rogue AND's/OR's.
            String selectionString = filter.getSelectionString();
            if (selectionString == null || selectionString.isEmpty()) continue;

            sb.append(prefix);
            sb.append(" ");
            sb.append(selectionString);
            sb.append(" ");
            prefix = mFilterType.toString();
        }
        sb.append(") ");
        return sb.toString();
    }

    /**
     * Merges all of the selection arguments from filters in this group into a
     * single array.
     * @param constraint the constraint passed into the top-level filter
     */
    @Override public String[] getSelectionArgs(CharSequence constraint) {
        List<String> allArgs = new ArrayList<>();
        for (SimpleSelectionFilter filter : mFilters) {
            Collections.addAll(allArgs, filter.getSelectionArgs(constraint));
        }

        String[] allArgsArray = new String[allArgs.size()];
        allArgs.toArray(allArgsArray);

        return allArgsArray;
    }

    @Override public String getDescription() {
        return mName;
    }

    /**
     * Sets the displayed name for this filter group.
     * @param name the filter name
     * @return this, with the name set
     */
    public SimpleSelectionFilterGroup setName(String name) {
        mName = name;
        return this;
    }

    @Override public String toString() {
        return mName;
    }
}
