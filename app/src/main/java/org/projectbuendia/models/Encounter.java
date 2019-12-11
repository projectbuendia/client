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

package org.projectbuendia.models;

import android.content.ContentValues;
import android.database.Cursor;

import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.projectbuendia.client.json.Datatype;
import org.projectbuendia.client.json.JsonEncounter;
import org.projectbuendia.client.json.JsonObservation;
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
    public final String providerUuid;
    public final DateTime time;
    public final Obs[] observations;

    public Encounter(@Nullable String uuid, String patientUuid, String providerUuid,
                     DateTime time, Obs[] observations) {
        super(uuid);
        this.patientUuid = patientUuid;
        this.providerUuid = providerUuid;
        this.time = time;
        this.observations = Utils.orDefault(observations, new Obs[0]);
    }

    public static Encounter fromJson(JsonEncounter encounter) {
        List<Obs> observations = new ArrayList<>();
        if (encounter.observations != null) {
            for (JsonObservation obs : encounter.observations) {
                observations.add(new Obs(
                    obs.uuid, encounter.uuid, encounter.patient_uuid, encounter.provider_uuid,
                    obs.concept_uuid, obs.type, obs.time, obs.order_uuid, obs.getValueAsString(), null
                ));
            }
        }
        return new Encounter(encounter.uuid, encounter.patient_uuid, encounter.provider_uuid,
            encounter.time, observations.toArray(new Obs[0]));
    }

    /** Serializes this into a {@link JSONObject}. */
    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("patient_uuid", patientUuid);
        json.put("time", Utils.formatUtc8601(time));
        if (observations.length > 0) {
            JSONArray jsonObsArray = new JSONArray();
            for (Obs obs : observations) {
                jsonObsArray.put(obs.toJson());
            }
            json.put("observations", jsonObsArray);
        }
        return json;
    }

    /**
     * Converts this Encounter to an array of ContentValues objects for
     * insertion into a database or content provider.
     */
    public ContentValues[] toContentValuesArray() {
        ContentValues[] cvs = new ContentValues[observations.length];
        int i = 0;
        for (Obs obs : observations) {
            ContentValues cv = new ContentValues();
            cv.put(Observations.UUID, obs.uuid);
            cv.put(Observations.ENCOUNTER_UUID, uuid);
            cv.put(Observations.PATIENT_UUID, patientUuid);
            cv.put(Observations.PROVIDER_UUID, providerUuid);
            cv.put(Observations.CONCEPT_UUID, obs.conceptUuid);
            cv.put(Observations.TYPE, obs.type.name());
            cv.put(Observations.MILLIS, time.getMillis());
            cv.put(Observations.ORDER_UUID, obs.orderUuid);
            cv.put(Observations.VALUE, obs.value);
            cvs[i++] = cv;
        }
        return cvs;
    }

    /**
     * A CursorLoader that loads Encounters.  Expects the Cursor to contain only
     * a single encounter, represented by multiple observations, with one observation per row.
     */
    public static Encounter load(Cursor cursor) {
        String encounterUuid = null;
        String patientUuid = null;
        String providerUuid = null;
        DateTime time = null;
        List<Obs> observations = new ArrayList<>();
        do { // cursor is already at the first matching observation
            String uuid = Utils.getString(cursor, Observations.UUID);
            encounterUuid = Utils.getString(cursor, Observations.ENCOUNTER_UUID);
            patientUuid = Utils.getString(cursor, Observations.PATIENT_UUID);
            providerUuid = Utils.getString(cursor, Observations.PROVIDER_UUID);
            String conceptUuid = Utils.getString(cursor, Observations.CONCEPT_UUID);
            Datatype type = Datatype.valueOf(Utils.getString(cursor, Observations.TYPE));
            time = Utils.getDateTime(cursor, Observations.MILLIS);
            String orderUuid = Utils.getString(cursor, Observations.ORDER_UUID);
            String value = Utils.getString(cursor, Observations.VALUE);
            observations.add(new Obs(
                uuid, encounterUuid, patientUuid, providerUuid,
                conceptUuid, type, time, orderUuid, value, null
            ));
        } while (cursor.moveToNext());
        if (encounterUuid != null && patientUuid != null && time != null) {
            return new Encounter(encounterUuid, patientUuid, providerUuid,
                time, observations.toArray(new Obs[observations.size()]));
        }
        return null; // PATIENT_UUID should never be null, so this should never happen
    }

    /** For developer use only, to help fabricate a response from a nonexistent server. */
    public Encounter withUuid(String uuid) {
        return new Encounter(uuid, patientUuid, providerUuid, time, observations);
    }
}
