package org.projectbuendia.client.sync;

import android.content.ContentResolver;
import android.database.Cursor;

import org.projectbuendia.client.AppSettings;
import org.projectbuendia.client.json.ConceptType;
import org.projectbuendia.client.models.ConceptUuids;
import org.projectbuendia.client.providers.Contracts.ConceptNames;
import org.projectbuendia.client.providers.Contracts.Concepts;
import org.projectbuendia.client.utils.Utils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.projectbuendia.client.utils.Utils.eq;

/** An in-memory cache of concepts name and types, loaded from the local database. */
public class ConceptService {
    private final ContentResolver resolver;

    private Locale loadedLocale = null;
    private Map<String, String> names = null;
    private Map<String, ConceptType> types = null;

    public ConceptService(ContentResolver resolver) {
        this.resolver = resolver;
    }

    public ConceptType getType(String uuid) {
        if (types == null) {
            types = loadTypes(resolver);
        }
        return types.get(uuid);
    }

    public String getName(String uuid, Locale locale) {
        // TODO(ping): Internationalize concept names in a way that doesn't require
        // reloading all concepts just to get the default locale for one concept.
        locale = AppSettings.APP_DEFAULT_LOCALE;
        if (names == null || !eq(locale, loadedLocale)) {
            names = loadNames(resolver, locale);
            loadedLocale = locale;
        }
        String name = names.get(uuid);
        if (name == null) name = Utils.compressUuid(uuid) + "?";
        return name;
    }

    public void invalidate() {
        types = null;
        names = null;
    }

    public Locale getLocale() {
        return loadedLocale;
    }

    private static Map<String, ConceptType> loadTypes(ContentResolver resolver) {
        Map<String, ConceptType> result = new HashMap<>();
        try (Cursor c = resolver.query(
            Concepts.URI,
            new String[] {Concepts.UUID, Concepts.CONCEPT_TYPE},
            null, null, null
        )) {
            while (c.moveToNext()) {
                String uuid = c.getString(0);
                ConceptType type;
                try {
                    type = ConceptType.valueOf(c.getString(1));
                } catch (IllegalArgumentException e) {
                    continue;  // bad concept type name
                }
                result.put(uuid, type);
            }
        }

        // Special case: we know this is a date even if no fomrs or charts mention it.
        result.put(ConceptUuids.ADMISSION_DATE_UUID, ConceptType.DATE);

        return result;
    }

    private static Map<String, String> loadNames(ContentResolver resolver, Locale locale) {
        Map<String, String> result = new HashMap<>();
        String languageTag = Utils.toLanguageTag(locale);

        try (Cursor c = resolver.query(
            ConceptNames.URI,
            new String[] {ConceptNames.CONCEPT_UUID, ConceptNames.NAME},
            ConceptNames.LOCALE + " = ?", new String[] {languageTag}, null
        )) {
            while (c.moveToNext()) {
                result.put(c.getString(0), c.getString(1));
            }
        }
        return result;
    }
}
