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

package org.projectbuendia.models.tasks;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;

import com.android.volley.toolbox.RequestFuture;

import org.projectbuendia.client.events.CrudEventBus;
import org.projectbuendia.client.events.data.ItemLoadFailedEvent;
import org.projectbuendia.client.events.data.ItemLoadedEvent;
import org.projectbuendia.client.filter.db.patient.UuidFilter;
import org.projectbuendia.client.json.JsonPatient;
import org.projectbuendia.models.Patient;
import org.projectbuendia.client.net.OpenMrsServer;
import org.projectbuendia.client.net.Server;
import org.projectbuendia.client.providers.Contracts.Patients;
import org.projectbuendia.client.utils.Logger;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * An {@link AsyncTask} that downloads one specific patient from the server and
 * stores it locally (unlike sync, which ensures all patients are stored locally).
 * <p/>
 * <p>Posts an {@link ItemLoadedEvent} or an {@link ItemLoadFailedEvent} on
 * the given {@link CrudEventBus} to indicate success or failure.
 */
public class FetchPatientTask extends AsyncTask<Void, Void, ItemLoadFailedEvent> {

    private static final Logger LOG = Logger.create();

    private final TaskFactory mTaskFactory;
    private final Server mServer;
    private final ContentResolver mContentResolver;
    private final String mPatientId;
    private final CrudEventBus mBus;

    private String mUuid;

    /** Creates a new {@link FetchPatientTask}. */
    public FetchPatientTask(
        TaskFactory taskFactory,
        Server server,
        ContentResolver contentResolver,
        String patientId,
        CrudEventBus bus) {
        mTaskFactory = taskFactory;
        mServer = server;
        mContentResolver = contentResolver;
        mPatientId = patientId;
        mBus = bus;
    }

    @Override protected ItemLoadFailedEvent doInBackground(Void... params) {
        RequestFuture<JsonPatient> future = RequestFuture.newFuture();

        // Try to download the specified patient from the server.
        LOG.i("Downloading single patient %s from server", mPatientId);
        mServer.getPatient(mPatientId, future, future);
        JsonPatient json;
        try {
            json = future.get(OpenMrsServer.TIMEOUT_SECONDS, SECONDS);
        } catch (TimeoutException e) {
            return new ItemLoadFailedEvent("timeout", mPatientId, e);
        } catch (InterruptedException e) {
            return new ItemLoadFailedEvent("interrupted", mPatientId, e);
        } catch (ExecutionException e) {
            return new ItemLoadFailedEvent("network error", mPatientId, e);
        }
        if (json == null) {
            LOG.i("Patient ID %s not found on server", mPatientId);
            return new ItemLoadFailedEvent("not found", mPatientId);
        }

        // Update the patient in the local database.
        Patient patient = Patient.fromJson(json);
        Uri uri = null;
        try (Cursor c = mContentResolver.query(Patients.URI, null,
            Patients.ID + " = ?", new String[] {mPatientId}, null)) {
            if (c.moveToNext()) {
                uri = Patients.URI.buildUpon().appendPath(mPatientId).build();
                mContentResolver.update(uri, patient.toContentValues(),
                    Patients.ID + " = ?", new String[] {mPatientId});
                LOG.i("Updated local patient %s", mPatientId);
            } else {
                uri = mContentResolver.insert(Patients.URI, patient.toContentValues());
                LOG.i("Added new local patient %s", mPatientId);
            }
        }

        // Record the UUID to use for fetching the patient back from the local database.
        if (uri == null || uri.equals(Uri.EMPTY)) {
            LOG.i("Patient ID %s not found on server", mPatientId);
            return new ItemLoadFailedEvent("not found", mPatientId);
        }
        if (json.uuid == null) {
            return new ItemLoadFailedEvent("server error", mPatientId);
        }
        mUuid = json.uuid;
        return null;
    }

    @Override protected void onPostExecute(ItemLoadFailedEvent event) {
        // If an error occurred, post the error event.
        if (event != null) {
            mBus.post(event);
            return;
        }

        // After updating a patient, we fetch the patient from the database. The
        // result of the fetch determines if adding a patient was truly successful
        // and propagates a new event to report success/failure.
        mTaskFactory.newLoadItemTask(
            Patients.URI, null, new UuidFilter(), mUuid, Patient::load, mBus
        ).execute();
    }
}
