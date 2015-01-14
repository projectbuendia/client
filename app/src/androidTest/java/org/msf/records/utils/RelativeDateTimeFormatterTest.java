package org.msf.records.utils;

import android.test.InstrumentationTestCase;

import org.joda.time.DateTime;

/**
 * Test cases for {@link RelativeDateTimeFormatter}.
 */
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
}
