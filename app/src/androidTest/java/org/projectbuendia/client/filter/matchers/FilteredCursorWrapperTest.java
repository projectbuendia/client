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

import android.support.test.runner.AndroidJUnit4;

import com.google.common.collect.Iterators;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.projectbuendia.client.FakeTypedCursor;
import org.projectbuendia.models.TypedCursor;

import java.util.Iterator;

import androidx.test.filters.SmallTest;

import static junit.framework.TestCase.assertEquals;

/** Tests for {@link FilteredCursor}. */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class FilteredCursorWrapperTest {
    private static final MatchingFilter<String> SUBSTRING_FILTER = String::contains;

    private static final String[] SAMPLE_DATA = new String[] {
        "apple", "orange", "pear", "grapefruit"
    };

    private static final TypedCursor<String> SAMPLE_CURSOR = new FakeTypedCursor<>(SAMPLE_DATA);

    /** Tests that getCount() returns the expected number of matches. */
    @Test
    public void testGetCount_returnsNumberOfMatches() {
        assertEquals(2, filterWithConstraint("pe").getCount());
        assertEquals(1, filterWithConstraint("pea").getCount());
        assertEquals(0, filterWithConstraint("peak").getCount());
    }

    private FilteredCursor<String> filterWithConstraint(String constraint) {
        return new FilteredCursor<>(SAMPLE_CURSOR, SUBSTRING_FILTER, constraint);
    }

    /** Tests that get() returns any and all matched entries. */
    @Test
    public void testGet_returnsMatchedEntries() {
        FilteredCursor<String> cursor = filterWithConstraint("pe");
        assertEquals("pear", cursor.get(0));
        assertEquals("grapefruit", cursor.get(1));
    }

    /** Tests that the iterator returned by iterator() only iterates on matched entries. */
    @Test
    public void testIterator_returnsIteratorForMatchedEntries() {
        Iterator<String> iterator = filterWithConstraint("pe").iterator();
        String[] iteratorValues = Iterators.toArray(iterator, String.class);
        assertEquals(2, iteratorValues.length);
        assertEquals("pear", iteratorValues[0]);
        assertEquals("grapefruit", iteratorValues[1]);
    }
}
