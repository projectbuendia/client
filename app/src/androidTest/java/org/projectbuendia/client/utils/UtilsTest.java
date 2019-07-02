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
import org.projectbuendia.client.ui.dialogs.ObsDetailDialogFragment.Section;
import org.projectbuendia.client.ui.dialogs.ObsDetailDialogFragment.SectionComparator;

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
        Arrays.sort(sorted, Utils.alphanumericComparator);
        Joiner joiner = Joiner.on("/");
        String[] expected = {
            "a1", "a2", "a2a", "a02b", "a2b", "a11", "a11a", "b1",
            "b7465829459273654782633", "b7465829459273654782634"};
        assertEquals(joiner.join(expected), joiner.join(sorted));
    }

    @Test public void testSectionComparator() {
        List<String> conceptUuids = Arrays.asList("5", "1", "3");

        List<Section> sections = Arrays.asList(
            new Section(new LocalDate(2019, 1, 7), "1", "a"),
            new Section(new LocalDate(2019, 1, 4), "1", "a"),
            new Section(new LocalDate(2019, 1, 8), "2", "b"),
            new Section(new LocalDate(2019, 1, 3), "2", "b"),
            new Section(new LocalDate(2019, 1, 4), "3", "c"),
            new Section(new LocalDate(2019, 1, 4), "4", "d"),
            new Section(new LocalDate(2019, 1, 4), "5", "e")
        );

        Collections.sort(sections, new SectionComparator(conceptUuids));

        assertEquals("2019-01-03/2/b", toString(sections.get(0)));
        assertEquals("2019-01-04/5/e", toString(sections.get(1)));
        assertEquals("2019-01-04/1/a", toString(sections.get(2)));
        assertEquals("2019-01-04/3/c", toString(sections.get(3)));
        assertEquals("2019-01-04/4/d", toString(sections.get(4)));
        assertEquals("2019-01-07/1/a", toString(sections.get(5)));
        assertEquals("2019-01-08/2/b", toString(sections.get(6)));
    }

    private String toString(Section section) {
        return section.date.toString() + "/" + section.conceptUuid + "/" + section.conceptName;
    }
}
