package org.msf.records.utils.date;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.Period;

/**
 * An object that pretty-prints Joda {@link LocalDate}s using relative phrases such as "4 days ago"
 * or "yesterday".
 */
public class RelativeDateTimeFormatter {

    public enum Casing {

        LOWER_CASE(
                "right now",
                "in the future",
                "today",
                "yesterday",
                "%1$s days ago"),
        SENTENCE_CASE(
                "Right now",
                "In the future",
                "Today",
                "Yesterday",
                "%1$s days ago"),
        TITLE_CASE(
                "Right Now",
                "In the Future",
                "Today",
                "Yesterday",
                "%1$s Days Ago");

        public final String rightNow;
        public final String inTheFuture;
        public final String today;
        public final String yesterday;
        public final String daysAgo;

        Casing(
                String rightNow, String inTheFuture, String today, String yesterday,
                String daysAgo) {
            this.rightNow = rightNow;
            this.inTheFuture = inTheFuture;
            this.today = today;
            this.yesterday = yesterday;
            this.daysAgo = daysAgo;
        }
    }

    public static class Builder {

        private Casing mCasing;

        public Builder withCasing(Casing casing) {
            mCasing = casing;
            return this;
        }

        public RelativeDateTimeFormatter build() {
            return new RelativeDateTimeFormatter(mCasing);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private final Casing mCasing;

    private RelativeDateTimeFormatter(Casing casing) {
        mCasing = casing;
    }

    public String format(LocalDate now, LocalDate other) {
        // Ensure DateTimes don't match so that we exclude the 'right now' case.
        return format(now.toDateTimeAtStartOfDay().plusMinutes(1), other.toDateTimeAtStartOfDay());
    }

    /**
     * Returns a formatted representation of {@code other}, relative to {@code now}.
     */
    public String format(DateTime now, DateTime other) {
        if (other.isEqual(now)) {
            return mCasing.rightNow;
        }

        if (other.isAfter(now)) {
            return mCasing.inTheFuture;
        }

        int daysAgo = new Period(other, now).toStandardDays().getDays();
        if (daysAgo == 0) {
            return mCasing.today;
        } else if (daysAgo == 1) {
            return mCasing.yesterday;
        } else {
            return String.format(mCasing.daysAgo, daysAgo);
        }
    }
}
