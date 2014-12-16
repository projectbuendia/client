package org.msf.records.utils;

import android.text.format.DateFormat;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.joda.time.PeriodType;

import java.io.UnsupportedEncodingException;
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
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            buf.append(byteToHex(data[i]).toUpperCase(Locale.US));
            buf.append(" ");
        }
        return (buf.toString());
    }

    /**
     * Converts a byte to a two-character hex string.
     */
    public static String byteToHex(byte data) {
        StringBuffer buf = new StringBuffer();
        buf.append(toHexChar((data >>> 4) & 0x0F));
        buf.append(toHexChar(data & 0x0F));
        return buf.toString();
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

    private Utils() {
    	// Prevent instantiation.
    }
}
