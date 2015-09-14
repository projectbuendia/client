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

import android.test.InstrumentationTestCase;

/** Tests for {@link MatchingFilterGroup}. */
public class MatchingFilterGroupTest extends InstrumentationTestCase {
    private static final MatchingFilter<String> PREFIX_FILTER = new MatchingFilter<String>() {
        @Override
        public boolean matches(String object, CharSequence constraint) {
            return object.startsWith(constraint.toString());
        }
    };

    private static final MatchingFilter<String> SUFFIX_FILTER = new MatchingFilter<String>() {
        @Override
        public boolean matches(String object, CharSequence constraint) {
            return object.endsWith(constraint.toString());
        }
    };

    private static final MatchingFilterGroup<String> AND_FILTER_GROUP =
        new MatchingFilterGroup<>(
            MatchingFilterGroup.FilterType.AND, PREFIX_FILTER, SUFFIX_FILTER);

    private static final MatchingFilterGroup<String> OR_FILTER_GROUP =
        new MatchingFilterGroup<>(
            MatchingFilterGroup.FilterType.OR, PREFIX_FILTER, SUFFIX_FILTER);

    /** Tests that OR filter groups match on any of their filters. */
    public void testMatches_multiFilterOr() {
        assertTrue(OR_FILTER_GROUP.matches("foobar", "foo"));
        assertTrue(OR_FILTER_GROUP.matches("foobar", "bar"));
        assertTrue(OR_FILTER_GROUP.matches("foobar", "foobar"));
    }

    /** Tests that AND filter groups must match on all of their filters. */
    public void testMatches_multiFilterAnd() {
        assertFalse(AND_FILTER_GROUP.matches("foobar", "foo"));
        assertFalse(AND_FILTER_GROUP.matches("foobar", "bar"));
        assertTrue(AND_FILTER_GROUP.matches("foobar", "foobar"));
    }

    /** Tests that OR filter groups with only one filter work like an ordinary filter. */
    public void testMatches_singleFilterOr() {
        MatchingFilterGroup<String> singleFilterGroup =
            new MatchingFilterGroup<>(MatchingFilterGroup.FilterType.OR, PREFIX_FILTER);
        assertTrue(singleFilterGroup.matches("foobar", "foo"));
        assertFalse(singleFilterGroup.matches("foobar", "bar"));
    }

    /** Tests that AND filter groups with only one filter work like an ordinary filter. */
    public void testMatches_singleFilterAnd() {
        MatchingFilterGroup<String> singleFilterGroup =
            new MatchingFilterGroup<>(MatchingFilterGroup.FilterType.AND, PREFIX_FILTER);
        assertTrue(singleFilterGroup.matches("foobar", "foo"));
        assertFalse(singleFilterGroup.matches("foobar", "bar"));
    }
}
