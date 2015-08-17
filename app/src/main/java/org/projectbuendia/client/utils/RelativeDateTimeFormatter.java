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

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.Period;

/**
 * An object that pretty-prints JODA {@link LocalDate}s using relative phrases such as "4 days ago"
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

    /** Returns a formatted representation of {@code other}, relative to {@code now}. */
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
