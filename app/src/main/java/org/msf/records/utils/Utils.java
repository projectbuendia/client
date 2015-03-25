package org.msf.records.utils;

import com.google.common.collect.Lists;

import org.msf.records.App;
import org.msf.records.net.Server;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

/** Utility methods. */
public class Utils {

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

    /**
     * Compares two objects that may be null, Integer, Long, BigInteger, or String.
     * null sorts before everything; all integers sort before all strings; integers
     * sort according to numeric value; strings sort according to string value.
     */
    public static Comparator<Object> nullIntStrComparator = new Comparator<Object>() {
        @Override
        public int compare(Object a, Object b) {
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
    public static Comparator<List<Object>> nullIntStrListComparator = new Comparator<List<Object>>() {
        @Override
        public int compare(List<Object> a, List<Object> b) {
            for (int i = 0; i < Math.min(a.size(), b.size()); i++) {
                int result = nullIntStrComparator.compare(a.get(i), b.get(i));
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
     *   - compare(s, t) == 0 if and only if s.equals(t).
     *   - compare(s, s + t) < 0 for any strings s and t.
     *   - compare(s + x, s + y) == Integer.compare(x, y) for all integers x, y
     *     and strings s that do not end in a digit.
     *   - compare(s + t, s + u) == compare(s, t) for all strings s and strings
     *     t, u that consist entirely of Unicode letters.
     * For example, the strings ["b1", "a11a", "a11", "a2", "a2b", "a2a", "a1"]
     * have the sort order ["a1", "a2", "a2a", "a2b", "a11", "a11a", "b1"].
     */
    public static Comparator<String> alphanumericComparator = new Comparator<String>() {
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

        @Override
        public int compare(String a, String b) {
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
    };

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
     * @param pairs An even number of arguments providing key-value pairs of
     *              arbitrary data to record with the event.
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

    private Utils() {
        // Prevent instantiation.
    }
}
