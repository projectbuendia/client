package org.projectbuendia.client.models;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;

import org.projectbuendia.client.providers.Contracts;
import org.projectbuendia.client.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nonnull;

import static org.projectbuendia.client.utils.Utils.eq;

public class LocationForestProvider {
    private LocationForest currentForest = null;
    private Locale currentLocale = null;
    private Runnable onForestReplacedListener = null;

    private final ContentResolver resolver;
    private final ContentObserver locationChangeObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange) {
            currentForest = null;
            if (onForestReplacedListener != null) {
                onForestReplacedListener.run();
            }
        }
    };
    private final ContentObserver patientChangeObserver = new ContentObserver(new Handler()) {
        public void onChange(boolean selfChange) {
            if (currentForest != null) {
                currentForest.updatePatientCounts(getPatientCountsByLocationUuid());
            }
        }
    };

    public LocationForestProvider(ContentResolver resolver) {
        this.resolver = resolver;
        resolver.registerContentObserver(
            Contracts.Locations.URI, true, locationChangeObserver);
        resolver.registerContentObserver(
            Contracts.Patients.URI, true, patientChangeObserver);
    }

    public void dispose() {
        resolver.unregisterContentObserver(locationChangeObserver);
        resolver.unregisterContentObserver(patientChangeObserver);
    }

    public @Nonnull LocationForest getForest(Locale locale) {
        if (currentForest != null && eq(currentLocale, locale)) {
            return currentForest;
        }
        currentForest = loadForest(locale);
        currentLocale = locale;
        return currentForest;
    }

    public void setOnForestReplacedListener(Runnable listener) {
        onForestReplacedListener = listener;
    }

    private LocationForest loadForest(Locale locale) {
        List<LocationForest.Record> records = new ArrayList<>();
        Uri uri = Contracts.Locations.URI;
        Map<String, Integer> countsByUuid = getPatientCountsByLocationUuid();
        try (Cursor cursor = resolver.query(uri, null, null, null, null)) {
            while (cursor.moveToNext()) {
                String uuid = Utils.getString(cursor, Contracts.Locations.UUID, "");
                String name = Utils.getString(cursor, Contracts.Locations.NAME, "?");
                String parentUuid = Utils.getString(cursor, Contracts.Locations.PARENT_UUID);
                records.add(new LocationForest.Record(
                    uuid, parentUuid, name, Utils.getOrDefault(countsByUuid, uuid, 0)
                ));
            }
        }
        return new LocationForest(records, locale);
    }

    private Map<String, Integer> getPatientCountsByLocationUuid() {
        Uri uri = Contracts.PatientCounts.URI;
        Map<String, Integer> countsByLocationUuid = new HashMap<>();
        try (Cursor cursor = resolver.query(uri, null, null, null, null)) {
            while (cursor.moveToNext()) {
                String locationUuid = Utils.getString(cursor, Contracts.PatientCounts.LOCATION_UUID);
                Integer count = Utils.getInt(cursor, Contracts.PatientCounts.PATIENT_COUNT);
                countsByLocationUuid.put(locationUuid, count);
            }
        }
        return countsByLocationUuid;
    }
}
