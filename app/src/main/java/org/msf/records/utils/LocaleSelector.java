package org.msf.records.utils;

import java.util.Locale;

/**
 * Selects a Locale object for use throughout the app.
 * TODO(akalachman): Replace with proper locale management.
 */
public class LocaleSelector {
    public static Locale getCurrentLocale() {
        return Locale.ENGLISH;
    }
}
