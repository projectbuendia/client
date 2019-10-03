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

package org.projectbuendia.client.models.tasks;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.AsyncTask;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.RequestFuture;

import org.projectbuendia.client.App;
import org.projectbuendia.client.events.CrudEventBus;
import org.projectbuendia.client.events.data.ItemCreatedEvent;
import org.projectbuendia.client.events.data.ItemLoadFailedEvent;
import org.projectbuendia.client.events.data.ItemLoadedEvent;
import org.projectbuendia.client.events.data.PatientAddFailedEvent;
import org.projectbuendia.client.filter.db.patient.UuidFilter;
import org.projectbuendia.client.json.JsonObservation;
import org.projectbuendia.client.json.JsonPatient;
import org.projectbuendia.client.models.Patient;
import org.projectbuendia.client.net.Server;
import org.projectbuendia.client.providers.Contracts.Observations;
import org.projectbuendia.client.providers.Contracts.Patients;
import org.projectbuendia.client.sync.SyncManager;
import org.projectbuendia.client.utils.Logger;

import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

/**
 * An {@link AsyncTask} that adds a patient to a server.
 * <p/>
 * <p>If the operation succeeds, a {@link ItemCreatedEvent} is posted on the given
 * {@link CrudEventBus} with the added patient. If the operation fails, a
 * {@link PatientAddFailedEvent} is posted instead.
 */
public class AddPatientTask extends AsyncTask<Void, Void, PatientAddFailedEvent> {

    private static final Logger LOG = Logger.create();

    private final TaskFactory mTaskFactory;
    private final Server mServer;
    private final ContentResolver mContentResolver;
    private final JsonPatient mPatient;
    private final CrudEventBus mBus;
    @Inject SyncManager mSyncManager;

    private String mUuid;

    /** Creates a new {@link AddPatientTask}. */
    public AddPatientTask(
        TaskFactory taskFactory,
        Server server,
        ContentResolver contentResolver,
        JsonPatient patient,
        CrudEventBus bus) {
        mTaskFactory = taskFactory;
        mServer = server;
        mContentResolver = contentResolver;
        mPatient = patient;
        mBus = bus;
        App.inject(this);
    }

    @Override protected PatientAddFailedEvent doInBackground(Void... params) {
        RequestFuture<JsonPatient> future = RequestFuture.newFuture();

        mServer.addPatient(mPatient, future, future);
        JsonPatient json;
        try {
            json = future.get();
        } catch (InterruptedException e) {
            return new PatientAddFailedEvent(PatientAddFailedEvent.REASON_INTERRUPTED, e);
        } catch (ExecutionException e) {
            int failureReason = PatientAddFailedEvent.REASON_NETWORK;
            if (e.getCause() != null && e.getCause() instanceof VolleyError) {
                String message = e.getCause().getMessage();
                if (message.contains("could not insert: [org.openmrs.PatientIdentifier]")) {
                    failureReason = PatientAddFailedEvent.REASON_INVALID_ID;
                } else if (message.contains("already has the ID")) {
                    failureReason = PatientAddFailedEvent.REASON_DUPLICATE_ID;
                } else if (isValidationErrorMessageForField(message, "names[0].givenName")) {
                    failureReason = PatientAddFailedEvent.REASON_INVALID_GIVEN_NAME;
                } else if (isValidationErrorMessageForField(message, "names[0].familyName")) {
                    failureReason = PatientAddFailedEvent.REASON_INVALID_FAMILY_NAME;
                }
            }
            return new PatientAddFailedEvent(failureReason, e);
        }

        if (json.uuid == null) {
            LOG.e("Server successfully added a new patient but returned no UUID");
            return new PatientAddFailedEvent(PatientAddFailedEvent.REASON_SERVER, null);
        }

        Patient patient = Patient.fromJson(json);
        Uri uri = mContentResolver.insert(Patients.URI, patient.toContentValues());
        if (uri == null || uri.equals(Uri.EMPTY)) {
            return new PatientAddFailedEvent(PatientAddFailedEvent.REASON_CLIENT, null);
        }
        mUuid = json.uuid;
        if (json.observations != null) {
            ContentValues[] cvs = new ContentValues[json.observations.size()];
            int i = 0;
            for (JsonObservation obs : json.observations) {
                cvs[i++] = obs.toContentValues();
            }
            mContentResolver.bulkInsert(Observations.URI, cvs);
            if (DenormalizeObsTask.needsDenormalization(cvs)) {
                App.getModel().denormalizeObservations(mBus, patient.uuid);
            }
        }
        return null;
    }

    private boolean isValidationErrorMessageForField(String message, String fieldName) {
        return message.contains("'Patient#null' failed to validate with reason: "
            + fieldName);
    }

    @Override protected void onPostExecute(PatientAddFailedEvent event) {
        // If an error occurred, post the error event.
        if (event != null) {
            mBus.post(event);
            return;
        }

        // Otherwise, start a fetch task to fetch the patient from the database.
        mBus.register(new CreationEventSubscriber());
        LoadItemTask<Patient> task = mTaskFactory.newLoadItemTask(
            Patients.URI, null, new UuidFilter(), mUuid, Patient::load, mBus);
        task.execute();
    }

    // After updating a patient, we fetch the patient from the database. The result of the fetch
    // determines if adding a patient was truly successful and propagates a new event to report
    // success/failure.
    @SuppressWarnings("unused") // Called by reflection from EventBus.
    private final class CreationEventSubscriber {
        public void onEventMainThread(ItemLoadedEvent<?> event) {
            if (event.item instanceof Patient) {
                mBus.post(new ItemCreatedEvent<>((Patient) event.item));
                mBus.unregister(this);
            }
        }

        public void onEventMainThread(ItemLoadFailedEvent event) {
            mBus.post(new PatientAddFailedEvent(
                PatientAddFailedEvent.REASON_CLIENT, new Exception(event.error)));
            mBus.unregister(this);
        }
    }
}
