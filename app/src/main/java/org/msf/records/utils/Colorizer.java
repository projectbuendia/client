package org.msf.records.utils;

import android.support.v4.util.LruCache;

import java.util.Arrays;

/**
 * A class that generates aesthetically pleasing colors based on a color wheel.
 */
public class Colorizer {

    private static final int[] sColorWheel = new int[] {
            0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFF00FFFF, 0xFFFF00FF, 0xFFFFFF00,
            0xFFFF7000, 0xFF7FFF00, 0xFF00FF7F, 0xFF007FFF, 0xFF7F00FF, 0xFFFF007F };

    private static final int[][] sColorWheelBytes = getColorWheelBytes();

    /**
     * A {@link Colorizer} that has 12 colors.
     */
    public static final Colorizer _12 = new Colorizer(0);

    /**
     * A {@link Colorizer} that has 24 colors.
     */
    public static final Colorizer _24 = new Colorizer(1);

    /**
     * A {@link Colorizer} that has 48 colors.
     */
    public static final Colorizer _48 = new Colorizer(2);

    /**
     * A {@link Colorizer} that has 96 colors.
     */
    public static final Colorizer _96 = new Colorizer(3);

    private final LruCache<Integer, Integer> mCache = new LruCache<Integer, Integer>(128);

    private final int mInterpolations;
    private final int mInterpolationMultiplier;
    private final int mColorCount;
    private final double mShift;

    private Colorizer(int interpolations) {
        if (interpolations < 0 || interpolations > 5) {
            throw new IllegalArgumentException(
                    "The number of interpolations must be between 0 and 5.");
        }

        mInterpolations = interpolations;
        mInterpolationMultiplier = 1 << interpolations;
        mColorCount = sColorWheel.length * (mInterpolationMultiplier);
        mShift = 0.;
    }

    private Colorizer(Colorizer colorizer, double shiftOffset) {
        mInterpolations = colorizer.mInterpolations;
        mInterpolationMultiplier = colorizer.mInterpolationMultiplier;
        mColorCount = colorizer.mColorCount;
        mShift = Math.max(Math.min(colorizer.mShift + shiftOffset, 1.), -1.);
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

    /**
     * Gets an ARGB color value for the specified integer.
     */
    public int getColorArgb(int i) {
        Integer cachedValue = mCache.get(i);
        if (cachedValue != null) {
            return cachedValue;
        }

        int colorIndex = i % mColorCount;
        if (colorIndex < 0) {
            colorIndex += mColorCount;
        }

        int[] rgb;
        if (mInterpolations == 0) {
            rgb = Arrays.copyOf(sColorWheelBytes[colorIndex], 3);
        } else {
            int baseWheelIndex = colorIndex / mInterpolationMultiplier;
            int baseWheelOffset = colorIndex - baseWheelIndex * mInterpolationMultiplier;
            double offsetFraction = (double) baseWheelOffset / mInterpolationMultiplier;

            int[] startRgb = sColorWheelBytes[baseWheelIndex];
            int[] endRgb = sColorWheelBytes[(baseWheelIndex + 1) % 12];

            // TODO(dxchen): Consider doing interpolations in another color space.
            rgb = new int[] {
                    (int) (startRgb[0] + offsetFraction * (endRgb[0] - startRgb[0])),
                    (int) (startRgb[1] + offsetFraction * (endRgb[1] - startRgb[1])),
                    (int) (startRgb[2] + offsetFraction * (endRgb[2] - startRgb[2]))
            };
        }

        double shift = mShift * 0xFF;

        int value = 0xFF000000
                | (int) Math.max(Math.min(rgb[0] + shift, 255.), 0) << 16
                | (int) Math.max(Math.min(rgb[1] + shift, 255.), 0) << 8
                | (int) Math.max(Math.min(rgb[2] + shift, 255.), 0);
        mCache.put(i, value);
        return value;
    }

    /**
     * Gets an ARGB value for the specified object.
     */
    public int getColorArgb(Object o) {
        return getColorArgb(o == null ? 0 : o.hashCode());
    }

    private static int[][] getColorWheelBytes() {
        int[][] colorWheelBytes = new int[sColorWheel.length][];
        for (int i = 0; i < sColorWheel.length; i++) {
            colorWheelBytes[i] = new int[] {
                    ((sColorWheel[i] >>> 16) & 0xff),
                    ((sColorWheel[i] >>> 8) & 0xff),
                    ((sColorWheel[i]) & 0xff)
            };
        }

        return colorWheelBytes;
    }
}
