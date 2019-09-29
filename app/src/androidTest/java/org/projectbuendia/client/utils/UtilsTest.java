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

package org.projectbuendia.client.utils;

import com.google.common.base.Joiner;

import org.joda.time.LocalDate;
import org.junit.Test;
import org.projectbuendia.client.ui.dialogs.ObsDetailDialogFragment;
import org.projectbuendia.client.ui.dialogs.ObsDetailDialogFragment.Group;
import org.projectbuendia.client.ui.dialogs.ObsDetailDialogFragment.GroupComparator;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class UtilsTest {
    @Test public void testAlphanumericComparator() {
        String[] elements = {
            "b1", "a11a", "a11", "a2", "a2b", "a02b", "a2a", "a1",
            "b7465829459273654782634", "b7465829459273654782633"};
        String[] sorted = elements.clone();
        Arrays.sort(sorted, Utils.ALPHANUMERIC_COMPARATOR);
        Joiner joiner = Joiner.on("/");
        String[] expected = {
            "a1", "a2", "a2a", "a02b", "a2b", "a11", "a11a", "b1",
            "b7465829459273654782633", "b7465829459273654782634"};
        assertEquals(joiner.join(expected), joiner.join(sorted));
    }

    @Test public void testSectionComparator() {
        String[] conceptUuids = {"5", "1", "3"};

        List<ObsDetailDialogFragment.Group> groups = Arrays.asList(
            new ObsDetailDialogFragment.Group(new LocalDate(2019, 1, 7), "1"),
            new ObsDetailDialogFragment.Group(new LocalDate(2019, 1, 4), "1"),
            new ObsDetailDialogFragment.Group(new LocalDate(2019, 1, 8), "2"),
            new ObsDetailDialogFragment.Group(new LocalDate(2019, 1, 3), "2"),
            new Group(new LocalDate(2019, 1, 4), "3"),
            new Group(new LocalDate(2019, 1, 4), "4"),
            new Group(new LocalDate(2019, 1, 4), "5")
        );

        Collections.sort(groups, new GroupComparator(conceptUuids));

        assertEquals("2019-01-03/2", toString(groups.get(0)));
        assertEquals("2019-01-04/5", toString(groups.get(1)));
        assertEquals("2019-01-04/1", toString(groups.get(2)));
        assertEquals("2019-01-04/3", toString(groups.get(3)));
        assertEquals("2019-01-04/4", toString(groups.get(4)));
        assertEquals("2019-01-07/1", toString(groups.get(5)));
        assertEquals("2019-01-08/2", toString(groups.get(6)));
    }

    private String toString(ObsDetailDialogFragment.Group group) {
        return group.date.toString() + "/" + group.conceptUuid;
    }
}
