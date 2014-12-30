package org.msf.records.utils;

import android.text.format.DateFormat;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.joda.time.PeriodType;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Locale;

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
     * Converts a timestamp to a date string.
     */
    public static String timestampToDate(Long timestamp) {
        if (timestamp == null) {
            timestamp = 0L;
        }
        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
        cal.setTimeInMillis(timestamp * 1000);
        return DateFormat.format("dd-MM-yyyy", cal).toString();
    }

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
