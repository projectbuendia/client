package org.projectbuendia.client.sync;

import android.content.ContentResolver;
import android.database.Cursor;

import org.projectbuendia.client.App;
import org.projectbuendia.client.json.Datatype;
import org.projectbuendia.client.models.ConceptUuids;
import org.projectbuendia.client.providers.Contracts.Concepts;
import org.projectbuendia.client.utils.Loc;
import org.projectbuendia.client.utils.Utils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** An in-memory cache of concepts name and types, loaded from the local database. */
public class ConceptService {
    private final ContentResolver resolver;

    private Map<String, Datatype> types = null;
    private Map<String, Loc> names = null;

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
        Loc loc = names.get(uuid);
        return loc != null ? loc.get(locale) : Utils.compressUuid(uuid) + "?";
    }

    public synchronized void invalidate() {
        types = null;
        names = null;
    }

    private void load() {
        Map<String, Datatype> types = new HashMap<>();
        Map<String, Loc> names = new HashMap<>();

        // Special case: these are dates even if no forms or charts mention them.
        types.put(ConceptUuids.ADMISSION_DATE_UUID, Datatype.DATE);
        types.put(ConceptUuids.FIRST_SYMPTOM_DATE_UUID, Datatype.DATE);

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
                names.put(uuid, new Loc(name));
            }
        }

        synchronized (this) {
            this.types = types;
            this.names = names;
        }
    }
}
