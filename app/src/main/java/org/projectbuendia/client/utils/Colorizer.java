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

import android.support.v4.util.LruCache;

import java.util.Arrays;
import java.util.Random;

/** A class that generates aesthetically pleasing colors based on a color wheel. */
public class Colorizer {

    /** A {@link Colorizer} that has 12 colors. */
    public static final Colorizer C_12 = new Colorizer(0);
    /** A {@link Colorizer} that has 24 colors. */
    public static final Colorizer C_24 = new Colorizer(1);
    /** A {@link Colorizer} that has 48 colors. */
    public static final Colorizer C_48 = new Colorizer(2);
    /** A {@link Colorizer} that has 96 colors. */
    public static final Colorizer C_96 = new Colorizer(3);
    private static final int[] sDefaultPalette = new int[] {
        0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFF00FFFF, 0xFFFF00FF, 0xFFFFFF00,
        0xFFFF7000, 0xFF7FFF00, 0xFF00FF7F, 0xFF007FFF, 0xFF7F00FF, 0xFFFF007F};
    private static final int[][] sDefaultPaletteBytes = getPaletteBytes(sDefaultPalette);
    private final LruCache<Integer, Integer> mCache = new LruCache<>(128);
    private final int[][] mPaletteBytes;
    private final int mInterpolations;
    private final int mInterpolationMultiplier;
    private final int mColorCount;
    private final double mShift;
    private final Random mRandom;

    public static Colorizer withPalette(int... palette) {
        return new Colorizer(Arrays.copyOf(palette, palette.length));
    }

    public Colorizer interpolate(int interpolation) {
        return new Colorizer(this, interpolation);
    }

    /**
     * Creates a new {@link Colorizer} based on this one but with the specified tint applied to each
     * calculated color.
     */
    public Colorizer withTint(double tint) {
        if (tint < 0. || tint > 1.) {
            throw new IllegalArgumentException("Tint must be between 0 and 1.");
        }

        return new Colorizer(this, tint);
    }

    /**
     * Creates a new {@link Colorizer} based on this one but with the specified shade applied to
     * each calculated color.
     */
    public Colorizer withShade(double shade) {
        if (shade < 0. || shade > 1.) {
            throw new IllegalArgumentException("Shade must be between 0 and 1.");
        }

        return new Colorizer(this, -shade);
    }

    /** Gets an ARGB value for the specified object. */
    public int getColorArgb(Object o) {
        return getColorArgb(mix(o == null ? 0 : o.hashCode()));
    }

    /** Gets an ARGB color value for the specified integer. */
    public int getColorArgb(int i) {
        Integer cachedValue = mCache.get(i);
        if (cachedValue != null) {
            return cachedValue;
        }

        int colorIndex = i%mColorCount;
        if (colorIndex < 0) {
            colorIndex += mColorCount;
        }

        int[] rgb;
        if (mInterpolations == 0) {
            rgb = Arrays.copyOf(mPaletteBytes[colorIndex], 3);
        } else {
            int basePaletteIndex = colorIndex/mInterpolationMultiplier;
            int basePaletteOffset = colorIndex - basePaletteIndex*mInterpolationMultiplier;
            double offsetFraction = (double) basePaletteOffset/mInterpolationMultiplier;

            int[] startRgb = mPaletteBytes[basePaletteIndex];
            int[] endRgb = mPaletteBytes[(basePaletteIndex + 1)%mPaletteBytes.length];

            // TODO: Consider doing interpolations in another color space.
            rgb = new int[] {
                (int) (startRgb[0] + offsetFraction*(endRgb[0] - startRgb[0])),
                (int) (startRgb[1] + offsetFraction*(endRgb[1] - startRgb[1])),
                (int) (startRgb[2] + offsetFraction*(endRgb[2] - startRgb[2]))
            };
        }

        double shift = mShift*0xFF;

        int value = 0xFF000000
            | (int) Math.max(Math.min(rgb[0] + shift, 255.), 0)<<16
            | (int) Math.max(Math.min(rgb[1] + shift, 255.), 0)<<8
            | (int) Math.max(Math.min(rgb[2] + shift, 255.), 0);
        mCache.put(i, value);
        return value;
    }

    private int mix(int value) {
        mRandom.setSeed(value);
        return mRandom.nextInt();
    }

    private Colorizer(int[] palette) {
        this(getPaletteBytes(palette), 0, 0.);
    }

    private Colorizer(int[][] paletteBytes, int interpolations, double shift) {
        mPaletteBytes = paletteBytes;
        mInterpolations = interpolations;
        mInterpolationMultiplier = 1<<interpolations;
        mColorCount = paletteBytes.length*mInterpolationMultiplier;
        mShift = shift;
        mRandom = new Random();
    }

    private static int[][] getPaletteBytes(int[] palette) {
        int[][] paletteBytes = new int[palette.length][];
        for (int i = 0; i < palette.length; i++) {
            paletteBytes[i] = new int[] {
                ((palette[i]>>>16) & 0xff),
                ((palette[i]>>>8) & 0xff),
                ((palette[i]) & 0xff)
            };
        }

        return paletteBytes;
    }

    private Colorizer(int interpolations) {
        this(sDefaultPaletteBytes, interpolations, 0.);
    }

    private Colorizer(Colorizer colorizer, int interpolations) {
        this(colorizer.mPaletteBytes, colorizer.mInterpolations + interpolations, colorizer.mShift);
    }

    private Colorizer(Colorizer colorizer, double shiftOffset) {
        this(
            colorizer.mPaletteBytes,
            colorizer.mInterpolations,
            Math.max(Math.min(colorizer.mShift + shiftOffset, 1.), -1.));
    }
}
