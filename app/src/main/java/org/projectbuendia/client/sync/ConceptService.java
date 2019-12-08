package org.projectbuendia.client.sync;

import android.content.ContentResolver;
import android.database.Cursor;

import org.projectbuendia.client.App;
import org.projectbuendia.client.json.Datatype;
import org.projectbuendia.client.models.ConceptUuids;
import org.projectbuendia.client.providers.Contracts.Concepts;
import org.projectbuendia.client.utils.Intl;
import org.projectbuendia.client.utils.Utils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** An in-memory cache of concepts name and types, loaded from the local database. */
public class ConceptService {
    private final ContentResolver resolver;

    private Map<String, Datatype> types = null;
    private Map<String, Intl> names = null;

    public ConceptService(ContentResolver resolver) {
        this.resolver = resolver;
    }

    public synchronized Datatype getType(String uuid) {
        if (types == null) load();
        return types.get(uuid);
    }

    public synchronized String getName(String uuid) {
        return getName(uuid, App.getSettings().getLocale());
    }

    public synchronized String getName(String uuid, Locale locale) {
        if (names == null) load();
        Intl intl = names.get(uuid);
        return intl != null ? intl.get(locale) : Utils.compressUuid(uuid) + "?";
    }

    public synchronized void invalidate() {
        types = null;
        names = null;
    }

    private void load() {
        Map<String, Datatype> types = new HashMap<>();
        Map<String, Intl> names = new HashMap<>();

        try (Cursor c = resolver.query(Concepts.URI, null, null, null, null)) {
            while (c.moveToNext()) {
                String uuid = Utils.getString(c, Concepts.UUID);
                String type = Utils.getString(c, Concepts.TYPE);
                String name = Utils.getString(c, Concepts.NAME);
                try {
                    types.put(uuid, Datatype.valueOf(type));
                } catch (IllegalArgumentException e) {
                    continue;  // bad concept type name
                }
                names.put(uuid, new Intl(name));
            }
        }

        // Special case: yes and no concepts.
        names.put(ConceptUuids.YES_UUID, new Intl("Yes [fr:Oui]"));
        names.put(ConceptUuids.NO_UUID, new Intl("No [fr:Non]"));

        synchronized (this) {
            this.types = types;
            this.names = names;
        }
    }
}
