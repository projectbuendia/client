package org.msf.records.filter;

import java.util.ArrayList;
import java.util.List;

/**
 * A FilterGroup is a filter that acts as a container for multiple filters or filter groups, with
 * an optional name used for string representations.
 */
public class FilterGroup implements SimpleSelectionFilter {
    private static final String DEFAULT_FILTER_NAME = "";

    private FilterType mFilterType;
    private List<SimpleSelectionFilter> mFilters;
    private String mName = DEFAULT_FILTER_NAME;

    /**
     * Specifies whether filters in this group will be AND'd or OR'd in the
     * selection.
     */
    public enum FilterType {
        OR,
        AND
    }

    // Assume AND by default.
    public FilterGroup(SimpleSelectionFilter... filters) {
        this(FilterType.AND, filters);
    }

    public FilterGroup(FilterType filterType, SimpleSelectionFilter... filters) {
        mFilters = new ArrayList<SimpleSelectionFilter>();
        for (SimpleSelectionFilter filter : filters) {
            mFilters.add(filter);
        }
        mFilterType = filterType;
    }

    public FilterGroup(FilterType filterType, List<SimpleSelectionFilter> filters) {
        mFilters = filters;
        mFilterType = filterType;
    }

    public List<SimpleSelectionFilter> getFilters() {
        return mFilters;
    }

    /**
     * Dynamically build the selection string by combining the selection strings
     * of the filters in this group.
     * @return
     */
    @Override
    public String getSelectionString() {
        StringBuilder sb = new StringBuilder();

        String prefix = "";
        sb.append(" (");
        for (SimpleSelectionFilter filter : mFilters) {
            // Ignore empty or null selection strings, which could
            // otherwise result in rogue AND's/OR's.
            String selectionString = filter.getSelectionString();
            if (selectionString == null || selectionString.isEmpty()) {
                continue;
            }

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
     * @return
     */
    @Override
    public String[] getSelectionArgs(CharSequence constraint) {
        List<String> allArgs = new ArrayList<String>();
        for (SimpleSelectionFilter filter : mFilters) {
            for (String arg : filter.getSelectionArgs(constraint)) {
                allArgs.add(arg);
            }
        }

        String[] allArgsArray = new String[allArgs.size()];
        allArgs.toArray(allArgsArray);

        return allArgsArray;
    }

    /**
     * Sets the displayed name for this filter group.
     * @param name the filter name
     * @return this, with the name set
     */
    public FilterGroup setName(String name) {
        mName = name;
        return this;
    }

    @Override
    public String toString() {
        return mName;
    }
}
