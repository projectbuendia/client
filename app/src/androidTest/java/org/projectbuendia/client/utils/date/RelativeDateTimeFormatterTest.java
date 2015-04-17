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

package org.projectbuendia.client.utils.date;

import android.test.InstrumentationTestCase;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;

/** Test cases for {@link RelativeDateTimeFormatter}. */
public class RelativeDateTimeFormatterTest extends InstrumentationTestCase {

    private RelativeDateTimeFormatter mFormatter;
    private DateTime mNow;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mFormatter = RelativeDateTimeFormatter.builder()
                .withCasing(RelativeDateTimeFormatter.Casing.LOWER_CASE)
                .build();
        mNow = DateTime.parse("2000-01-01T12:00Z");
    }

    public void testFormat_rightNow() throws Exception {
        assertEquals("right now", mFormatter.format(mNow, mNow));
    }

    public void testFormat_inTheFuture() throws Exception {
        assertEquals("in the future", mFormatter.format(mNow, mNow.plusDays(1)));
    }

    public void testFormat_today() throws Exception {
        assertEquals("today", mFormatter.format(mNow, mNow.minusHours(1)));
    }

    public void testFormat_yesterday() throws Exception {
        assertEquals("yesterday", mFormatter.format(mNow, mNow.minusDays(1)));
    }

    public void testFormat_daysAgo() throws Exception {
        assertEquals("2 days ago", mFormatter.format(mNow, mNow.minusDays(2)));
    }

    /** Tests that format produces 'today' for identical {@link LocalDate} objects. */
    public void testFormatLocalDate_today() {
        assertEquals("today", mFormatter.format(mNow.toLocalDate(), mNow.toLocalDate()));
    }
}
