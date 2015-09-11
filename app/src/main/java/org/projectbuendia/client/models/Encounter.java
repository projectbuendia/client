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

package org.projectbuendia.client.models;

import android.content.ContentValues;

import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.projectbuendia.client.net.Server;
import org.projectbuendia.client.net.json.JsonEncounter;
import org.projectbuendia.client.sync.providers.Contracts.Observations;
import org.projectbuendia.client.utils.Logger;

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
 * <p>NOTE: Because of lack of typing info from the server, {@link Encounter} attempts to
 * determine the most appropriate type, but this typing is not guaranteed to succeed; also,
 * currently only <code>DATE</code> and <code>UUID</code> (coded) types are supported.
 */
@Immutable
public class Encounter extends Base<String> {
    private static final Logger LOG = Logger.create();

    public final String patientUuid;
    public final @Nullable String encounterUuid;
    public final DateTime timestamp;
    public final Observation[] observations;
    public final String[] orderUuids;

    /**
     * Creates a new Encounter for the given patient.
     * @param patientUuid The UUID of the patient.
     * @param encounterUuid The UUID of this encounter, or null for encounters created on the client.
     * @param timestamp The encounter time.
     * @param observations An array of observations to include in the encounter.
     * @param orderUuids A list of UUIDs of the orders executed during this encounter.
     */
    public Encounter(
        String patientUuid,
        @Nullable String encounterUuid,
        DateTime timestamp,
        Observation[] observations,
        String[] orderUuids) {
        id = encounterUuid;
        this.patientUuid = patientUuid;
        this.encounterUuid = id;
        this.timestamp = timestamp;
        this.observations = observations == null ? new Observation[] {} : observations;
        this.orderUuids = orderUuids == null ? new String[] {} : orderUuids;
    }

    /** Serializes this into a {@link JSONObject}. */
    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put(Server.PATIENT_UUID_KEY, patientUuid);
        json.put(Server.ENCOUNTER_TIMESTAMP, timestamp.getMillis() / 1000);
        if (observations.length > 0) {
            JSONArray observationsJson = new JSONArray();
            for (Observation obs : observations) {
                JSONObject observationJson = new JSONObject();
                observationJson.put(Server.OBSERVATION_QUESTION_UUID, obs.conceptUuid);
                String valueKey = obs.type == Observation.Type.DATE ?
                        Server.OBSERVATION_ANSWER_DATE : Server.OBSERVATION_ANSWER_UUID;
                observationJson.put(valueKey, obs.value);
                observationsJson.put(observationJson);
            }
            json.put(Server.ENCOUNTER_OBSERVATIONS_KEY, observationsJson);
        }
        if (orderUuids.length > 0) {
            JSONArray orderUuidsJson = new JSONArray();
            for (String orderUuid : orderUuids) {
                orderUuidsJson.put(orderUuid);
            }
            json.put(Server.ENCOUNTER_ORDER_UUIDS, orderUuidsJson);
        }
        return json;
    }

    /** Represents a single observation within this encounter. */
    public static final class Observation {
        public final String conceptUuid;
        public final String value;
        public final Type type;

        /** Data type of the observation. */
        public enum Type {
            DATE,
            NON_DATE
        }

        /**
         * Produces a best guess for the type of a given value, since the server doesn't give us
         * typing information.
         */
        public static Type estimatedTypeFor(String value) {
            try {
                new DateTime(Long.parseLong(value));
                return Type.DATE;
            } catch (Exception e) {
                return Type.NON_DATE;
            }
        }

        public Observation(String conceptUuid, String value, Type type) {
            this.conceptUuid = conceptUuid;
            this.value = value;
            this.type = type;
        }
    }

    /**
     * Converts this instance of {@link Encounter} to an array of
     * {@link android.content.ContentValues} objects for insertion into a database or content
     * provider.
     */
    public ContentValues[] toContentValuesArray() {
        ContentValues[] cvs = new ContentValues[observations.length + orderUuids.length];
        long timestampSec = timestamp.getMillis() / 1000;
        for (int i = 0; i < observations.length; i++) {
            Observation obs = observations[i];
            ContentValues cv = new ContentValues();
            cv.put(Observations.CONCEPT_UUID, obs.conceptUuid);
            cv.put(Observations.ENCOUNTER_TIME, timestampSec);
            cv.put(Observations.ENCOUNTER_UUID, encounterUuid);
            cv.put(Observations.PATIENT_UUID, patientUuid);
            cv.put(Observations.VALUE, obs.value);
            cvs[i] = cv;
        }
        for (int i = 0; i < orderUuids.length; i++) {
            ContentValues cv = new ContentValues();
            cv.put(Observations.CONCEPT_UUID, AppModel.ORDER_EXECUTED_CONCEPT_UUID);
            cv.put(Observations.ENCOUNTER_TIME, timestampSec);
            cv.put(Observations.ENCOUNTER_UUID, encounterUuid);
            cv.put(Observations.PATIENT_UUID, patientUuid);
            cv.put(Observations.VALUE, orderUuids[i]);
            cvs[observations.length + i] = cv;
        }
        return cvs;
    }

    /**
     * Creates an instance of {@link Encounter} from a network
     * {@link JsonEncounter} object and corresponding patient UUID.
     */
    public static Encounter fromJson(String patientUuid, JsonEncounter encounter) {
        List<Observation> observations = new ArrayList<Observation>();
        if (encounter.observations != null) {
            for (Map.Entry<Object, Object> observation : encounter.observations.entrySet()) {
                observations.add(new Observation(
                    (String) observation.getKey(),
                    (String) observation.getValue(),
                    Observation.estimatedTypeFor((String) observation.getValue())
                ));
            }
        }
        return new Encounter(patientUuid, encounter.uuid, encounter.timestamp,
                observations.toArray(new Observation[observations.size()]), encounter.order_uuids);
    }
}
