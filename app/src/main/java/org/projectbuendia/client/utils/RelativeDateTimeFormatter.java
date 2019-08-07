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
import org.joda.time.Days;
import org.joda.time.Duration;
import org.joda.time.LocalDate;

/**
 * An object that pretty-prints JODA {@link LocalDate}s using relative phrases
 * such as "4 days ago" or "yesterday".
 */
public class RelativeDateTimeFormatter {
    public RelativeDateTimeFormatter() {
    }

    public String format(LocalDate date) {
        return format(date, LocalDate.now());
    }

    /** Formats a representation of {@code date} relative to {@code anchor}. */
    public String format(LocalDate date, LocalDate anchor) {
        if (date.isAfter(anchor)) {
            return "in the future"; // TODO/i18n
        }
        int daysAgo = Days.daysBetween(date, anchor).getDays();
        return daysAgo > 1 ? daysAgo + " days ago" : // TODO/i18n
            daysAgo == 1 ? "yesterday" : "today"; // TODO/i18n
    }

    public String format(DateTime dateTime) {
        return format(dateTime, DateTime.now());
    }

    /** Formats a representation of {@code dateTime} relative to {@code anchor}. */
    public String format(DateTime dateTime, DateTime anchor) {
        if (dateTime.isAfter(anchor)) {
            return "in the future"; // TODO/i18n
        }

        long millis = new Duration(dateTime, anchor).getMillis();
        long daysAgo = millis / Utils.DAY;
        long hoursAgo = millis / Utils.HOUR;
        long minutesAgo = millis / Utils.MINUTE;

        return daysAgo > 1 ? daysAgo + " days ago" : // TODO/i18n
            hoursAgo > 1 ? hoursAgo + " hours ago" : // TODO/i18n
                minutesAgo > 1 ? minutesAgo + " min ago" : // TODO/i18n
                    "right now"; // TODO/i18n
    }
}
