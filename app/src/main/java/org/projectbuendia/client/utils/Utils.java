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

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.Dialog;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.google.common.collect.Lists;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;
import org.joda.time.Instant;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.joda.time.ReadableInstant;
import org.joda.time.ReadablePartial;
import org.joda.time.base.AbstractInstant;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.projectbuendia.client.App;
import org.projectbuendia.client.R;
import org.projectbuendia.client.json.JsonUser;
import org.projectbuendia.client.net.Server;

import java.io.File;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Utility methods. */
public class Utils {
    public static final int SECOND = 1000;  // in ms
    public static final int MINUTE = 60 * SECOND;  // in ms
    public static final int HOUR = 60 * MINUTE;  // in ms
    public static final int DAY = 24 * HOUR;  // in ms

    public static final String EN_DASH = "\u2013";
    public static final String EM_DASH = "\u2014";
    public static final String BULLET = "\u2022";

    // Minimum and maximum representable Instant, DateTime, and LocalDate values.
    public static final Instant MIN_TIME = new Instant(Long.MIN_VALUE);
    public static final Instant MAX_TIME = new Instant(Long.MAX_VALUE);
    public static final DateTime MIN_DATETIME = new DateTime(MIN_TIME, DateTimeZone.UTC);
    public static final DateTime MAX_DATETIME = new DateTime(MAX_TIME, DateTimeZone.UTC);
    public static final LocalDate MIN_DATE = new LocalDate(0, 1, 1).year().withMinimumValue();
    public static final LocalDate MAX_DATE = new LocalDate(0, 12, 31).year().withMaximumValue();

    private static Map<Integer, String> sHttpMethods = initHttpMethods();
    private static Map<Integer, String> initHttpMethods() {
        Map<Integer, String> map = new HashMap<>();
        map.put(Request.Method.DEPRECATED_GET_OR_POST, "DEPRECATED_GET_OR_POST");
        map.put(Request.Method.GET, "GET");
        map.put(Request.Method.POST, "POST");
        map.put(Request.Method.PUT, "PUT");
        map.put(Request.Method.DELETE, "DELETE");
        map.put(Request.Method.HEAD, "HEAD");
        map.put(Request.Method.OPTIONS, "OPTIONS");
        map.put(Request.Method.TRACE, "TRACE");
        map.put(Request.Method.PATCH, "PATCH");
        return map;
    }


    /** Prevent instantiation. */
    private Utils() { }


    // ==== Basic types ====

    // Java's default .equals() and == are both broken whereas Objects.equals is
    // usually correct, so let's make its logic available under a short, easy name.
    public static boolean eq(Object a, Object b) {
        // noinspection EqualsReplaceableByObjectsCall (this is deliberately inlined)
        return (a == b) || (a != null && a.equals(b));
    }

    public static boolean eqAny(Object x, Object... values) {
        for (Object value : values) {
            if (eq(x, value)) return true;
        }
        return false;
    }

    /** Returns a value if that value is not null, or a specified default value otherwise. */
    public static @Nonnull <T> T orDefault(@Nullable T value, @Nonnull T defaultValue) {
        return value != null ? value : defaultValue;
    }

    /** Returns a value if that value is not null, or a specified default value otherwise. */
    public static @Nonnull String nonemptyOrDefault(@Nullable String value, @Nonnull String defaultValue) {
        return isEmpty(value) ? defaultValue : value;
    }

    /** Converts nulls to a default integer value. */
    public static int toNonnull(@Nullable Integer n, int defaultValue) {
        return n == null ? defaultValue : n;
    }

    /** The same operation as map.getOrDefault(key), which is only available in API 24+. */
    public static <K, V> V getOrDefault(Map<K, V> map, K key, V defaultValue) {
        return map.containsKey(key) ? map.get(key) : defaultValue;
    }

    /** Safely index into an array, clamping the index if it's out of bounds. */
    public static <T> T safeIndex(T[] array, int index) {
        if (array.length == 0) return null;
        if (index < 0) index = 0;
        if (index > array.length - 1) index = array.length - 1;
        return array[index];
    }

    /** Converts a list of Strings to an array of Strings. */
    public static String[] toStringArray(List<String> items) {
        if (items == null) return new String[0];
        return items.toArray(new String[0]);
    }

    /** Converts a list of Longs to an array of primitive longs. */
    public static long[] toLongArray(List<Long> items) {
        if (items == null) return new long[0];
        long[] array = new long[items.size()];
        int i = 0;
        for (Long item : items) {
            array[i++] = item;
        }
        return array;
    }

    /** Provides Math.floorMod for Android versions prior to API level 24, d > 0. */
    public static int floorMod(int x, int d) {
        return ((x % d) + d) % d;
    }

    /** Provides Math.floorDiv for Android versions prior to API level 24, d > 0. */
    public static int floorDiv(int x, int d) {
        return (x - floorMod(x, d)) / d;
    }

    /** Formats a number to the minimum necessary number of decimal places. */
    public static String format(double x, int maxPrec) {
        String result = String.format("%." + maxPrec + "f", x);
        if (result.contains("e")) return result;
        if (!result.contains(".") && !result.contains(",")) return result;
        return result.replaceAll("[.,]?0*$", "");
    }

    // ==== Collections ====

    public static boolean isEmpty(Object obj) {
        if (obj instanceof Collection) {
            return isEmpty((Collection) obj);
        }
        if (obj instanceof String) {
            return eq(obj, "");
        }
        if (obj instanceof Object[]) {
            return ((Object[]) obj).length == 0;
        }
        return obj == null;
    }

    /** Performs a null-safe check for a null or empty array. */
    public static <T> boolean isEmpty(@Nullable T[] array) {
        return array == null || array.length == 0;
    }

    /** Performs a null-safe check for a null or empty Collection. */
    public static boolean isEmpty(@Nullable Collection collection) {
        return collection == null || collection.size() == 0;
    }

    /** Converts nulls to empty Lists. */
    public static <T> List<T> toNonnull(@Nullable List<T> list) {
        return list != null ? list : Arrays.<T>asList();
    }

    /** Performs a null-safe check for an array with at least one item. */
    public static <T> boolean hasItems(@Nullable T[] array) {
        return array != null && array.length > 0;
    }

    /** Performs a null-safe check for a Collection with at least one item. */
    public static boolean hasItems(@Nullable Collection collection) {
        return collection != null && collection.size() > 0;
    }

    /** Concatenates two arrays. */
    public static <T> T[] concat(T[] a, T[] b) {
        List<T> result = new ArrayList<>(Arrays.asList(a));
        result.addAll(Arrays.asList(b));
        return result.toArray(b);
    }

    /** Prepends an item to an array to yield a new array. */
    public static <T> T[] concat(T a, T[] b) {
        List<T> result = new ArrayList<>(Arrays.asList(a));
        result.addAll(Arrays.asList(b));
        return result.toArray(b);
    }

    /** Appends an item to an array to yield a new array. */
    public static <T> T[] concat(T[] a, T b) {
        List<T> result = new ArrayList<>(Arrays.asList(a));
        result.add(b);
        return result.toArray(a);
    }

    /** Gets an item given a Pythonic list index, or null if out of bounds. */
    public static <T> @Nullable T getitem(List<T> list, int index) {
      int n = list.size();
      if (index < 0) index += n;
      if (index > n) index = n;
      return (index >= 0 && index < n) ? list.get(index) : null;
    }

    public static <T> @Nullable T first(List<T> list) {
        return getitem(list, 0);
    }

    public static <T> @Nullable T last(List<T> list) {
        return getitem(list, -1);
    }

    public static <T> List<T> slice(List<T> list, int start, int stop) {
        int n = list.size();
        if (start < 0) start += n;
        if (stop < 0) stop += n;
        if (start > n) start = n;
        if (stop > n) stop = n;
        return list.subList(start, stop);
    }


    // ==== Strings ====

    /** Performs a null-safe check for a null or empty String. */
    public static boolean isEmpty(@Nullable String str) {
        return str == null || str.length() == 0;
    }

    /** Performs a null-safe check for a null, empty, or whitespace String. */
    public static boolean isBlank(@Nullable String str) {
        return str == null || str.length() == 0 || str.trim().length() == 0;
    }

    /** Performs a null-safe check for a String with at least one character. */
    public static boolean hasChars(@Nullable String str) {
        return str != null && str.length() > 0;
    }

    /** Converts empty strings to null. */
    public static @Nullable String toNonemptyOrNull(@Nullable String str) {
        return isEmpty(str) ? null : str;
    }

    /** Converts nulls to empty strings. */
    public static @Nonnull String toNonnull(@Nullable String str) {
        return str != null ? str : "";
    }

    /** Calls toString() on a nullable object, returning an empty string if null. */
    public static @Nonnull String toNonnullString(@Nullable Object obj) {
        return obj != null ? obj.toString() : "";
    }

    /** Calls toString() on a nullable object, returning null if the object is null. */
    public static @Nullable String toNullableString(@Nullable Object obj) {
        return obj != null ? obj.toString() : null;
    }

    /** Calls toString() on a nullable object, returning a default value if the object is null. */
    public static @Nonnull String toStringOrDefault(@Nullable Object obj, @Nonnull String defaultValue) {
        return obj != null ? obj.toString() : defaultValue;
    }

    /** Formats a string using ASCII encoding. */
    public static String format(String template, Object... args) {
        return String.format(Locale.US, template, args);
    }

    /** Splits a string, returning an array padded out to known length with empty strings. */
    public static String[] splitFields(String text, String separator, int count) {
        String[] fields = text.split(separator, -1);
        String[] result = new String[count];
        for (int i = 0; i < count; i++) {
            result[i] = i < fields.length ? fields[i] : "";
        }
        return result;
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


    // ==== Number parsing ====

    /** Converts a String to an integer, returning null if parsing fails. */
    public static Integer toIntOrNull(@Nullable String str) {
        if (str == null) return null;
        try {
            return Integer.valueOf(str);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Converts a String to an integer, returning a default value if parsing fails. */
    public static int toIntOrDefault(String str, int defaultValue) {
        Integer value = toIntOrNull(str);
        return value == null ? defaultValue : value;
    }

    /** Converts a String to a long integer, returning null if parsing fails. */
    public static @Nullable Long toLongOrNull(@Nullable String str) {
        if (str == null) return null;
        try {
            return Long.parseLong(str);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Converts a String to a double, returning null if parsing fails. */
    public static @Nullable Double toDoubleOrNull(@Nullable String str) {
        if (str == null) return null;
        try {
            return Double.parseDouble(str.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Converts a String to a double, returning a default value if parsing fails. */
    public static double toDoubleOrDefault(@Nullable String str, double defaultValue) {
        Double value = toDoubleOrNull(str);
        return value == null ? defaultValue : value;
    }

    /** Converts objects of integer types to longs. */
    public static Long toLongOrNull(Object obj) {
        if (obj instanceof Integer) return (long) (Integer) obj;
        if (obj instanceof Long) return (long) obj;
        if (obj instanceof BigInteger) return ((BigInteger) obj).longValue();
        if (obj instanceof String) return toLongOrNull((String) obj);
        return null;
    }

    /** Converts objects of integer types to BigIntegers. */
    public static BigInteger toBigInteger(Object obj) {
        if (obj instanceof Integer) return BigInteger.valueOf(((Integer) obj).longValue());
        if (obj instanceof Long) return BigInteger.valueOf((Long) obj);
        if (obj instanceof BigInteger) return (BigInteger) obj;
        return null;
    }


    // ==== Dates and times ====

    private static final DateTimeFormatter ISO8601_UTC_DATETIME_FORMATTER =
        DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZoneUTC();

    /** Returns the lesser of two DateTimes, treating null as the greatest value. */
    public static @Nullable DateTime min(DateTime a, DateTime b) {
        return a == null ? b : b == null ? a : a.isBefore(b) ? a : b;
    }

    /** Returns the greater of two DateTimes, treating null as the least value. */
    public static @Nullable DateTime max(DateTime a, DateTime b) {
        return a == null ? b : b == null ? a : a.isAfter(b) ? a : b;
    }

    /** Converts a nullable LocalDate to a yyyy-mm-dd String or null. */
    public static @Nullable String format(@Nullable LocalDate date) {
        return date != null ? date.toString() : null;
    }

    /** Creates a DateTime object in the default local time zone. */
    public static @Nullable DateTime toLocalDateTime(@Nullable Long millis) {
        return millis != null ? new DateTime(millis, DateTimeZone.getDefault()) : null;
    }

    /** Creates a DateTime object in the default local time zone. */
    public static @Nullable DateTime toLocalDateTime(@Nullable ReadableInstant instant) {
        return instant != null ? new DateTime(instant, DateTimeZone.getDefault()) : null;
    }

    /** Converts a yyyy-mm-dd String or null to a nullable LocalDate. */
    public static @Nullable LocalDate toLocalDate(@Nullable String string) {
        try {
            return string != null ? LocalDate.parse(string) : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Formats a nullable DateTime or Instant as "yyyy-mm-ddThh:mm:ssZ" or null. */
    public static @Nullable String formatUtc8601(@Nullable AbstractInstant t) {
        return t != null ? t.toString(ISO8601_UTC_DATETIME_FORMATTER) : null;
    }

    /** Parses a nullable String into a nullable Interval. */
    public static Interval toNullableInterval(String str) {
        return str != null ? Interval.parse(str) : null;
    }

    public static enum DateStyle {
        MONTH_DAY(R.string.month_day_format),
        YEAR_MONTH_DAY(R.string.year_month_day_format),
        HOUR_MINUTE(R.string.hour_minute_format),
        MONTH_DAY_HOUR_MINUTE(R.string.month_day_hour_minute_format),

        // "Sentence" formats should work in phrases like "The store opens X"
        SENTENCE_MONTH_DAY(R.string.sentence_month_day_format),
        SENTENCE_YEAR_MONTH_DAY(R.string.sentence_year_month_day_format),
        SENTENCE_HOUR_MINUTE(R.string.sentence_hour_minute_format),
        SENTENCE_MONTH_DAY_HOUR_MINUTE(R.string.sentence_month_day_hour_minute_format),

        // "Relative" formats should work in phrases like "after X", "until X"
        RELATIVE_MONTH_DAY(R.string.relative_month_day_format),
        RELATIVE_YEAR_MONTH_DAY(R.string.relative_year_month_day_format),
        RELATIVE_HOUR_MINUTE(R.string.relative_hour_minute_format),
        RELATIVE_MONTH_DAY_HOUR_MINUTE(R.string.relative_month_day_hour_minute_format);

        private final int formatId;
        private Map<Locale, DateTimeFormatter> formatters = new HashMap<>();
        private DateTimeFormatter formatter = null;

        private DateStyle(int formatId) {
            this.formatId = formatId;
        }

        public DateTimeFormatter getFormatter() {
            Locale locale = App.getSettings().getLocale();
            if (formatters.get(locale) == null) {
                formatters.put(locale, DateTimeFormat.forPattern(App.str(formatId)));
            }
            return formatters.get(locale);
        }
    }

    public static @Nullable String format(@Nullable ReadablePartial partial, DateStyle style) {
        if (partial == null) return null;
        return style.getFormatter().print(partial);
    }

    public static @Nullable String format(@Nullable DateTime datetime, DateStyle style) {
        if (datetime == null) return null;
        return style.getFormatter().print(datetime);
    }

    /** Gets the DateTime at the start of a day. */
    public static DateTime getDayStart(LocalDate day) {
        return day.toDateTimeAtStartOfDay();
    }

    /** Gets the DateTime at the end of a day. */
    public static DateTime getDayEnd(LocalDate day) {
        return day.plusDays(1).toDateTimeAtStartOfDay();
    }

    /** Creates an interval from a min and max, where null means "unbounded". */
    public static Interval toInterval(ReadableInstant start, ReadableInstant stop) {
        return new Interval(Utils.orDefault(start, MIN_DATETIME), Utils.orDefault(stop, MAX_DATETIME));
    }

    /** Gets the DateTime at the center of an Interval. */
    public static DateTime centerOf(Interval interval) {
        return interval.getStart().plus(interval.toDuration().dividedBy(2));
    }

    /** Converts a nullable DateTime to a nullable number of millis since 1970-01-01T00:00:00. */
    public static Long toNullableMillis(ReadableInstant instant) {
        return instant != null ? instant.getMillis() : null;
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

    /** Checks whether a birthdate indicates an age less than 5 years old. */
    public static boolean isChild(LocalDate birthdate) {
        return birthdate != null && new Period(birthdate, LocalDate.now()).getYears() < 5;
    }

    /** Converts a birthdate to a string describing age in months or years. */
    public static String birthdateToAge(LocalDate birthdate) {
        Period age = new Period(birthdate, LocalDate.now());
        int years = age.getYears(), months = age.getMonths();
        return years >= 5 ? App.str(R.string.abbrev_n_years, years) :
            App.str(R.string.abbrev_n_months, months + years * 12);
    }


    // ==== Localization ====

    public static Locale toLocale(String languageTag) {
        if (Build.VERSION.SDK_INT >= 21) return Locale.forLanguageTag(languageTag);
        String[] parts = splitFields(languageTag, "_", 2);
        return new Locale(parts[0], parts[1]);
    }

    public static @Nullable String toLanguageTag(@Nullable Locale locale) {
        if (locale == null) return null;
        if (Build.VERSION.SDK_INT >= 21) return locale.toLanguageTag();
        return locale.getLanguage() +
            (Utils.isEmpty(locale.getCountry()) ? "" : "-" + locale.getCountry()) +
            (Utils.isEmpty(locale.getVariant()) ? "" : "-" + locale.getVariant());
    }

    /** Clears the activity stack and launches the given activity. */
    public static void jumpToActivity(Activity current, Class target) {
        if (!target.isInstance(current)) {
            current.finish();
            current.startActivity(new Intent(current, target).setFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
        }
    }

    /** Restarts the current activity (for use after a configuration change). */
    public static void restartActivity(Activity activity) {
        activity.finish();
        activity.startActivity(
            activity.getIntent(),
            ActivityOptions.makeCustomAnimation(activity, 0, 0).toBundle());
    }


    // ==== System ====

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

    public static void recursivelyDelete(File path) {
        if (path.isDirectory()) {
            for (File child : path.listFiles()) {
                recursivelyDelete(child);
            }
        }
        path.delete();
    }


    // ==== Cursors ====

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
        return millis == null ? null : Utils.toLocalDateTime(millis);
    }

    /** Gets a nullable boolean value from a cursor. */
    public static Boolean getBoolean(Cursor c, String columnName) {
        int index = c.getColumnIndex(columnName);
        return c.isNull(index) ? null : (c.getLong(index) != 0);
    }

    /** Gets a boolean value from a cursor, returning a default value instead of null. */
    public static boolean getBoolean(Cursor c, String columnName, boolean defaultValue) {
        int index = c.getColumnIndex(columnName);
        return c.isNull(index) ? defaultValue : (c.getLong(index) != 0);
    }

    /** Gets a nullable integer value from a cursor. */
    public static Integer getInt(Cursor c, String columnName) {
        int index = c.getColumnIndex(columnName);
        return c.isNull(index) ? null : (int) c.getLong(index);
    }

    /** Gets an integer value from a cursor, returning a default value instead of null. */
    public static int getInt(Cursor c, String columnName, int defaultValue) {
        int index = c.getColumnIndex(columnName);
        // The cast (Long) c.getLong(index) is necessary to work around the fact that
        // the Java compiler chooses type (long) for (boolean) ? (Long) : (long),
        // causing an NPE when defaultValue is null.  The correct superset of (Long) and
        // (long) is obviously (Long); the Java specification (15.25) is incorrect.
        return c.isNull(index) ? defaultValue : (int) c.getLong(index);
    }

    /** Gets a nullable long value from a cursor. */
    public static Long getLong(Cursor c, String columnName) {
        int index = c.getColumnIndex(columnName);
        return c.isNull(index) ? null : c.getLong(index);
    }

    /** Gets a long integer value from a cursor, returning a default value instead of null. */
    public static long getLong(Cursor c, String columnName, @Nonnull long defaultValue) {
        int index = c.getColumnIndex(columnName);
        // The cast (Long) c.getLong(index) is necessary to work around the fact that
        // the Java compiler chooses type (long) for (boolean) ? (Long) : (long),
        // causing an NPE when defaultValue is null.  The correct superset of (Long) and
        // (long) is obviously (Long); the Java specification (15.25) is incorrect.
        return c.isNull(index) ? defaultValue : c.getLong(index);
    }


    // ==== Bundles ====

    /** Gets a nullable DateTime value from a Bundle.  Always use this instead of getLong() directly. */
    public static DateTime getDateTime(Bundle bundle, String key) {
        // getLong never returns null; we have to check explicitly.
        return bundle.containsKey(key) ? Utils.toLocalDateTime(bundle.getLong(key)) : null;
    }

    /** Creates a Bundle containing one key-value pair. */
    public static Bundle bundle(String key1, Object value1) {
        return addToBundle(new Bundle(), key1, value1);
    }

    /** Creates a Bundle containing two key-value pairs. */
    public static Bundle bundle(String key1, Object value1, String key2, Object value2) {
        return addToBundle(
            addToBundle(
                new Bundle(), key1, value1
            ), key2, value2
        );
    }

    /** Creates a Bundle containing three key-value pairs. */
    public static Bundle bundle(String key1, Object value1, String key2, Object value2,
                                String key3, Object value3) {
        return addToBundle(
            addToBundle(
                addToBundle(
                    new Bundle(), key1, value1
                ), key2, value2
            ), key3, value3
        );
    }

    /** Adds a key-value pair to a Bundle and returns the Bundle for chaining. */
    public static Bundle addToBundle(Bundle bundle, String key, Object value) {
        if (value instanceof String) bundle.putString(key, (String) value);
        else if (value instanceof Double) bundle.putDouble(key, (Double) value);
        else if (value instanceof Float) bundle.putFloat(key, (Float) value);
        else if (value instanceof Long) bundle.putLong(key, (Long) value);
        else if (value instanceof Integer) bundle.putInt(key, (Integer) value);
        else if (value instanceof Boolean) bundle.putBoolean(key, (Boolean) value);
        else if (value instanceof ReadableInstant) bundle.putLong(key, ((ReadableInstant) value).getMillis());
        else if (value instanceof Bundle) bundle.putBundle(key, (Bundle) value);
        else if (value instanceof Serializable) bundle.putSerializable(key, (Serializable) value);
        else if (value != null) throw new IllegalArgumentException(format(
            "Don't know how to put a value of type %s into a Bundle", value.getClass()));
        return bundle;
    }

    /** Builds a Message with a Bundle of data. */
    public static Message newMessage(Handler handler, int what, Bundle data) {
        Message message = handler.obtainMessage(what);
        message.setData(data);
        return message;
    }


    // ==== Colour ====

    public static int colorWithOpacity(int color, double opacity) {
        byte alpha = (byte) (255 * opacity);
        return (color & 0x00_ff_ff_ff) | (alpha * 0x01_00_00_00);
    }


    // ==== UI Views ====

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
    public static void showIf(@Nullable View view, boolean show) {
        if (view != null) {
            view.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    /** Returns true if a view's visibility is set to VISIBLE. */
    public static boolean isVisible(@Nullable View view) {
        return view != null && view.getVisibility() == View.VISIBLE;
    }

    /** Sets the text of a child view identified by its ID. */
    public static void setText(@Nullable View view, int id, String text) {
        if (view != null) {
            TextView textView = view.findViewById(id);
            if (textView != null) textView.setText(text);
        }
    }

    /** Sets a view's enabled state and focusable state at the same time. */
    public static void setEnabled(View view, boolean enabled) {
        view.setEnabled(enabled);
        view.setFocusable(enabled);
        if (view instanceof EditText) view.setFocusableInTouchMode(true);
    }

    /** Set the enabled state of all the children of a view. */
    public static void setChildrenEnabled(ViewGroup view, boolean enabled) {
        for (int i = 0; i < view.getChildCount(); i++) {
            setEnabled(view.getChildAt(i), enabled);
        }
    }

    /** Brings up the soft keyboard. */
    public static void showKeyboard(Window window) {
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    /** Puts focus on the first empty field, or otherwise the first field. */
    public static void focusFirstEmptyField(EditText... fields) {
        for (EditText field : fields) {
            if (field.isEnabled() && field.getText().toString().isEmpty()) {
                field.requestFocus();
                return;
            }
        }

        // If all fields are populated, default to the first field.
        if (fields.length >= 1) {
            fields[0].requestFocus();
            fields[0].setSelection(fields[0].getText().length());
        }
    }

    /** Gets the text in an editable text field and trims away whitespace. */
    public static String getText(EditText field) {
        return field.getText().toString().trim();
    }

    /** Gets the integer value in a text field, or a default value if invalid. */
    public static int getInt(EditText field, int defaultValue) {
        return toIntOrDefault(field.getText().toString().trim(), defaultValue);
    }

    /** Gets the numeric value in a text field, or a default value if invalid. */
    public static double getDouble(EditText field, double defaultValue) {
        return toDoubleOrDefault(field.getText().toString().trim(), defaultValue);
    }


    // ==== OpenMRS ====

    private static final Pattern COMPRESSIBLE_UUID = Pattern.compile("^([0-9]+)A+$");

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

    /** Expands a UUID from a small integer. */
    public static String toUuid(int id) {
        return expandUuid(id);
    }

    /** Gets the UUID of the currently active user (provider). */
    public static String getProviderUuid() {
        JsonUser user = App.getUserManager().getActiveUser();
        return user != null ? user.getUuid() : null;
    }


    // ==== Ordering ====

    /**
     * Compares two objects that may be null, Integer, Long, BigInteger, or String.
     * null sorts before everything; all integers sort before all strings; integers
     * sort according to numeric value; strings sort according to string value.
     */
    public static final Comparator<Object> NULL_INT_STR_COMPARATOR = new Comparator<Object>() {
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
    public static final Comparator<List<Object>> NULL_INT_STR_LIST_COMPARATOR = new Comparator<List<Object>>() {
        @Override public int compare(List<Object> a, List<Object> b) {
            for (int i = 0; i < Math.min(a.size(), b.size()); i++) {
                int result = NULL_INT_STR_COMPARATOR.compare(a.get(i), b.get(i));
                if (result != 0) {
                    return result;
                }
            }
            return a.size() - b.size();
        }
    };

    // Note: Use of \L here assumes a string that is already NFC-normalized.
    private static final Pattern NUMBER_OR_WORD_PATTERN = Pattern.compile("([0-9]+)|\\p{L}+");

    /**
     * Compares two strings in a manner that sorts alphabetic parts in alphabetic
     * order and numeric parts in numeric order, while guaranteeing that:
     * - compare(s, t) == 0 if and only if s.equals(t).
     * - compare(s, s + t) < 0 for any strings s and t.
     * - compare(s + x, s + y) == Integer.compare(x, y) for all integers x, y
     * and strings s that do not end in a digit.
     * - compare(s + t, s + u) == compare(t, u) for all strings s and strings
     * t, u that consist entirely of Unicode letters.
     * For example, the strings ["b1", "a11a", "a11", "a2", "a2b", "a2a", "a1"]
     * have the sort order ["a1", "a2", "a2a", "a2b", "a11", "a11a", "b1"].
     */
    public static final Comparator<String> ALPHANUMERIC_COMPARATOR = new Comparator<String>() {
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
            return NULL_INT_STR_LIST_COMPARATOR.compare(aParts, bParts);
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


    // ==== Logging ====

    /**
     * Logs a user action by sending a dummy request to the server.  (The server
     * logs can then be scanned later to produce analytics for the client app.)
     * @param action An identifier for the user action; should describe a user-
     *               initiated operation in the UI (e.g. "foo_button_pressed").
     * @param pairs  An even number of arguments providing key-value pairs of
     *               arbitrary data to record with the event.
     */
    public static void logUserAction(String action, String... pairs) {
        Server server = App.getServer();
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
        Server server = App.getServer();
        if (server != null) {
            List<String> allPairs = Lists.newArrayList("event", event);
            allPairs.addAll(Arrays.asList(pairs));
            server.logToServer(allPairs);
        }
    }


    // ==== Debugging ====

    /** Gets the stack trace as a string.  Handy for looking inside exceptions when debugging. */
    public static String toString(Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    /** Converts a string to a lowercase CSS-safe identifier. */
    // We use this to give predictable class names to HTML rows so that tests
    // can verify the values in the patient chart.  See PatientChartActivityTest.
    public static String toCssIdentifier(String input) {
        return input.trim().toLowerCase().replaceAll("[^a-z0-9-]+", "-");
    }

    /** Returns an unambiguous string representation of a string, prefixed with its length. */
    public static String reprWithLen(String str) {
        return format("(length %d) ", str.length()) + repr(str);
    }

    /** Returns an unambiguous string representation of a string, suitable for logging. */
    public static String repr(String str) {
        return repr(str, 100);
    }

    /** Returns an unambiguous string representation of a string, suitable for logging. */
    public static String repr(String str, int maxLength) {
        try {
            return str != null ? escape(str, maxLength) : "(null String)";
        } catch (Throwable ignored) {
            return "(repr of " + str + " failed)";
        }
    }

    /** Returns an unambiguous string representation of a byte array, suitable for logging. */
    public static String repr(byte[] bytes, int maxLength) {
        try {
            return bytes != null ?
                escape(new String(bytes, "ISO-8859-1"), maxLength) : "(null byte[])";
        } catch (Throwable ignored) {
            return "(repr of " + bytes + " failed)";
        }
    }

    /** Returns a list-like string representation of an array of objects, suitable for logging. */
    public static String repr(Object[] array) {
        return "[" + join(", ", array) + "]";
    }

    public static String join(String sep, Object[] array) {
        String result = "";
        if (array.length > 0) result += array[0];
        for (int i = 1; i < array.length; i++) {
            result += sep + array[i];
        }
        return result;
    }

    public static String join(String sep, Iterable<?> iterable) {
        String result = "";
        int i = 0;
        for (Object obj : iterable) {
            if (i++ > 0) result += sep;
            result += obj;
        }
        return result;
    }

    /** Uses backslash sequences to form a printable representation of a string. */
    private static String escape(String str, int maxLength) {
        StringBuilder buffer = new StringBuilder("\"");
        for (int i = 0; i < str.length() && i < maxLength; i++) {
            char c = str.charAt(i);
            switch (str.charAt(i)) {
                case '\t':
                    buffer.append("\\t");
                    break;
                case '\r':
                    buffer.append("\\r");
                    break;
                case '\n':
                    buffer.append("\\n");
                    break;
                case '\\':
                    buffer.append("\\\\");
                    break;
                case '"':
                    buffer.append("\\\"");
                    break;
                default:
                    if ((int) c >= 32 && (int) c <= 126) {
                        buffer.append(c);
                    } else if ((int) c < 256) {
                        buffer.append(format("\\x%02x", (int) c));
                    } else {
                        buffer.append(format("\\u%04x", (int) c));
                    }
            }
        }
        buffer.append(str.length() > maxLength ? "\"..." : "\"");
        return buffer.toString();
    }

    /** Formats a description of a Request. */
    public static <T> String repr(Request<T> req) {
        try {
            if (req == null) {
                return "(null Request)";
            }
            String method = sHttpMethods.get(req.getMethod());
            if (method == null) {
                method = format("(method %d)", req.getMethod());
            }
            String data = "";
            if (req.getPostBody() != null) {
                try {
                    data = format(" (%s) %s", req.getBodyContentType(), repr(req.getPostBody(), 500));
                } catch (AuthFailureError e) {
                    data += " (" + repr(e) + ")";
                }
            }
            return format("(%s) %s %s%s", typeof(req), method, req.getUrl(), data);
        } catch (Throwable ignored) {
            return "(repr of " + req + " failed)";
        }
    }

    /** Formats a short description of a Throwable. */
    public static <T> String repr(Throwable t) {
        try {
            if (t == null) {
                return "(null Throwable)";
            }
            return format("%s: %s", typeof(t), t.getMessage());
        } catch (Throwable ignored) {
            return "(repr of " + t + " failed)";
        }
    }

    /** Formats a short description of the type of an object. */
    public static String typeof(Object obj) {
        String name = obj.getClass().getSimpleName();
        if (name.isEmpty()) {
            String[] parts = obj.getClass().getName().split("\\.");
            return parts.length == 0 ? "(anonymous)" : parts[parts.length - 1];
        }
        return name;
    }
}
