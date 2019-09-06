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
import org.projectbuendia.client.json.ConceptType;
import org.projectbuendia.client.json.JsonEncounter;
import org.projectbuendia.client.net.Server;
import org.projectbuendia.client.providers.Contracts.Observations;
import org.projectbuendia.client.utils.Logger;
import org.projectbuendia.client.utils.Utils;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import static org.projectbuendia.client.utils.Utils.eq;

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
    public final Obs[] observations;
    public final String[] orderUuids;

    public Encounter(
        @Nullable String uuid, String patientUuid,
        DateTime timestamp, Obs[] observations, String[] orderUuids) {
        super(uuid);
        this.patientUuid = patientUuid;
        this.timestamp = timestamp;
        this.observations = Utils.orDefault(observations, new Obs[0]);
        this.orderUuids = Utils.orDefault(orderUuids, new String[0]);
    }

    public static Encounter fromJson(JsonEncounter encounter) {
        List<Obs> observations = new ArrayList<>();
        if (encounter.observations != null) {
            long millis = encounter.timestamp.getMillis();
            for (String key : encounter.observations.keySet()) {
                String value = Utils.toNullableString(encounter.observations.get(key));
                // TODO(ping): These observations will be undeletable until the next
                // sync replaces them with observations that have UUIDs.  For these
                // observations to be deletable immediately, we would need the server
                // to return them in the Encounter response with individual UUIDs.
                observations.add(new Obs(null, millis, key, estimatedTypeFor(key, value), value, null));
            }
        }
        return new Encounter(encounter.uuid, encounter.patient_uuid, encounter.timestamp,
            observations.toArray(new Obs[0]), encounter.order_uuids);
    }

    /** Serializes this into a {@link JSONObject}. */
    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put(Server.PATIENT_UUID_KEY, patientUuid);
        json.put(Server.ENCOUNTER_TIMESTAMP, timestamp.getMillis()/1000);
        if (observations.length > 0) {
            JSONArray jsonObsArray = new JSONArray();
            for (Obs obs : observations) {
                jsonObsArray.put(obs.toJson());
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
            Obs obs = observations[i];
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

    /** A hacky attempt to guess the type of an observation value. :( */
    public static ConceptType estimatedTypeFor(String conceptUuid, String value) {
        if (eq(conceptUuid, ConceptUuids.PLACEMENT_UUID)) return ConceptType.TEXT;
        try {
            new DateTime(Long.parseLong(value));
            return ConceptType.DATE;
        } catch (Exception e) {
            return ConceptType.CODED;
        }
    }

    /**
     * A CursorLoader that loads Encounters.  Expects the Cursor to contain only
     * a single encounter, represented by multiple observations, with one observation per row.
     */
    public static Encounter load(Cursor cursor) {
        final String encounterUuid = Utils.getString(cursor, Observations.ENCOUNTER_UUID);
        final long millis = Utils.getLong(cursor, Observations.ENCOUNTER_MILLIS);
        String uuid = null;
        String patientUuid = null;
        List<Obs> observations = new ArrayList<>();
        cursor.move(-1); // TODO(ping): Why?
        while (cursor.moveToNext()) {
            uuid = Utils.getString(cursor, Observations.UUID);
            patientUuid = Utils.getString(cursor, Observations.PATIENT_UUID);
            String conceptUuid = Utils.getString(cursor, Observations.CONCEPT_UUID);
            String value = Utils.getString(cursor, Observations.VALUE);
            observations.add(new Obs(
                uuid, millis, conceptUuid, estimatedTypeFor(conceptUuid, value), value, null
            ));
        }
        if (patientUuid != null) {
            return new Encounter(encounterUuid, patientUuid, new DateTime(millis),
                observations.toArray(new Obs[observations.size()]), null);
        }
        return null; // PATIENT_UUID should never be null, so this should never happen
    }

    /** For developer use only, to help fabricate a response from a nonexistent server. */
    public Encounter withUuid(String uuid) {
        return new Encounter(uuid, patientUuid, timestamp, observations, orderUuids);
    }

}
