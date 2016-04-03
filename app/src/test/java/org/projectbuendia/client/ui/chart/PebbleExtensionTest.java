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

package org.projectbuendia.client.ui.chart;

import com.mitchellbosecke.pebble.extension.Filter;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/** Test cases for {@link PebbleExtension}. */
public class PebbleExtensionTest {

    private Instant mNowUTC;
    private Map<String, Object> mArgs;
    private static final String TIME_FORMAT = "HH:mm";

    @Test
    public void testFormatTimeFilter_mustReturnTimeFormattedInDefaultTimeZone() throws Exception {
        mArgs.put("pattern", TIME_FORMAT);
        Filter filter = new  PebbleExtension.FormatTimeFilter();

        String result = (String) filter.apply(mNowUTC, mArgs);

        assertEquals(result, "12:00");
    }

    @Before
    public void setUp() {
        mArgs = new HashMap<>();
        mNowUTC = new Instant(
                new DateTime("2000-01-01T12:00", DateTimeZone.getDefault()));
    }
}
