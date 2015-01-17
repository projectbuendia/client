package org.msf.records.utils;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.joda.time.PeriodType;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility methods.
 */
public class Utils {

    /**
     * Converts a byte array to a hex string.
     *
     * <p>The resulting strings will have two characters per byte.
     */
    public static String bytesToHex(byte[] data) {
        StringBuilder hex = new StringBuilder();
        for (byte aData : data) {
            hex.append(byteToHex(aData).toUpperCase(Locale.US));
            hex.append(" ");
        }
        return (hex.toString());
    }

    /**
     * Converts a byte to a two-character hex string.
     */
    public static String byteToHex(byte data) {
        return String.valueOf(toHexChar((data >>> 4) & 0x0F)) + toHexChar(data & 0x0F);
    }

    /**
     * Converts an integer between 0 and 15 to a hex char.
     */
    public static char toHexChar(int i) {
        if (0 <= i && i <= 9) {
            return (char) ('0' + i);
        } else {
            return (char) ('a' + (i - 10));
        }
    }

    /**
     * Compares two objects that may each be null, Integer, or String.  null sorts
     * before everything; all Integers sort before all Strings; Integers sort
     * according to numeric value; Strings sort according to string value.
     */
    public static Comparator<Object> integerOrStringComparator = new Comparator<Object>() {
        @Override
        public int compare(Object a, Object b) {
            if (a instanceof Integer && b instanceof Integer) {
                return (Integer) a - (Integer) b;
            }
            if (a instanceof String && b instanceof String) {
                return ((String) a).compareTo((String) b);
            }
            return (a == null ? 0 : a instanceof Integer ? 1 : 2)
                    - (b == null ? 0 : b instanceof Integer ? 1 : 2);
        }
    };

    /**
     * Compares two lists, each of whose elements is a null, Integer, or String,
     * lexicographically by element, just like Python does.
     */
    public static Comparator<List<Object>> integerOrStringListComparator = new Comparator<List<Object>>() {
        @Override
        public int compare(List<Object> a, List<Object> b) {
            int result = 0;
            for (int i = 0; result == 0; i++) {
                if (i >= a.size() || i >= b.size()) {
                    return a.size() - b.size();
                }
                result = integerOrStringComparator.compare(a.get(i), b.get(i));
            }
            return result;
        }
    };

    /**
     * Compares two strings in a way that sorts alphabetic parts in alphabetic
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
        // Note: Use of \L here assumes a string that is already NFC-normalized.
        private final Pattern NUMBER_OR_WORD_PATTERN = Pattern.compile("[0-9]+|\\p{L}+");

        /**
         * Breaks a string into a list of Integers (from sequences of ASCII digits)
         * and Strings (from sequences of letters).  Other characters are ignored.
         */
        private List<Object> getParts(String str) {
            Matcher matcher = NUMBER_OR_WORD_PATTERN.matcher(str);
            List<Object> parts = new ArrayList<>();
            while (matcher.find()) {
                String part = matcher.group();
                parts.add(part.matches("[0-9]+") ? Integer.valueOf(part) : part);
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
            return integerOrStringListComparator.compare(aParts, bParts);
        }
    };

    /**
     *  Calculates the time difference between given timestamp and current timestamp.
     *
     * @param  timestamp  the long to compare with current date
     * @return Period between the 2 dates
     */
    public static Period timeDifference(Long timestamp) {
        if (timestamp == null) {
            return Period.millis(0);
        }
        DateTime start = new DateTime(timestamp * 1000);
        DateTime currentDate = new DateTime();
        return new Period(start, currentDate, PeriodType.days());
    }

    /**
     * Encodes a URL parameter, catching the useless exception that never happens.
     */
    public static String urlEncode(String s) {
        try {
            // Oh Java, how you make the simplest operation a waste of millions of programmer-hours.
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError("UTF-8 should be supported in every JVM");
        }
    }

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

    /** Converts a birthdate to a string describing age in months or years. */
    public static String birthdateToAge(LocalDate birthdate) {
        Period age = new Period(birthdate, LocalDate.now());
        if (age.getYears() >= 2) {
            return "" + age.getYears() + " y";
        } else {
            return "" + (age.getYears() * 12 + age.getMonths()) + " mo";
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
        } catch (Exception e) {
            // This should never happen
            return null;
        }
    }

    private Utils() {
    	// Prevent instantiation.
    }
}
