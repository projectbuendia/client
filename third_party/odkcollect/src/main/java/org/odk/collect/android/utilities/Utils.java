package org.odk.collect.android.utilities;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Locale;

public class Utils {
    // ==== Basic types ====

    /**
     * Java's default .equals() and == are both broken whereas Objects.equals is
     * usually correct, so let's make its logic available under a short, easy name.
     */
    public static boolean eq(Object a, Object b) {
        // noinspection EqualsReplaceableByObjectsCall (this is deliberately inlined)
        return (a == b) || (a != null && a.equals(b));
    }

    /** Returns a value if that value is not null, or a specified default value otherwise. */
    public static @NonNull <T> T orDefault(@Nullable T value, @NonNull T defaultValue) {
        return value != null ? value : defaultValue;
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

    public static @Nullable String localize(@Nullable String packed, Context context) {
        if (packed == null) return null;
        return new Loc(packed).get(context.getResources().getConfiguration().locale);
    }
}
