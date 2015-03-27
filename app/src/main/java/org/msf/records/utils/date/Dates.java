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

package org.msf.records.utils.date;

import android.support.annotation.Nullable;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/** Static utility functions for dealing with dates and timestamps. */
public class Dates {
    static final DateTimeFormatter SHORT_DATE_FORMATTER = DateTimeFormat.forPattern("d MMM");
    static final DateTimeFormatter MEDIUM_DATE_FORMATTER = DateTimeFormat.forPattern("d MMM yyyy");
    static final DateTimeFormatter MEDIUM_DATETIME_FORMATTER = DateTimeFormat.mediumDateTime();

    /** Converts a LocalDate to a yyyy-mm-dd String. */
    @Nullable
    public static String toString(@Nullable LocalDate date) {
        return date == null ? null : date.toString();
    }

    /** Converts a yyyy-mm-dd String to a LocalDate. */
    @Nullable
    public static LocalDate toLocalDate(@Nullable String string) {
        try {
            return string == null ? null : LocalDate.parse(string);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Converts a nullable {@link LocalDate} to a nullable String with day and month only. */
    @Nullable
    public static String toShortString(@Nullable LocalDate localDate) {
        return localDate == null ? null : SHORT_DATE_FORMATTER.print(localDate);
    }

    /** Converts a nullable {@link LocalDate} to a nullable String with day, month, and year. */
    @Nullable
    public static String toMediumString(@Nullable LocalDate localDate) {
        return localDate == null ? null : MEDIUM_DATE_FORMATTER.print(localDate);
    }

    /**
     * Converts a nullable {@link DateTime} to a nullable String with full date and time, but no
     * time zone.
     */
    @Nullable
    public static String toMediumString(@Nullable DateTime dateTime) {
        return dateTime == null ? null : MEDIUM_DATETIME_FORMATTER.print(dateTime);
    }

    /** Converts a birthdate to a string describing age in months or years. */
    public static String birthdateToAge(LocalDate birthdate) {
        // TODO: Localization
        Period age = new Period(birthdate, LocalDate.now());
        if (age.getYears() >= 2) {
            return "" + age.getYears() + " y";
        } else {
            return "" + (age.getYears() * 12 + age.getMonths()) + " mo";
        }
    }

    /**
     * Describes a given date as a number of days since a starting date, where the starting date
     * itself is Day 1.  Returns a value <= 0 if the given date is null or in the future.
     */
    public static int dayNumberSince(@Nullable LocalDate startDate, @Nullable LocalDate date) {
        if (startDate == null || date == null) {
            return -1;
        }
        return Days.daysBetween(startDate, date).getDays() + 1;
    }
}
