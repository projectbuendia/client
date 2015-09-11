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

package org.projectbuendia.client.data.app.tasks;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.AsyncTask;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.RequestFuture;

import org.projectbuendia.client.data.app.AppPatient;
import org.projectbuendia.client.data.app.AppPatientDelta;
import org.projectbuendia.client.data.app.converters.AppTypeConverters;
import org.projectbuendia.client.events.CrudEventBus;
import org.projectbuendia.client.events.data.PatientAddFailedEvent;
import org.projectbuendia.client.events.data.ItemCreatedEvent;
import org.projectbuendia.client.events.data.ItemFetchFailedEvent;
import org.projectbuendia.client.events.data.ItemFetchedEvent;
import org.projectbuendia.client.filter.db.patient.UuidFilter;
import org.projectbuendia.client.net.Server;
import org.projectbuendia.client.net.json.JsonPatient;
import org.projectbuendia.client.sync.SyncAccountService;
import org.projectbuendia.client.sync.providers.Contracts;
import org.projectbuendia.client.utils.Logger;

import java.util.concurrent.ExecutionException;

/**
 * An {@link AsyncTask} that adds a patient to a server.
 *
 * <p>If the operation succeeds, a {@link ItemCreatedEvent} is posted on the given
 * {@link CrudEventBus} with the added patient. If the operation fails, a
 * {@link PatientAddFailedEvent} is posted instead.
 */
public class AddPatientTask extends AsyncTask<Void, Void, PatientAddFailedEvent> {

    private static final Logger LOG = Logger.create();

    private final TaskFactory mTaskFactory;
    private final AppTypeConverters mConverters;
    private final Server mServer;
    private final ContentResolver mContentResolver;
    private final AppPatientDelta mPatientDelta;
    private final CrudEventBus mBus;

    private String mUuid;

    /** Creates a new {@link AddPatientTask}. */
    public AddPatientTask(
            TaskFactory taskFactory,
            AppTypeConverters converters,
            Server server,
            ContentResolver contentResolver,
            AppPatientDelta patientDelta,
            CrudEventBus bus) {
        mTaskFactory = taskFactory;
        mConverters = converters;
        mServer = server;
        mContentResolver = contentResolver;
        mPatientDelta = patientDelta;
        mBus = bus;
    }

    @Override
    protected PatientAddFailedEvent doInBackground(Void... params) {
        RequestFuture<JsonPatient> patientFuture = RequestFuture.newFuture();

        mServer.addPatient(mPatientDelta, patientFuture, patientFuture);
        JsonPatient patient;
        try {
            patient = patientFuture.get();
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

        if (patient.uuid == null) {
            LOG.e(
                    "Although the server reported a patient successfully added, it did not return "
                            + "a UUID for that patient. This indicates a server error.");

            return new PatientAddFailedEvent(
                    PatientAddFailedEvent.REASON_SERVER, null /*exception*/);
        }

        AppPatient appPatient = AppPatient.fromNet(patient);
        Uri uri = mContentResolver.insert(
                Contracts.Patients.CONTENT_URI, appPatient.toContentValues());

        // Perform incremental observation sync so we get admission date.
        SyncAccountService.startIncrementalObsSync();

        if (uri == null || uri.equals(Uri.EMPTY)) {
            return new PatientAddFailedEvent(
                    PatientAddFailedEvent.REASON_CLIENT, null /*exception*/);
        }

        mUuid = patient.uuid;

        return null;
    }

    @Override
    protected void onPostExecute(PatientAddFailedEvent event) {
        // If an error occurred, post the error event.
        if (event != null) {
            mBus.post(event);
            return;
        }

        // If the UUID was not set, a programming error occurred. Log and post an error event.
        if (mUuid == null) {
            LOG.e(
                    "Although a patient add ostensibly succeeded, no UUID was set for the newly-"
                            + "added patient. This indicates a programming error.");

            mBus.post(new PatientAddFailedEvent(
                    PatientAddFailedEvent.REASON_UNKNOWN, null /*exception*/));
            return;
        }

        // Otherwise, start a fetch task to fetch the patient from the database.
        mBus.register(new CreationEventSubscriber());
        FetchItemTask<AppPatient> task = mTaskFactory.newFetchSingleAsyncTask(
                Contracts.Patients.CONTENT_URI,
                null,
                new UuidFilter(),
                mUuid,
                mConverters.patient,
                mBus);
        task.execute();
    }

    private boolean isValidationErrorMessageForField(String message, String fieldName) {
        return message.contains("'Patient#null' failed to validate with reason: "
                + fieldName);
    }

    // After updating a patient, we fetch the patient from the database. The result of the fetch
    // determines if adding a patient was truly successful and propagates a new event to report
    // success/failure.
    @SuppressWarnings("unused") // Called by reflection from EventBus.
    private final class CreationEventSubscriber {
        public void onEventMainThread(ItemFetchedEvent<AppPatient> event) {
            mBus.post(new ItemCreatedEvent<>(event.item));
            mBus.unregister(this);
        }

        public void onEventMainThread(ItemFetchFailedEvent event) {
            mBus.post(new PatientAddFailedEvent(
                    PatientAddFailedEvent.REASON_CLIENT, new Exception(event.error)));
            mBus.unregister(this);
        }
    }
}
