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

import android.support.test.runner.AndroidJUnit4;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.projectbuendia.client.utils.RelativeDateTimeFormatter;

import androidx.test.filters.SmallTest;

import static junit.framework.TestCase.assertEquals;

/** Test cases for {@link RelativeDateTimeFormatter}. */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class RelativeDateTimeFormatterTest   {

    private RelativeDateTimeFormatter mFormatter;
    private DateTime mNow;
    private LocalDate mToday;

    @Test
    public void testFormat_rightNow() throws Exception {
        assertEquals("right now", mFormatter.format(mNow, mNow));
    }

    @Test
    public void testFormat_inTheFuture() throws Exception {
        assertEquals("in the future", mFormatter.format(mNow.plusDays(1), mNow));
    }

    @Test
    public void testFormat_17minutesAgo() throws Exception {
        assertEquals("17 min ago", mFormatter.format(mNow.minusMinutes(17), mNow));
    }


    @Test
    public void testFormat_60minutesAgo() throws Exception {
        assertEquals("60 min ago", mFormatter.format(mNow.minusHours(1), mNow));
    }

    @Test
    public void testFormat_yesterday() throws Exception {
        assertEquals("yesterday", mFormatter.format(mToday.minusDays(1), mToday));
    }

    @Test
    public void testFormat_daysAgo() throws Exception {
        assertEquals("2 days ago", mFormatter.format(mNow.minusDays(2), mNow));
    }

    // Regression test for https://github.com/projectbuendia/client/issues/389
    @Test public void testFormat_moreThanAMonthAgo() throws Exception {
        assertEquals("99 days ago", mFormatter.format(mNow.minusDays(99), mNow));
    }

    @Test
    public void testFormatLocalDate_today() {
        assertEquals("today", mFormatter.format(mToday, mToday));
    }

    @Before
    public void setup() {
        mFormatter = new RelativeDateTimeFormatter();
        mNow = DateTime.parse("2000-01-01T12:00Z");
        mToday = LocalDate.parse("2000-01-01");
    }
}
