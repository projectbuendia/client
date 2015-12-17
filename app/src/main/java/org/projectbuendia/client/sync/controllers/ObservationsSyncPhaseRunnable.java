/*
 * Copyright 2015 The Project Buendia Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at: http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distrib-
 * uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
 * specific language governing permissions and limitations under the License.
 */

package org.projectbuendia.client.sync.controllers;

import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.SyncResult;
import android.net.Uri;
import android.os.RemoteException;

import org.projectbuendia.client.json.JsonObservation;
import org.projectbuendia.client.providers.Contracts;
import org.projectbuendia.client.providers.Contracts.Observations;
import org.projectbuendia.client.utils.Logger;

import java.util.ArrayList;

import static java.lang.String.format;

/**
 * Handles syncing observations. Uses an incremental sync mechanism - see
 * {@link IncrementalSyncPhaseRunnable} for details.
 */
public class ObservationsSyncPhaseRunnable extends IncrementalSyncPhaseRunnable<JsonObservation> {
    private static final Logger LOG = Logger.create();

    public ObservationsSyncPhaseRunnable() {
        super(
            "observations",
            Contracts.Table.OBSERVATIONS,
            JsonObservation.class);
    }

    @Override
    protected ArrayList<ContentProviderOperation> getUpdateOps(
        JsonObservation[] list, SyncResult syncResult) {
        int deletes = 0;
        int inserts = 0;
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        for (JsonObservation observation: list) {
            if (observation.voided) {
                Uri uri = Observations.CONTENT_URI.buildUpon().appendPath(observation.uuid).build();
                ops.add(ContentProviderOperation.newDelete(uri).build());
                deletes++;
            } else {
                ops.add(ContentProviderOperation.newInsert(Observations.CONTENT_URI)
                    .withValues(getObsValuesToInsert(observation)).build());
                inserts++;
            }
        }
        LOG.d("Observations processed! Inserts: %d, Deletes: %d", inserts, deletes);
        syncResult.stats.numInserts += inserts;
        syncResult.stats.numDeletes += deletes;
        return ops;
    }

    /** Converts an encounter data response into appropriate inserts in the encounters table. */
    public static ContentValues getObsValuesToInsert(
        JsonObservation observation) {
        ContentValues cvs = new ContentValues();
        cvs.put(Observations.UUID, observation.uuid);
        cvs.put(Observations.PATIENT_UUID, observation.patient_uuid);
        cvs.put(Observations.ENCOUNTER_UUID, observation.encounter_uuid);
        cvs.put(Observations.ENCOUNTER_MILLIS, observation.timestamp.getMillis());
        cvs.put(Observations.CONCEPT_UUID, observation.concept_uuid);
        cvs.put(Observations.VALUE, observation.value);

        return cvs;
    }

    @Override
    protected void afterSyncFinished(
        ContentResolver contentResolver,
        SyncResult syncResult,
        ContentProviderClient providerClient) throws RemoteException {
        // Remove all temporary observations now we have the real ones
        providerClient.delete(Observations.CONTENT_URI,
            format("%s IS NULL AND %s == 1", Observations.UUID, Observations.SUBMITTED),
            new String[0]);
    }
}