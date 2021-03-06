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
import android.os.AsyncTask;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.RequestFuture;

import org.projectbuendia.client.events.CrudEventBus;
import org.projectbuendia.client.events.data.ItemLoadFailedEvent;
import org.projectbuendia.client.events.data.ItemLoadedEvent;
import org.projectbuendia.client.events.data.ItemUpdatedEvent;
import org.projectbuendia.client.events.data.PatientUpdateFailedEvent;
import org.projectbuendia.client.filter.db.SimpleSelectionFilter;
import org.projectbuendia.client.filter.db.patient.UuidFilter;
import org.projectbuendia.client.json.JsonPatient;
import org.projectbuendia.models.Patient;
import org.projectbuendia.client.net.OpenMrsServer;
import org.projectbuendia.client.net.Server;
import org.projectbuendia.client.providers.Contracts.Patients;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * An {@link AsyncTask} that updates a patient on a server.
 * <p/>
 * <p>If the operation succeeds, a {@link ItemUpdatedEvent} is posted on the given
 * {@link CrudEventBus} with both the old and updated patient data. If the operation fails, a
 * {@link PatientUpdateFailedEvent} is posted instead.
 */
public class UpdatePatientTask extends AsyncTask<Void, Void, PatientUpdateFailedEvent> {
    private static final SimpleSelectionFilter FILTER = new UuidFilter();

    private final TaskFactory mTaskFactory;
    private final Server mServer;
    private final ContentResolver mContentResolver;
    private final JsonPatient mPatient;
    private final String mUuid;
    private final CrudEventBus mBus;

    UpdatePatientTask(
        TaskFactory taskFactory,
        Server server,
        ContentResolver contentResolver,
        JsonPatient patient,
        CrudEventBus bus) {
        mTaskFactory = taskFactory;
        mServer = server;
        mContentResolver = contentResolver;
        mPatient = patient;
        mUuid = patient.uuid;
        mBus = bus;
    }

    @Override protected PatientUpdateFailedEvent doInBackground(Void... params) {
        RequestFuture<JsonPatient> patientFuture = RequestFuture.newFuture();

        mServer.updatePatient(mPatient, patientFuture, patientFuture);
        try {
            patientFuture.get(OpenMrsServer.TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            return new PatientUpdateFailedEvent(PatientUpdateFailedEvent.REASON_TIMEOUT, e);
        } catch (InterruptedException e) {
            return new PatientUpdateFailedEvent(PatientUpdateFailedEvent.REASON_INTERRUPTED, e);
        } catch (ExecutionException e) {
            // TODO: Parse the VolleyError to see exactly what kind of error was raised.
            return new PatientUpdateFailedEvent(
                PatientUpdateFailedEvent.REASON_NETWORK, (VolleyError) e.getCause());
        }

        int count = mContentResolver.update(
            Patients.URI,
            mPatient.toContentValues(),
            FILTER.getSelectionString(),
            FILTER.getSelectionArgs(mUuid));

        switch (count) {
            case 0:
                return new PatientUpdateFailedEvent(
                    PatientUpdateFailedEvent.REASON_NO_SUCH_PATIENT, null /*exception*/);
            case 1:
                return null;
            default:
                return new PatientUpdateFailedEvent(
                    PatientUpdateFailedEvent.REASON_SERVER, null /*exception*/);
        }
    }

    @Override protected void onPostExecute(PatientUpdateFailedEvent event) {
        // If an error occurred, post the error event.
        if (event != null) {
            mBus.post(event);
            return;
        }

        // Otherwise, start a fetch task to fetch the patient from the database.
        mBus.register(new UpdateEventSubscriber());
        LoadItemTask<Patient> task = mTaskFactory.newLoadItemTask(
            Patients.URI, null, new UuidFilter(), mUuid, Patient::load, mBus);
        task.execute();
    }

    // After updating a patient, we fetch the patient from the database. The result of the fetch
    // determines if updating a patient was truly successful and propagates a new event to report
    // success/failure.
    @SuppressWarnings("unused") // Called by reflection from EventBus.
    private final class UpdateEventSubscriber {
        public void onEventMainThread(ItemLoadedEvent<?> event) {
            if (event.item instanceof Patient) {
                mBus.post(new ItemUpdatedEvent<>(mUuid, (Patient) event.item));
                mBus.unregister(this);
            }
        }

        public void onEventMainThread(ItemLoadFailedEvent event) {
            mBus.post(new PatientUpdateFailedEvent(
                PatientUpdateFailedEvent.REASON_CLIENT, new Exception(event.error)));
            mBus.unregister(this);
        }
    }
}
