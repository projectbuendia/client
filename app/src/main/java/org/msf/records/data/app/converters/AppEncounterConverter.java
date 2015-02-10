package org.msf.records.data.app.converters;

import android.database.Cursor;

import org.joda.time.DateTime;
import org.msf.records.data.app.AppEncounter;
import org.msf.records.sync.providers.Contracts;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link org.msf.records.data.app.converters.AppTypeConverter} that converts
 * {@link org.msf.records.data.app.AppEncounter}s. Expects the {@link Cursor} to contain only a
 * single encounter, represented by multiple observations, one per row.
 */
public class AppEncounterConverter implements AppTypeConverter<AppEncounter> {
    private String mPatientUuid;

    public AppEncounterConverter(String patientUuid) {
        mPatientUuid = patientUuid;
    }

    @Override
    public AppEncounter fromCursor(Cursor cursor) {
        String encounterUuid = cursor.getString(
                cursor.getColumnIndex(Contracts.ObservationColumns.ENCOUNTER_UUID));
        long timestamp = cursor.getLong(
                cursor.getColumnIndex(Contracts.ObservationColumns.ENCOUNTER_TIME));
        DateTime dateTime = new DateTime(timestamp);
        List<AppEncounter.AppObservation> observationList = new ArrayList<>();
        cursor.move(0);
        while (cursor.moveToNext()) {
            String value =
                    cursor.getString((cursor.getColumnIndex(Contracts.ObservationColumns.VALUE)));
            AppEncounter.AppObservation.Type type =
                    AppEncounter.AppObservation.estimatedTypeFor(value);
            observationList.add(new AppEncounter.AppObservation(
                    cursor.getString((cursor.getColumnIndex(Contracts.ObservationColumns.CONCEPT_UUID))),
                    value,
                    type
            ));
        }
        AppEncounter.AppObservation[] observations =
                new AppEncounter.AppObservation[observationList.size()];
        observationList.toArray(observations);

        return new AppEncounter(mPatientUuid, encounterUuid, dateTime, observations);
    }
}
