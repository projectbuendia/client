package org.msf.records.utils;

import java.util.regex.Pattern;

import javax.annotation.concurrent.Immutable;

/**
 * An object that represents a version consisting of any number of numbers separated by dots.
 */
@Immutable
public class LexicographicVersion implements Comparable<LexicographicVersion> {
    private static final Pattern VERSION_PATTERN = Pattern.compile("[0-9]+(\\.[0-9]+)*");
    private final String mString;

    public LexicographicVersion(String string) {
        if (!VERSION_PATTERN.matcher(string).matches()) {
            throw new IllegalArgumentException(
                    "\"" + string + "\" is not a valid version string.");
        }
        mString = string;
    }

    @Override
    public String toString() {
        return mString;
    }

    /** Compares two optionally-null versions, treating null as the lowest value. */
    public static int compare(LexicographicVersion a, LexicographicVersion b) {
        if (a == null || b == null) {
            return (a == null ? 0 : 1) - (b == null ? 0 : 1);
        }
        return a.compareTo(b);
    }

    @Override
    public int compareTo(LexicographicVersion other) {
        return Utils.alphanumericComparator.compare(mString, other.mString);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof LexicographicVersion
                && compareTo((LexicographicVersion) other) == 0;
    }

    @Override
    public int hashCode() {
        return mString.hashCode();
    }
}
