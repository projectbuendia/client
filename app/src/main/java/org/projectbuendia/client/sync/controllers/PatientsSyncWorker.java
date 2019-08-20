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

import org.projectbuendia.client.App;
import org.projectbuendia.client.json.JsonPatient;
import org.projectbuendia.client.models.Patient;
import org.projectbuendia.client.providers.Contracts;
import org.projectbuendia.client.utils.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles syncing patients. Uses an incremental sync mechanism - see
 * {@link IncrementalSyncWorker} for details.
 */
public class PatientsSyncWorker extends IncrementalSyncWorker<JsonPatient> {
    private static final Logger LOG = Logger.create();
    private List<String> updatedPatientUuids = new ArrayList<>();

    public PatientsSyncWorker() {
        super("patients", Contracts.Table.PATIENTS, JsonPatient.class);
    }

    @Override public void initialize(
        ContentResolver resolver, SyncResult result, ContentProviderClient client) {
        updatedPatientUuids.clear();
    }


    @Override protected ArrayList<ContentProviderOperation> getUpdateOps(
        JsonPatient[] patients, SyncResult syncResult) {
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        int numInserts = 0;
        int numDeletes = 0;
        for (JsonPatient patient : patients) {
            if (patient.voided) {
                numDeletes++;
                ops.add(makeDeleteOpForPatientUuid(patient.uuid));
            } else {
                numInserts++;
                ops.add(makeInsertOpForPatient(patient));
            }
            updatedPatientUuids.add(patient.uuid);
        }
        LOG.d("Patients: %d inserts, %d deletes", numInserts, numDeletes);
        syncResult.stats.numInserts += numInserts;
        syncResult.stats.numDeletes += numDeletes;
        return ops;
    }

    private static ContentProviderOperation makeInsertOpForPatient(JsonPatient patient) {
        return ContentProviderOperation.newInsert(Contracts.Patients.URI)
                .withValues(Patient.fromJson(patient).toContentValues()).build();
    }

    private static ContentProviderOperation makeDeleteOpForPatientUuid(String uuid) {
        Uri uri = Contracts.Patients.URI.buildUpon().appendPath(uuid).build();
        return ContentProviderOperation.newDelete(uri).build();
    }

    @Override public void finalize(
        ContentResolver resolver, SyncResult result, ContentProviderClient client) {
        for (String uuid : updatedPatientUuids) {
            App.getModel().denormalizeObservations(App.getCrudEventBus(), uuid);
        }
        if (result.stats.numInserts + result.stats.numDeletes > 0) {
            resolver.notifyChange(Contracts.Patients.URI, null, false);
        }
    }
}
