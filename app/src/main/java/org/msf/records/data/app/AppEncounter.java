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

package org.msf.records.data.app;

import android.content.ContentValues;

import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.msf.records.net.Server;
import org.msf.records.net.model.Encounter;
import org.msf.records.sync.providers.Contracts;
import org.msf.records.utils.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * An encounter in the app model. Encounters contain one or more observations taken at a particular
 * timestamp. For more information on encounters and observations, see the official OpenMRS
 * documentation here:
 * <a href="https://wiki.openmrs.org/display/docs/Encounters+and+observations">
 *     https://wiki.openmrs.org/display/docs/Encounters+and+observations"
 * </a>
 *
 * <p>NOTE: Because of lack of typing info from the server, {@link AppEncounter} attempts to
 * determine the most appropriate type, but this typing is not guaranteed to succeed; also,
 * currently only <code>DATE</code> and <code>UUID</code> (coded) types are supported.
 */
@Immutable
public class AppEncounter extends AppTypeBase<String> {
    private static final Logger LOG = Logger.create();

    public final String patientUuid;
    public final String encounterUuid;
    public final DateTime timestamp;
    public final AppObservation[] observations;

    /**
     * Creates a new AppEncounter for the given patient.
     * @param patientUuid the patient under observation
     * @param encounterUuid id for this encounter--for encounters created on the client, use null
     * @param timestamp the encounter time
     * @param observations an array of observations to include in the encounter
     */
    public AppEncounter(
            String patientUuid,
            @Nullable String encounterUuid, // May not be known.
            DateTime timestamp,
            AppObservation[] observations) {
        id = encounterUuid;
        this.patientUuid = patientUuid;
        this.encounterUuid = id;
        this.timestamp = timestamp;
        this.observations = observations;
    }

    /**
     * Serializes this into a {@link JSONObject}.
     */
    public boolean toJson(JSONObject json) {
        try {
            JSONArray observationsJson = new JSONArray();
            for (AppObservation observation : observations) {
                JSONObject observationJson = new JSONObject();
                observationJson.put(Server.PATIENT_QUESTION_UUID, observation.conceptUuid);
                observationJson.put(observation.serverType(), observation.value);
                observationsJson.put(observationJson);
            }
            json.put(Server.PATIENT_OBSERVATIONS_KEY, observationsJson);
            json.put(Server.PATIENT_UUID_KEY, patientUuid);
            json.put(Server.PATIENT_OBSERVATIONS_TIMESTAMP, timestamp.getMillis() / 1000);
            return true;
        } catch (JSONException e) {
            LOG.e("Error constructing encounter JSON", e);
            return false;
        }
    }

    /**
     * Represents a single observation within this encounter.
     */
    public static final class AppObservation {
        public final String conceptUuid;
        public final String value;
        public final Type type;

        /**
         * Datatype of the observation.
         */
        public enum Type {
            DATE,
            UUID
        }

        /**
         * Produces a best guess for the type of a given value, since the server doesn't give us
         * typing information.
         */
        public static Type estimatedTypeFor(String value) {
            try {
                long longValue = Long.parseLong(value);
                DateTime dateTime = new DateTime(longValue);
                return Type.DATE;
            } catch (Exception e) {
                // Intentionally blank -- value is not numeric or not a date.
            }

            return Type.UUID;
        }

        /**
         * Returns the string used to represent the datatype of this observation on the server.
         */
        public String serverType() {
            switch (type) {
                case DATE:
                    return Server.PATIENT_ANSWER_DATE;
                case UUID:
                    return Server.PATIENT_ANSWER_UUID;
                default:
                    // Intentionally blank.
            }
            throw new IllegalArgumentException("Invalid type: " + type.toString());
        }

        /**
         * Creates a new observation.
         * @param conceptUuid UUID of the observation concept
         * @param value value of the observation
         * @param type datatype of the observation value
         */
        public AppObservation(String conceptUuid, String value, Type type) {
            this.conceptUuid = conceptUuid;
            this.value = value;
            this.type = type;
        }
    }

    /**
     * Converts this instance of {@link AppEncounter} to a
     * {@link android.content.ContentValues} object for insertion into a database or content
     * provider.
     */
    public ContentValues[] toContentValuesArray() {
        ContentValues[] valuesArray = new ContentValues[observations.length];
        long timestampSec = timestamp.getMillis() / 1000;
        for (int i = 0; i < observations.length; i++) {
            AppObservation observation = observations[i];
            ContentValues contentValues = new ContentValues();
            contentValues.put(Contracts.ObservationColumns.CONCEPT_UUID, observation.conceptUuid);
            contentValues.put(Contracts.ObservationColumns.ENCOUNTER_TIME, timestampSec);
            contentValues.put(Contracts.ObservationColumns.ENCOUNTER_UUID, encounterUuid);
            contentValues.put(Contracts.ObservationColumns.PATIENT_UUID, patientUuid);
            contentValues.put(Contracts.ObservationColumns.VALUE, observation.value);
            valuesArray[i] = contentValues;
        }

        return valuesArray;
    }

    /**
     * Creates an instance of {@link AppEncounter} from a network
     * {@link org.msf.records.net.model.Encounter} object and corresponding patient UUID.
     */
    public static AppEncounter fromNet(String patientUuid, Encounter encounter) {
        List<AppObservation> observationList = new ArrayList<AppObservation>();
        for (Map.Entry<Object, Object> observation : encounter.observations.entrySet()) {
            observationList.add(new AppObservation(
                    (String)observation.getKey(),
                    (String)observation.getValue(),
                    AppObservation.estimatedTypeFor((String)observation.getValue())
            ));
        }
        AppObservation[] observations = new AppObservation[observationList.size()];
        observationList.toArray(observations);

        return new AppEncounter(patientUuid, encounter.uuid, encounter.timestamp, observations);
    }
}
