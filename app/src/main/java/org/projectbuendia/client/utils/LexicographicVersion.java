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

import com.google.common.base.Preconditions;

import java.util.Arrays;

import javax.annotation.concurrent.Immutable;

/** An object that represents a version consisting of any number of numbers separated by dots. */
@Immutable
public class LexicographicVersion implements Comparable<LexicographicVersion> {

    private final String mRaw;
    private final int[] mParts;

    /** Returns an instance of {@link LexicographicVersion} parsed from the specified string. */
    public static LexicographicVersion parse(String raw) {
        Preconditions.checkNotNull(raw);
        Preconditions.checkArgument(!raw.equals(""));

        String[] stringParts = raw.split("\\.");
        int[] parts = new int[stringParts.length];
        for (int i = 0; i < stringParts.length; i++) {
            try {
                parts[i] = Integer.parseInt(stringParts[i]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                    "'" + raw + "' is not a valid version string.", e);
            }
        }

        return new LexicographicVersion(raw, parts);
    }

    @Override public String toString() {
        return mRaw;
    }

    public boolean greaterThan(LexicographicVersion other) {
        return compareTo(other) > 0;
    }

    @Override public int compareTo(LexicographicVersion other) {
        Preconditions.checkNotNull(other);
        if (this == other) {
            return 0;
        }

        int minPartsLength = Math.min(mParts.length, other.mParts.length);
        for (int i = 0; i < minPartsLength; i++) {
            if (mParts[i] < other.mParts[i]) {
                return -1;
            } else if (mParts[i] > other.mParts[i]) {
                return 1;
            }
        }

        return mParts.length - other.mParts.length;
    }

    public boolean greaterThanOrEqualTo(LexicographicVersion other) {
        return compareTo(other) >= 0;
    }

    public boolean lessThan(LexicographicVersion other) {
        return compareTo(other) < 0;
    }

    public boolean lessThanOrEqualTo(LexicographicVersion other) {
        return compareTo(other) <= 0;
    }

    @Override public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof LexicographicVersion)) {
            return false;
        }
        return compareTo((LexicographicVersion) other) == 0;
    }

    @Override public int hashCode() {
        return Arrays.hashCode(mParts);
    }

    private LexicographicVersion(String raw, int[] parts) {
        mRaw = raw;
        mParts = parts;
    }
}
