package org.projectbuendia.client.utils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A localized string, made from a string of the form "cat [fr:chat] [es:gato]". */
public class Loc {
    private static final Pattern BRACKETED_PATTERN = Pattern.compile("\\[(.*?)\\]");

    protected final String base;
    protected final Map<String, String> options;

    public Loc(String packed) {
        if (packed == null) packed = "";
        base = BRACKETED_PATTERN.matcher(packed).replaceAll("").trim();
        options = new HashMap<>();
        Matcher matcher = BRACKETED_PATTERN.matcher(packed);
        for (int pos = 0; matcher.find(pos); pos = matcher.end(1)) {
            String[] parts = Utils.splitFields(matcher.group(1), ":", 2);
            options.put(parts[0], parts[1]);
        }
    }

    public Loc(String base, Map<String, String> options) {
        this.base = base;
        this.options = options;
    }

    public String get(String languageTag) {
        return get(Utils.toLocale(languageTag));
    }

    public String get(Locale locale) {
        if (options == null || options.isEmpty()) return base;

        String tag = Utils.toLanguageTag(locale);
        if (options.containsKey(tag)) return options.get(tag);

        String lang = locale.getLanguage();
        String region = locale.getCountry();
        String variant = locale.getVariant();
        tag = Utils.toLanguageTag(new Locale(lang, region, variant));
        if (options.containsKey(tag)) return options.get(tag);
        tag = Utils.toLanguageTag(new Locale(lang, region));
        if (options.containsKey(tag)) return options.get(tag);
        tag = Utils.toLanguageTag(new Locale(lang));
        if (options.containsKey(tag)) return options.get(tag);

        return base;
    }

    public static Loc[] newArray(String... strings) {
        Loc[] locs = new Loc[strings.length];
        for (int i = 0; i < locs.length; i++) {
            locs[i] = new Loc(strings[i]);
        }
        return locs;
    }
}
