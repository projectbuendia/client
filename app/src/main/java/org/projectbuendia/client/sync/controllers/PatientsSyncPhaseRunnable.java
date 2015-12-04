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

import org.projectbuendia.client.json.JsonPatient;
import org.projectbuendia.client.models.Patient;
import org.projectbuendia.client.providers.Contracts;

import java.util.ArrayList;

/**
 * Handles syncing patients. Uses an incremental sync mechanism - see
 * {@link IncrementalSyncPhaseRunnable} for details.
 */
public class PatientsSyncPhaseRunnable extends IncrementalSyncPhaseRunnable<JsonPatient> {

    public PatientsSyncPhaseRunnable() {
        super(
                "patients",
                Contracts.Table.PATIENTS,
                JsonPatient.class);
    }

    @Override
    protected ArrayList<ContentProviderOperation> getUpdateOps(
            JsonPatient[] list, SyncResult syncResult) {
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        for (JsonPatient patient : list) {
            if (patient.voided) {
                syncResult.stats.numDeletes++;
                ops.add(makeDeleteOpForPatientUuid(patient.uuid));
            } else {
                syncResult.stats.numInserts++;
                ops.add(makeInsertOpForPatient(patient));
            }
        }

        return ops;
    }

    private static ContentProviderOperation makeInsertOpForPatient(JsonPatient patient) {
        return ContentProviderOperation.newInsert(Contracts.Patients.CONTENT_URI)
                .withValues(Patient.fromJson(patient).toContentValues()).build();
    }

    private static ContentProviderOperation makeDeleteOpForPatientUuid(String uuid) {
        Uri uri = Contracts.Patients.CONTENT_URI.buildUpon().appendPath(uuid).build();
        return ContentProviderOperation.newDelete(uri).build();
    }

    @Override
    protected void afterSyncFinished(
            ContentResolver contentResolver,
            SyncResult syncResult,
            ContentProviderClient providerClient) throws Throwable {
        contentResolver.notifyChange(Contracts.Patients.CONTENT_URI, null, false);
    }
}
