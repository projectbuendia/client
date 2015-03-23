/*
 * Copyright 2015 The Project Buendia Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.msf.records.data.app.tasks;

import android.content.ContentResolver;
import android.os.AsyncTask;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.RequestFuture;

import org.msf.records.data.app.AppEncounter;
import org.msf.records.data.app.AppPatient;
import org.msf.records.data.app.converters.AppEncounterConverter;
import org.msf.records.data.app.converters.AppTypeConverters;
import org.msf.records.events.CrudEventBus;
import org.msf.records.events.data.EncounterAddFailedEvent;
import org.msf.records.events.data.SingleItemCreatedEvent;
import org.msf.records.events.data.SingleItemFetchFailedEvent;
import org.msf.records.events.data.SingleItemFetchedEvent;
import org.msf.records.filter.db.encounter.EncounterUuidFilter;
import org.msf.records.net.Server;
import org.msf.records.net.model.Encounter;
import org.msf.records.sync.providers.Contracts;
import org.msf.records.utils.Logger;

import java.util.concurrent.ExecutionException;

/**
 * An {@link AsyncTask} that adds a patient encounter to a server.
 *
 * <p>If the operation succeeds, a {@link SingleItemCreatedEvent} is posted on the given
 * {@link CrudEventBus} with the added encounter. If the operation fails, a
 * {@link EncounterAddFailedEvent} is posted instead.
 */
public class AppAddEncounterAsyncTask extends AsyncTask<Void, Void, EncounterAddFailedEvent> {
    // TODO: Factor out common code between this class and AppAddPatientAsyncTask.
    private static final Logger LOG = Logger.create();

    private static final String[] ENCOUNTER_PROJECTION = new String[] {
            Contracts.ObservationColumns.CONCEPT_UUID,
            Contracts.ObservationColumns.ENCOUNTER_TIME,
            Contracts.ObservationColumns.ENCOUNTER_UUID,
            Contracts.ObservationColumns.PATIENT_UUID,
            Contracts.ObservationColumns.VALUE
    };

    private final AppAsyncTaskFactory mTaskFactory;
    private final AppTypeConverters mConverters;
    private final Server mServer;
    private final ContentResolver mContentResolver;
    private final AppPatient mPatient;
    private final AppEncounter mEncounter;
    private final CrudEventBus mBus;

    private String mUuid;

    /**
     * Creates a new {@link org.msf.records.data.app.tasks.AppAddEncounterAsyncTask}.
     */
    public AppAddEncounterAsyncTask(
            AppAsyncTaskFactory taskFactory,
            AppTypeConverters converters,
            Server server,
            ContentResolver contentResolver,
            AppPatient patient,
            AppEncounter encounter,
            CrudEventBus bus) {
        mTaskFactory = taskFactory; 
        mConverters = converters;
        mServer = server;
        mContentResolver = contentResolver;
        mPatient = patient;
        mEncounter = encounter;
        mBus = bus;
    }

    @Override
    protected EncounterAddFailedEvent doInBackground(Void... params) {
        RequestFuture<Encounter> encounterFuture = RequestFuture.newFuture();

        mServer.addEncounter(mPatient, mEncounter, encounterFuture, encounterFuture);
        Encounter encounter;
        try {
            encounter = encounterFuture.get();
        } catch (InterruptedException e) {
            return new EncounterAddFailedEvent(EncounterAddFailedEvent.Reason.INTERRUPTED, e);
        } catch (ExecutionException e) {
            LOG.e(e, "Server error while adding encounter");

            EncounterAddFailedEvent.Reason reason =
                    EncounterAddFailedEvent.Reason.UNKNOWN_SERVER_ERROR;
            if (e.getCause() != null) {
                String errorMessage = e.getCause().getMessage();
                if (errorMessage.contains("failed to validate")) {
                    reason = EncounterAddFailedEvent.Reason.FAILED_TO_VALIDATE;
                } else if (errorMessage.contains("Privileges required")) {
                    reason = EncounterAddFailedEvent.Reason.FAILED_TO_AUTHENTICATE;
                }
            }
            LOG.e("Error response: %s", ((VolleyError)e.getCause()).networkResponse);

            return new EncounterAddFailedEvent(reason, (VolleyError) e.getCause());
        }

        if (encounter.uuid == null) {
            LOG.e(
                    "Although the server reported an encounter successfully added, it did not "
                            + "return a UUID for that encounter. This indicates a server error.");

            return new EncounterAddFailedEvent(
                    EncounterAddFailedEvent.Reason.FAILED_TO_SAVE_ON_SERVER, null /*exception*/);
        }

        AppEncounter appEncounter = AppEncounter.fromNet(mPatient.uuid, encounter);

        if (appEncounter.observations.length > 0) {
            int inserted = mContentResolver.bulkInsert(Contracts.Observations.CONTENT_URI,
                    appEncounter.toContentValuesArray());

            if (inserted != appEncounter.observations.length) {
                LOG.w("Inserted %d observations for encounter. Expected: %d",
                        inserted, appEncounter.observations.length);
                return new EncounterAddFailedEvent(
                        EncounterAddFailedEvent.Reason.INVALID_NUMBER_OF_OBSERVATIONS_SAVED,
                        null /*exception*/);
            }
        } else {
            LOG.w("Encounter was sent to the server but contained no observations.");
        }

        mUuid = encounter.uuid;

        return null;
    }

    @Override
    protected void onPostExecute(EncounterAddFailedEvent event) {
        // If an error occurred, post the error event.
        if (event != null) {
            mBus.post(event);
            return;
        }

        // If the UUID was not set, a programming error occurred. Log and post an error event.
        if (mUuid == null) {
            LOG.e(
                    "Although an encounter add ostensibly succeeded, no UUID was set for the newly-"
                            + "added encounter. This indicates a programming error.");

            mBus.post(new EncounterAddFailedEvent(
                    EncounterAddFailedEvent.Reason.UNKNOWN, null /*exception*/));
            return;
        }

        // Otherwise, start a fetch task to fetch the encounter from the database.
        mBus.register(new CreationEventSubscriber());
        FetchSingleAsyncTask<AppEncounter> task = mTaskFactory.newFetchSingleAsyncTask(
                Contracts.Observations.CONTENT_URI,
                ENCOUNTER_PROJECTION,
                new EncounterUuidFilter(),
                mUuid,
                new AppEncounterConverter(mPatient.uuid),
                mBus);
        task.execute();
    }

    // After updating an encounter, we fetch the encounter from the database. The result of the
    // fetch determines if adding a patient was truly successful and propagates a new event to
    // report success/failure.
    @SuppressWarnings("unused") // Called by reflection from EventBus.
    private final class CreationEventSubscriber {
        public void onEventMainThread(SingleItemFetchedEvent<AppEncounter> event) {
            mBus.post(new SingleItemCreatedEvent<>(event.item));
            mBus.unregister(this);
        }

        public void onEventMainThread(SingleItemFetchFailedEvent event) {
            mBus.post(new EncounterAddFailedEvent(
                    EncounterAddFailedEvent.Reason.FAILED_TO_FETCH_SAVED_OBSERVATION,
                    new Exception(event.error)));
            mBus.unregister(this);
        }
    }
}
