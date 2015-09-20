// Copyright 2015 The Project Buendia Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at: http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distrib-
// uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
// specific language governing permissions and limitations under the License.

package org.projectbuendia.client.models.converters;

import android.database.Cursor;

import org.joda.time.DateTime;
import org.projectbuendia.client.models.Encounter;
import org.projectbuendia.client.models.Encounter.Observation;
import org.projectbuendia.client.providers.Contracts.Observations;

import java.util.ArrayList;
import java.util.List;

/**
 * An {@link Converter} that converts {@link Encounter}s. Expects the {@link Cursor} to
 * contain only a single encounter, represented by multiple observations, with one observation per
 * row.
 * <p/>
 * <p>Unlike other {@link Converter}s, {@link EncounterConverter} must be instantiated
 * once per patient, since {@link Encounter} contains the patient's UUID as one of its fields,
 * which is not present in the database representation of an encounter.
 */
public class EncounterConverter implements Converter<Encounter> {
    private String mPatientUuid;

    public EncounterConverter(String patientUuid) {
        mPatientUuid = patientUuid;
    }

    @Override public Encounter fromCursor(Cursor cursor) {
        final String encounterUuid = cursor.getString(
            cursor.getColumnIndex(Observations.ENCOUNTER_UUID));
        final long millis = cursor.getLong(
            cursor.getColumnIndex(Observations.ENCOUNTER_MILLIS));
        List<Observation> observations = new ArrayList<>();
        cursor.move(-1);
        while (cursor.moveToNext()) {
            String value = cursor.getString(cursor.getColumnIndex(Observations.VALUE));
            observations.add(new Observation(
                cursor.getString(cursor.getColumnIndex(Observations.CONCEPT_UUID)),
                value, Observation.estimatedTypeFor(value)
            ));
        }
        return new Encounter(mPatientUuid, encounterUuid, new DateTime(millis),
            observations.toArray(new Observation[observations.size()]), null);
    }
}
