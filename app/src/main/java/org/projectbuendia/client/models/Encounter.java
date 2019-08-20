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
import android.database.Cursor;

import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.projectbuendia.client.json.JsonEncounter;
import org.projectbuendia.client.net.Server;
import org.projectbuendia.client.providers.Contracts.Observations;
import org.projectbuendia.client.utils.Logger;
import org.projectbuendia.client.utils.Utils;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * An encounter in the app model. Encounters contain one or more observations taken at a particular
 * timestamp. For more information on encounters and observations, see the official OpenMRS
 * documentation here:
 * <a href="https://wiki.openmrs.org/display/docs/Encounters+and+observations">
 * https://wiki.openmrs.org/display/docs/Encounters+and+observations"
 * </a>
 * <p/>
 * <p>NOTE: Because of lack of typing info from the server, {@link Encounter} attempts to
 * determine the most appropriate type, but this typing is not guaranteed to succeed; also,
 * currently only <code>DATE</code> and <code>UUID</code> (coded) types are supported.
 */
@Immutable
public class Encounter extends Model {
    private static final Logger LOG = Logger.create();

    public final String patientUuid;
    public final DateTime timestamp;
    public final Observation[] observations;
    public final String[] orderUuids;

    public Encounter(
        @Nullable String uuid, String patientUuid,
        DateTime timestamp, Observation[] observations, String[] orderUuids) {
        super(uuid);
        this.patientUuid = patientUuid;
        this.timestamp = timestamp;
        this.observations = Utils.orDefault(observations, new Observation[0]);
        this.orderUuids = Utils.orDefault(orderUuids, new String[0]);
    }

    public static Encounter fromJson(JsonEncounter encounter) {
        List<Observation> observations = new ArrayList<>();
        if (encounter.observations != null) {
            for (String key : encounter.observations.keySet()) {
                String value = Utils.toNullableString(encounter.observations.get(key));
                observations.add(new Observation(key, value, Observation.estimatedTypeFor(value)));
            }
        }
        return new Encounter(encounter.uuid, encounter.patient_uuid, encounter.timestamp,
            observations.toArray(new Observation[0]), encounter.order_uuids);
    }

    /** Serializes this into a {@link JSONObject}. */
    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put(Server.PATIENT_UUID_KEY, patientUuid);
        json.put(Server.ENCOUNTER_TIMESTAMP, timestamp.getMillis()/1000);
        if (observations.length > 0) {
            JSONArray jsonObsArray = new JSONArray();
            for (Observation obs : observations) {
                JSONObject jsonObs = new JSONObject();
                jsonObs.put(Server.OBS_QUESTION_UUID, obs.conceptUuid);
                String valueKey = obs.type == Observation.Type.DATE ?
                    Server.OBS_ANSWER_DATE : Server.OBS_ANSWER_UUID;
                jsonObs.put(valueKey, obs.value);
                jsonObsArray.put(jsonObs);
            }
            json.put(Server.ENCOUNTER_OBSERVATIONS_KEY, jsonObsArray);
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

    /**
     * Converts this instance of {@link Encounter} to an array of
     * {@link android.content.ContentValues} objects for insertion into a database or content
     * provider.
     */
    public ContentValues[] toContentValuesArray() {
        ContentValues[] cvs = new ContentValues[observations.length + orderUuids.length];
        for (int i = 0; i < observations.length; i++) {
            Observation obs = observations[i];
            ContentValues cv = new ContentValues();
            cv.put(Observations.CONCEPT_UUID, obs.conceptUuid);
            cv.put(Observations.ENCOUNTER_MILLIS, timestamp.getMillis());
            cv.put(Observations.ENCOUNTER_UUID, uuid);
            cv.put(Observations.PATIENT_UUID, patientUuid);
            cv.put(Observations.VALUE, obs.value);
            cvs[i] = cv;
        }
        for (int i = 0; i < orderUuids.length; i++) {
            ContentValues cv = new ContentValues();
            cv.put(Observations.CONCEPT_UUID, ConceptUuids.ORDER_EXECUTED_UUID);
            cv.put(Observations.ENCOUNTER_MILLIS, timestamp.getMillis());
            cv.put(Observations.ENCOUNTER_UUID, uuid);
            cv.put(Observations.PATIENT_UUID, patientUuid);
            cv.put(Observations.VALUE, orderUuids[i]);
            cvs[observations.length + i] = cv;
        }
        return cvs;
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

        public Observation(String conceptUuid, String value, Type type) {
            this.conceptUuid = conceptUuid;
            this.value = value;
            this.type = type;
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
    }

    /**
     * A CursorLoader that loads Encounters.  Expects the Cursor to contain only
     * a single encounter, represented by multiple observations, with one observation per row.
     */
    public static class Loader implements CursorLoader<Encounter> {
        @Override public Encounter fromCursor(Cursor cursor) {
            final String encounterUuid = Utils.getString(cursor, Observations.ENCOUNTER_UUID);
            final long millis = Utils.getLong(cursor, Observations.ENCOUNTER_MILLIS);
            String patientUuid = null;
            List<Observation> observations = new ArrayList<>();
            cursor.move(-1); // TODO(ping): Why?
            while (cursor.moveToNext()) {
                patientUuid = Utils.getString(cursor, Observations.PATIENT_UUID);
                String conceptUuid = Utils.getString(cursor, Observations.CONCEPT_UUID);
                String value = Utils.getString(cursor, Observations.VALUE);
                observations.add(new Observation(
                    Utils.getString(cursor, Observations.CONCEPT_UUID),
                    value, Observation.estimatedTypeFor(value)
                ));
            }
            if (patientUuid != null) {
                return new Encounter(encounterUuid, patientUuid, new DateTime(millis),
                    observations.toArray(new Observation[observations.size()]), null);
            }
            return null; // PATIENT_UUID should never be null, so this should never happen
        }
    }
}
