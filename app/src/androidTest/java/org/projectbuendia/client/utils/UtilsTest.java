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

import org.junit.Assert;
import org.junit.Test;
import org.projectbuendia.client.models.ObsRow;

import java.util.ArrayList;
import java.util.Arrays;

public class UtilsTest {
    /** Tests Utils.alphanumericComparator. */
    @Test
    public void testAlphanumericComparator() {
        String[] elements = {
            "b1", "a11a", "a11", "a2", "a2b", "a02b", "a2a", "a1",
            "b7465829459273654782634", "b7465829459273654782633"};
        String[] sorted = elements.clone();
        Arrays.sort(sorted, Utils.alphanumericComparator);
        Joiner joiner = Joiner.on("/");
        String[] expected = {
            "a1", "a2", "a2a", "a02b", "a2b", "a11", "a11a", "b1",
            "b7465829459273654782633", "b7465829459273654782634"};
        Assert.assertEquals(joiner.join(expected), joiner.join(sorted));
    }

    @Test
    public void testSortObsRows() {
        String conceptUuid1 = "1";
        String conceptUuid2 = "2";

        ObsRow obsRow0 = new ObsRow("", 0, "", "", "", "");
        ObsRow obsRow1 = new ObsRow("", 0, "", conceptUuid1, "", "");
        ObsRow obsRow2 = new ObsRow("", 0, "", conceptUuid2, "", "");

        ArrayList<ObsRow> obsRows = new ArrayList<>();
        obsRows.add(obsRow0);
        obsRows.add(obsRow2);
        obsRows.add(obsRow1);

        ArrayList<String> conceptUuids = new ArrayList<>();
        conceptUuids.add(conceptUuid1);
        conceptUuids.add(conceptUuid2);

        Utils.sortObsRows(obsRows, conceptUuids);

        Assert.assertEquals(conceptUuid1, obsRows.get(0).conceptUuid);
        Assert.assertEquals(conceptUuid2, obsRows.get(1).conceptUuid);
        Assert.assertEquals("", obsRows.get(2).conceptUuid);
    }
}
