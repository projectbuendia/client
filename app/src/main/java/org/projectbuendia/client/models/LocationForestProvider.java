package org.projectbuendia.client.models;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;

import org.projectbuendia.client.AppSettings;
import org.projectbuendia.client.providers.Contracts;
import org.projectbuendia.client.providers.Contracts.LocationNames;
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
            Contracts.LocationNames.URI, true, locationChangeObserver);
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

    private LocationForest loadForest(Locale desiredLocale) {
        List<LocationForest.Record> records = new ArrayList<>();
        Uri uri = Contracts.Locations.URI;
        Map<String, String> namesByUuid = getNamesByLocationUuid(desiredLocale);
        Map<String, Integer> countsByUuid = getPatientCountsByLocationUuid();
        try (Cursor cursor = resolver.query(uri, null, null, null, null)) {
            while (cursor.moveToNext()) {
                String uuid = Utils.getString(cursor, Contracts.Locations.UUID);
                String parentUuid = Utils.getString(cursor, Contracts.Locations.PARENT_UUID);
                records.add(new LocationForest.Record(
                    uuid, parentUuid,
                    Utils.getOrDefault(namesByUuid, uuid, "?"),
                    Utils.getOrDefault(countsByUuid, uuid, 0)
                ));
            }
        }
        return new LocationForest(records);
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

    private Map<String, String> getNamesByLocationUuid(Locale desiredLocale) {
        Locale defaultLocale = AppSettings.getDefaultLocale();
        Uri uri = LocationNames.URI;
        Map<String, String> namesByLocationUuid = new HashMap<>();
        Map<String, Integer> scoresByLocationUuid = new HashMap<>();
        try (Cursor cursor = resolver.query(uri, null, null, null, null)) {
            while (cursor.moveToNext()) {
                String locationUuid = Utils.getString(cursor, LocationNames.LOCATION_UUID);
                String name = Utils.getString(cursor, LocationNames.NAME);
                Locale locale = Utils.toLocale(Utils.getString(cursor, LocationNames.LOCALE));
                int score = eq(locale, desiredLocale) ? 2 : eq(locale, defaultLocale) ? 1 : 0;
                if (score > Utils.getOrDefault(scoresByLocationUuid, locationUuid, 0)) {
                    namesByLocationUuid.put(locationUuid, name);
                    scoresByLocationUuid.put(locationUuid, score);
                }
            }
        }
        return namesByLocationUuid;
    }
}
