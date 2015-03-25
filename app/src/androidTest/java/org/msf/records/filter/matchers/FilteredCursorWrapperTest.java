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

package org.msf.records.filter.matchers;

import android.test.InstrumentationTestCase;

import com.google.common.collect.Iterators;

import org.msf.records.FakeTypedCursor;
import org.msf.records.data.app.TypedCursor;

import java.util.Iterator;

/**
 * Tests for {@link FilteredCursorWrapper}.
 */
public class FilteredCursorWrapperTest extends InstrumentationTestCase {
    private static final MatchingFilter<String> SUBSTRING_FILTER = new MatchingFilter<String>() {
        @Override
        public boolean matches(String object, CharSequence constraint) {
            return object.contains(constraint);
        }
    };

    private static final String[] SAMPLE_DATA = new String[] {
            "apple", "orange", "pear", "grapefruit"
    };

    private static final TypedCursor<String> SAMPLE_CURSOR = new FakeTypedCursor<>(SAMPLE_DATA);

    /** Tests that getCount() returns the expected number of matches. */
    public void testGetCount_returnsNumberOfMatches() {
        assertEquals(2, getWrapperForConstraint("pe").getCount());
        assertEquals(1, getWrapperForConstraint("pea").getCount());
        assertEquals(0, getWrapperForConstraint("peak").getCount());
    }

    /** Tests that get() returns any and all matched entries. */
    public void testGet_returnsMatchedEntries() {
        FilteredCursorWrapper<String> wrapper = getWrapperForConstraint("pe");
        assertEquals("pear", wrapper.get(0));
        assertEquals("grapefruit", wrapper.get(1));
    }

    /** Tests that the iterator returned by iterator() only iterates on matched entries. */
    public void testIterator_returnsIteratorForMatchedEntries() {
        Iterator<String> iterator = getWrapperForConstraint("pe").iterator();
        String[] iteratorValues = Iterators.toArray(iterator, String.class);
        assertEquals(2, iteratorValues.length);
        assertEquals("pear", iteratorValues[0]);
        assertEquals("grapefruit", iteratorValues[1]);
    }

    private FilteredCursorWrapper<String> getWrapperForConstraint(String constraint) {
        return new FilteredCursorWrapper<>(SAMPLE_CURSOR, SUBSTRING_FILTER, constraint);
    }
}
