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

package org.projectbuendia.client.filter.matchers;

import com.google.common.collect.ImmutableList;

/** A matcher that acts as a container for multiple filters or filter groups. */
public class MatchingFilterGroup<T> implements MatchingFilter<T> {
    private final FilterType mFilterType;
    private final ImmutableList<MatchingFilter> mFilters;

    /** Specifies whether filters in this group will be AND'd or OR'd in the selection. */
    public enum FilterType {
        OR,
        AND
    }

    public MatchingFilterGroup(FilterType filterType, MatchingFilter... filters) {
        mFilters = ImmutableList.copyOf(filters);
        mFilterType = filterType;
    }

    @Override public boolean matches(T object, CharSequence constraint) {
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
