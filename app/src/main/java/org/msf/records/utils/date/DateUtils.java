package org.msf.records.utils.date;

import android.support.annotation.Nullable;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * Static utility functions for dealing with dates and timestamps.
 */
public class DateUtils {
    static final DateTimeFormatter SHORT_DATE_FORMATTER = DateTimeFormat.forPattern("d MMM");
    static final DateTimeFormatter MEDIUM_DATE_FORMATTER = DateTimeFormat.forPattern("d MMM yyyy");
    static final DateTimeFormatter MEDIUM_DATETIME_FORMATTER = DateTimeFormat.mediumDateTime();

    /** Converts a LocalDate or null safely to a yyyy-mm-dd String or null. */
    public static String localDateToString(LocalDate date) {
        return date == null ? null : date.toString();
    }

    /** Converts a yyyy-mm-dd String or null safely to a LocalDate or null. */
    public static LocalDate stringToLocalDate(String string) {
        try {
            return string == null ? null : LocalDate.parse(string);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Converts a nullable {@link DateTime} to a nullable String with day and month only. */
    public static String dateTimeToShortDateString(@Nullable DateTime dateTime) {
        return dateTime == null ? null : SHORT_DATE_FORMATTER.print(dateTime);
    }

    /** Converts a nullable {@link DateTime} to a nullable String with day, month, and year. */
    public static String dateTimeToMediumDateString(@Nullable DateTime dateTime) {
        return dateTime == null ? null : MEDIUM_DATE_FORMATTER.print(dateTime);
    }

    /**
     * Converts a nullable {@link DateTime} to a nullable String with full date and time, but no
     * time zone.
     */
    public static String dateTimeToMediumDateTimeString(@Nullable DateTime dateTime) {
        return dateTime == null ? null : MEDIUM_DATETIME_FORMATTER.print(dateTime);
    }

    /** Converts a birthdate to a string describing age in months or years. */
    public static String birthdateToAge(LocalDate birthdate) {
        Period age = new Period(birthdate, LocalDate.now());
        if (age.getYears() >= 2) {
            return "" + age.getYears() + " y";
        } else {
            return "" + (age.getYears() * 12 + age.getMonths()) + " mo";
        }
    }
}
