package org.msf.records.filter.matchers;

import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * A matcher that acts as a container for multiple filters or filter groups.
 */
public class MatchingFilterGroup<T> implements MatchingFilter<T> {
    private final FilterType mFilterType;
    private final ImmutableList<MatchingFilter> mFilters;

    /**
     * Specifies whether filters in this group will be AND'd or OR'd in the
     * selection.
     */
    public enum FilterType {
        OR,
        AND
    }

    public MatchingFilterGroup(FilterType filterType, MatchingFilter... filters) {
        mFilters = ImmutableList.copyOf(filters);
        mFilterType = filterType;
    }

    @Override
    public boolean matches(T object, CharSequence constraint) {
        if (mFilterType == FilterType.OR) {
            return matchesOr(object, constraint);
        } else {
            return matchesAnd(object, constraint);
        }
    }

    private boolean matchesOr(T object, CharSequence constraint) {
        for (MatchingFilter filter : mFilters) {
            if (filter.matches(object, constraint)) {
                return true;
            }
        }

        return false;
    }

    private boolean matchesAnd(T object, CharSequence constraint) {
        for (MatchingFilter filter : mFilters) {
            if (!filter.matches(object, constraint)) {
                return false;
            }
        }

        return true;
    }
}
