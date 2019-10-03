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
import android.content.SyncResult;
import android.net.Uri;
import android.os.RemoteException;

import org.projectbuendia.client.App;
import org.projectbuendia.client.json.JsonObservation;
import org.projectbuendia.client.models.tasks.DenormalizeObsTask;
import org.projectbuendia.client.providers.Contracts;
import org.projectbuendia.client.providers.Contracts.Observations;
import org.projectbuendia.client.utils.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Handles syncing observations. Uses an incremental sync mechanism - see
 * {@link IncrementalSyncWorker} for details.
 */
public class ObservationsSyncWorker extends IncrementalSyncWorker<JsonObservation> {
    private static final Logger LOG = Logger.create();
    private Set<String> patientUuidsToUpdate = new HashSet<>();

    public ObservationsSyncWorker() {
        super("observations", Contracts.Table.OBSERVATIONS, JsonObservation.class);
    }

    @Override public void initialize(
        ContentResolver resolver, SyncResult result, ContentProviderClient client) {
        patientUuidsToUpdate.clear();
    }

    @Override
    protected ArrayList<ContentProviderOperation> getUpdateOps(
            JsonObservation[] observations, SyncResult syncResult) {
        int numInserts = 0;
        int numDeletes = 0;
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        for (JsonObservation observation : observations) {
            if (observation.voided) {
                Uri uri = Observations.URI.buildUpon().appendPath(observation.uuid).build();
                ops.add(ContentProviderOperation.newDelete(uri).build());
                numDeletes++;
            } else {
                ops.add(ContentProviderOperation.newInsert(Observations.URI)
                        .withValues(observation.toContentValues()).build());
                numInserts++;
            }
            if (DenormalizeObsTask.needsDenormalization(observation.concept_uuid)) {
                patientUuidsToUpdate.add(observation.patient_uuid);
            }
        }
        LOG.d("Observations: %d inserts, %d deletes", numInserts, numDeletes);
        syncResult.stats.numInserts += numInserts;
        syncResult.stats.numDeletes += numDeletes;
        return ops;
    }

    @Override public void finalize(
        ContentResolver resolver, SyncResult result, ContentProviderClient client
    ) throws RemoteException {
        // Remove all temporary observations now we have the real ones
        client.delete(Observations.URI, Observations.UUID + " IS NULL", new String[0]);
        for (String uuid : patientUuidsToUpdate) {
            App.getModel().denormalizeObservations(App.getCrudEventBus(), uuid);
        }
    }
}
