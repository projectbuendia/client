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

import android.app.Dialog;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;

import com.google.common.collect.Lists;
import com.mitchellbosecke.pebble.PebbleEngine;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;
import org.joda.time.Instant;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.joda.time.ReadableInstant;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.projectbuendia.client.App;
import org.projectbuendia.client.R;
import org.projectbuendia.client.net.Server;
import org.projectbuendia.client.ui.chart.PebbleExtension;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Utility methods. */
public class Utils {
    // Minimum and maximum representable Instant, DateTime, and LocalDate values.
    public static final Instant MIN_TIME = new Instant(Long.MIN_VALUE);
    public static final Instant MAX_TIME = new Instant(Long.MAX_VALUE);
    public static final DateTime MIN_DATETIME = new DateTime(MIN_TIME, DateTimeZone.UTC);
    public static final DateTime MAX_DATETIME = new DateTime(MAX_TIME, DateTimeZone.UTC);
    public static final LocalDate MIN_DATE = new LocalDate(0, 1, 1).year().withMinimumValue();
    public static final LocalDate MAX_DATE = new LocalDate(0, 12, 31).year().withMaximumValue();

    static PebbleEngine sEngine;

    /**
     * Compares two objects that may be null, Integer, Long, BigInteger, or String.
     * null sorts before everything; all integers sort before all strings; integers
     * sort according to numeric value; strings sort according to string value.
     */
    public static Comparator<Object> nullIntStrComparator = new Comparator<Object>() {
        @Override public int compare(Object a, Object b) {
            BigInteger intA = toBigInteger(a);
            BigInteger intB = toBigInteger(b);
            if (intA != null && intB != null) {
                return intA.compareTo(intB);
            }
            if (a instanceof String && b instanceof String) {
                return ((String) a).compareTo((String) b);
            }
            return (a == null ? 0 : intA != null ? 1 : 2)
                - (b == null ? 0 : intB != null ? 1 : 2);
        }
    };
    /**
     * Compares two lists, each of whose elements is a null, Integer, Long,
     * BigInteger, or String, lexicographically by element, just like Python.
     */
    public static Comparator<List<Object>> nullIntStrListComparator =
        new Comparator<List<Object>>() {
            @Override public int compare(List<Object> a, List<Object> b) {
                for (int i = 0; i < Math.min(a.size(), b.size()); i++) {
                    int result = nullIntStrComparator.compare(a.get(i), b.get(i));
                    if (result != 0) {
                        return result;
                    }
                }
                return a.size() - b.size();
            }
        };

    private static final DateTimeFormatter SHORT_DATE_FORMATTER =
        DateTimeFormat.forPattern("d MMM"); // TODO/i18n
    private static final DateTimeFormatter MEDIUM_DATE_FORMATTER =
        DateTimeFormat.forPattern("d MMM yyyy"); // TODO/i18n
    private static final DateTimeFormatter SHORT_DATETIME_FORMATTER =
        DateTimeFormat.forPattern("d MMM 'at' HH:mm"); // TODO/i18n
    private static final DateTimeFormatter MEDIUM_DATETIME_FORMATTER =
        DateTimeFormat.mediumDateTime();
    private static final DateTimeFormatter TIME_OF_DAY_FORMATTER =
        DateTimeFormat.forPattern("HH:mm"); // TODO/i18n
    // Note: Use of \L here assumes a string that is already NFC-normalized.
    private static final Pattern NUMBER_OR_WORD_PATTERN = Pattern.compile("([0-9]+)|\\p{L}+");
    private static final Pattern COMPRESSIBLE_UUID = Pattern.compile("^([0-9]+)A+$");

    /**
     * Compares two strings in a manner that sorts alphabetic parts in alphabetic
     * order and numeric parts in numeric order, while guaranteeing that:
     * - compare(s, t) == 0 if and only if s.equals(t).
     * - compare(s, s + t) < 0 for any strings s and t.
     * - compare(s + x, s + y) == Integer.compare(x, y) for all integers x, y
     * and strings s that do not end in a digit.
     * - compare(s + t, s + u) == compare(s, t) for all strings s and strings
     * t, u that consist entirely of Unicode letters.
     * For example, the strings ["b1", "a11a", "a11", "a2", "a2b", "a2a", "a1"]
     * have the sort order ["a1", "a2", "a2a", "a2b", "a11", "a11a", "b1"].
     */
    public static Comparator<String> alphanumericComparator = new Comparator<String>() {
        @Override public int compare(String a, String b) {
            String aNormalized = Normalizer.normalize(a == null ? "" : a, Normalizer.Form.NFC);
            String bNormalized = Normalizer.normalize(b == null ? "" : b, Normalizer.Form.NFC);
            List<Object> aParts = getParts(aNormalized);
            List<Object> bParts = getParts(bNormalized);
            // Add a separator to ensure that the tiebreakers added below are never
            // compared against the actual numeric or alphabetic parts.
            aParts.add(null);
            bParts.add(null);
            // Break ties between strings that yield the same parts (e.g. "a04b"
            // and "a4b") using the normalized original string as a tiebreaker.
            aParts.add(aNormalized);
            bParts.add(bNormalized);
            // Break ties between strings that become the same after normalization
            // using the non-normalized string as a further tiebreaker.
            aParts.add(a);
            bParts.add(b);
            return nullIntStrListComparator.compare(aParts, bParts);
        }

        /**
         * Breaks a string into a list of Integers (from sequences of ASCII digits)
         * and Strings (from sequences of letters).  Other characters are ignored.
         */
        private List<Object> getParts(String str) {
            Matcher matcher = NUMBER_OR_WORD_PATTERN.matcher(str);
            List<Object> parts = new ArrayList<>();
            while (matcher.find()) {
                try {
                    String part = matcher.group();
                    String intPart = matcher.group(1);
                    parts.add(intPart != null ? new BigInteger(intPart) : part);
                } catch (Exception e) {  // shouldn't happen, but just in case
                    parts.add(null);
                }
            }
            return parts;
        }
    };

    /** Performs a null-safe check for a null or empty string. */
    public static boolean isEmpty(@Nullable String str) {
        return str == null || str.isEmpty();
    }

    /** Converts empty strings to null. */
    public static String toNonemptyOrNull(@Nullable String str) {
        return isEmpty(str) ? null : str;
    }

    /** Parses a long integer value from a string, or returns null if parsing fails. */
    public static @Nullable Long toLongOrNull(@Nullable String str) {
        if (str == null) return null;
        try {
            return Long.parseLong(str);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Parses a double value from a string, or returns null if parsing fails. */
    public static @Nullable Double toDoubleOrNull(@Nullable String str) {
        if (str == null) return null;
        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Converts objects with integer type to BigInteger. */
    public static BigInteger toBigInteger(Object obj) {
        if (obj instanceof Integer) {
            return BigInteger.valueOf(((Integer) obj).longValue());
        }
        if (obj instanceof Long) {
            return BigInteger.valueOf(((Long) obj).longValue());
        }
        if (obj instanceof BigInteger) {
            return (BigInteger) obj;
        }
        return null;
    }

    /** URL-encodes a nullable string, catching the useless exception that never happens. */
    public static String urlEncode(@Nullable String s) {
        if (s == null) {
            return "";
        }
        try {
            // Oh Java, how you make the simplest operation a waste of millions of programmer-hours.
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError("UTF-8 should be supported in every JVM");
        }
    }

    /**
     * Returns the value for a system property. System properties need to start with "debug." and
     * can be set using "adb shell setprop $propertyName $value".
     */
    public static String getSystemProperty(String key) {
        // Accessing hidden APIs via reflection.
        try {
            final Class<?> systemProperties = Class.forName("android.os.SystemProperties");
            final Method get = systemProperties.getMethod("get", String.class, String.class);
            return (String) get.invoke(null, key, null);
        } catch (Exception e) {  // should never happen
            return null;
        }
    }

    /**
     * Logs a user action by sending a dummy request to the server.  (The server
     * logs can then be scanned later to produce analytics for the client app.)
     * @param action An identifier for the user action; should describe a user-
     *               initiated operation in the UI (e.g. "foo_button_pressed").
     * @param pairs  An even number of arguments providing key-value pairs of
     *               arbitrary data to record with the event.
     */
    public static void logUserAction(String action, String... pairs) {
        Server server = App.getInstance().getServer();
        if (server != null) {
            List<String> allPairs = Lists.newArrayList("action", action);
            allPairs.addAll(Arrays.asList(pairs));
            server.logToServer(allPairs);
        }
    }

    /**
     * Logs an event by sending a dummy request to the server.  (The server logs
     * can then be scanned later to produce analytics for the client app.)
     * @param event An identifier for an event that is not directly initiated by
     *              the user (e.g. "form_submission_failed").
     * @param pairs An even number of arguments providing key-value pairs of
     *              arbitrary data to record with the event.
     */
    public static void logEvent(String event, String... pairs) {
        Server server = App.getInstance().getServer();
        if (server != null) {
            List<String> allPairs = Lists.newArrayList("event", event);
            allPairs.addAll(Arrays.asList(pairs));
            server.logToServer(allPairs);
        }
    }

    /**
     * Returns the specified name or a sentinel representing an unknown name, if the name is
     * null.
     */
    public static @Nonnull String nameOrUnknown(@Nullable String name) {
        return valueOrDefault(name, App.getInstance().getString(R.string.unknown_name));
    }

    /** Returns a value if that value is not null, or a specified default value otherwise. */
    public static @Nonnull <T> T valueOrDefault(@Nullable T value, @Nonnull T defaultValue) {
        return value == null ? defaultValue : value;
    }

    /** Converts a list of Longs to an array of primitive longs. */
    public static long[] toArray(List<Long> items) {
        long[] array = new long[items.size()];
        int i = 0;
        for (Long item : items) {
            array[i++] = item;
        }
        return array;
    }

    /** Gets a nullable string value from a cursor. */
    public static @Nullable String getString(Cursor c, String columnName) {
        return getString(c, columnName, null);
    }

    /** Gets a string value from a cursor, returning a default value instead of null. */
    public static String getString(Cursor c, String columnName, String defaultValue) {
        int index = c.getColumnIndex(columnName);
        return c.isNull(index) ? defaultValue : c.getString(index);
    }

    /** Gets a LocalDate value from a cursor, possibly returning null. */
    public static LocalDate getLocalDate(Cursor c, String columnName) {
        int index = c.getColumnIndex(columnName);
        return c.isNull(index) ? null : new LocalDate(c.getString(index));
    }

    /** Gets a nullable long value (in millis) from a cursor as a DateTime. */
    public static DateTime getDateTime(Cursor c, String columnName) {
        Long millis = getLong(c, columnName);
        return millis == null ? null : new DateTime(millis);
    }

    /** Gets a nullable long value from a cursor. */
    public static Long getLong(Cursor c, String columnName) {
        return getLong(c, columnName, null);
    }

    /** Gets a long integer value from a cursor, returning a default value instead of null. */
    public static Long getLong(Cursor c, String columnName, @Nullable Long defaultValue) {
        int index = c.getColumnIndex(columnName);
        // The cast (Long) c.getLong(index) is necessary to work around the fact that
        // the Java compiler chooses type (long) for (boolean) ? (Long) : (long),
        // causing an NPE when defaultValue is null.  The correct superset of (Long) and
        // (long) is obviously (Long); the Java specification (15.25) is incorrect.
        return c.isNull(index) ? defaultValue : (Long) c.getLong(index);
    }

    /** Gets a nullable Long value from a Bundle.  Always use this instead of getLong() directly. */
    public static Long getLong(Bundle bundle, String key) {
        // getLong never returns null; we have to check explicitly.
        return bundle.containsKey(key) ? bundle.getLong(key) : null;
    }

    /** Gets a nullable DateTime value from a Bundle.  Always use this instead of getLong() directly. */
    public static DateTime getDateTime(Bundle bundle, String key) {
        // getLong never returns null; we have to check explicitly.
        return bundle.containsKey(key) ? new DateTime(bundle.getLong(key)) : null;
    }

    /** Converts a nullable LocalDate to a yyyy-mm-dd String. */
    public static @Nullable String toString(@Nullable LocalDate date) {
        return date == null ? null : date.toString();
    }

    /** Returns the greater of two DateTimes, treating null as the least value. */
    public static @Nullable DateTime max(DateTime a, DateTime b) {
        return a == null ? b : b == null ? a : a.isAfter(b) ? a : b;
    }

    /** Returns the lesser of two DateTimes, treating null as the greatest value. */
    public static @Nullable DateTime min(DateTime a, DateTime b) {
        return a == null ? b : b == null ? a : a.isBefore(b) ? a : b;
    }

    /** Converts a nullable yyyy-mm-dd String to a LocalDate. */
    public static @Nullable LocalDate toLocalDate(@Nullable String string) {
        try {
            return string == null ? null : LocalDate.parse(string);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Converts a nullable {@link LocalDate} to a nullable String with day and month only. */
    public static @Nullable String toShortString(@Nullable LocalDate localDate) {
        return localDate == null ? null : SHORT_DATE_FORMATTER.print(localDate);
    }

    /** Converts a nullable {@link LocalDate} to a nullable String with day, month, and year. */
    public static @Nullable String toMediumString(@Nullable LocalDate localDate) {
        return localDate == null ? null : MEDIUM_DATE_FORMATTER.print(localDate);
    }

    /** Converts a nullable {@link DateTime} to a nullable String with time, day, and month only. */
    public static @Nullable String toShortString(@Nullable DateTime dateTime) {
        return dateTime == null ? null : SHORT_DATETIME_FORMATTER.print(dateTime);
    }

    /**
     * Converts a nullable {@link DateTime} to a nullable String with just the time in hours and
     * minutes.
     */
    public static @Nullable String toTimeOfDayString(@Nullable DateTime dateTime) {
        return dateTime == null ? null : TIME_OF_DAY_FORMATTER.print(dateTime);
    }

    /**
     * Converts a nullable {@link DateTime} to a nullable String with full date and time, but no
     * time zone.
     */
    public static @Nullable String toMediumString(@Nullable DateTime dateTime) {
        return dateTime == null ? null : MEDIUM_DATETIME_FORMATTER.print(dateTime);
    }

    /** Converts a birthdate to a string describing age in months or years. */
    public static String birthdateToAge(LocalDate birthdate, Resources resources) {
        Period age = new Period(birthdate, LocalDate.now());
        int years = age.getYears(), months = age.getMonths();
        return years >= 5 ? resources.getString(R.string.abbrev_n_years, years) :
                resources.getString(R.string.abbrev_n_months, months + years*12);
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

    /** Creates an interval from a min and max, where null means "unbounded". */
    public static Interval toInterval(ReadableInstant start, ReadableInstant stop) {
        return new Interval(start == null ? MIN_DATETIME : start,
            stop == null ? MAX_DATETIME : stop);
    }

    /** Compresses a UUID optionally to a small integer. */
    public static Object compressUuid(String uuid) {
        Matcher matcher = COMPRESSIBLE_UUID.matcher(uuid);
        if (uuid.length() == 36 && matcher.matches()) {
            return Integer.valueOf(matcher.group(1));
        }
        return uuid;
    }

    /** Expands a UUID that has been optionally compressed to a small integer. */
    public static String expandUuid(Object id) {
        String str = "" + id;
        if (str.matches("^[0-9]+$")) {
            return (str + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA").substring(0, 36);
        }
        return (String) id;
    }

    /** Shows or hides a dialog based on a boolean flag. */
    public static void showDialogIf(@Nullable Dialog dialog, boolean show) {
        if (dialog != null) {
            if (show) {
                dialog.show();
            } else {
                dialog.hide();
            }
        }
    }

    /** Shows or a hides a view based on a boolean flag. */
    public static void showIf(View view, boolean show) {
        view.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    /** Gets the stack trace as a string.  Handy for looking inside exceptions when debugging. */
    public static String toString(Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    public static String removeUnsafeChars(String input)
    {
        return input.replaceAll("[\\W]", "_");
    }

    /** Renders a Pebble template. */
    public static String renderTemplate(String filename, Map<String, Object> context) {
        if (sEngine == null) {
            // PebbleEngine caches compiled templates by filename, so as long as we keep using the
            // same engine instance, it's okay to call getTemplate(filename) on each render.
            sEngine = new PebbleEngine();
            sEngine.addExtension(new PebbleExtension());
        }
        try {
            StringWriter writer = new StringWriter();
            sEngine.getTemplate("assets/" + filename).evaluate(writer, context);
            return writer.toString();
        } catch (Exception e) {
            StringWriter writer = new StringWriter();
            e.printStackTrace(new PrintWriter(writer));
            return "<div style=\"font-size: 150%\">" + writer.toString().replace("&", "&amp;").replace("<", "&lt;").replace("\n", "<br>");
        }
    }

    private Utils() {
        // Prevent instantiation.
    }
}
