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
import android.os.AsyncTask;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.RequestFuture;

import org.projectbuendia.client.App;
import org.projectbuendia.client.events.CrudEventBus;
import org.projectbuendia.client.events.data.EncounterAddFailedEvent;
import org.projectbuendia.client.events.data.ItemCreatedEvent;
import org.projectbuendia.client.events.data.ItemLoadFailedEvent;
import org.projectbuendia.client.events.data.ItemLoadedEvent;
import org.projectbuendia.client.filter.db.encounter.EncounterUuidFilter;
import org.projectbuendia.client.json.JsonEncounter;
import org.projectbuendia.client.models.Encounter;
import org.projectbuendia.client.net.Server;
import org.projectbuendia.client.providers.Contracts.Observations;
import org.projectbuendia.client.utils.Logger;

import java.util.concurrent.ExecutionException;

/**
 * An {@link AsyncTask} that adds a patient encounter to the server.
 * <p/>
 * <p>If the operation succeeds, a {@link ItemCreatedEvent} is posted on the given
 * {@link CrudEventBus} with the added encounter. If the operation fails, a
 * {@link EncounterAddFailedEvent} is posted instead.
 */
public class AddEncounterTask extends AsyncTask<Void, Void, EncounterAddFailedEvent> {
    // TODO: Factor out common code between this class and AddPatientTask.
    private static final Logger LOG = Logger.create();

    private static final String[] ENCOUNTER_PROJECTION = new String[] {
        Observations.CONCEPT_UUID,
        Observations.ENCOUNTER_MILLIS,
        Observations.ENCOUNTER_UUID,
        Observations.PATIENT_UUID,
        Observations.VALUE
    };

    private final TaskFactory mTaskFactory;
    private final Server mServer;
    private final ContentResolver mContentResolver;
    private final Encounter mEncounter;
    private final CrudEventBus mBus;

    private String mUuid;

    /** Creates a new {@link AddEncounterTask}. */
    public AddEncounterTask(
        TaskFactory taskFactory,
        Server server,
        ContentResolver contentResolver,
        Encounter encounter,
        CrudEventBus bus
    ) {
        mTaskFactory = taskFactory;
        mServer = server;
        mContentResolver = contentResolver;
        mEncounter = encounter;
        mBus = bus;
    }

    @Override protected EncounterAddFailedEvent doInBackground(Void... params) {
        RequestFuture<JsonEncounter> future = RequestFuture.newFuture();

        mServer.addEncounter(mEncounter, future, future);
        JsonEncounter jsonEncounter;
        try {
            jsonEncounter = future.get();
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
            LOG.e("Error response: %s", ((VolleyError) e.getCause()).networkResponse);

            return new EncounterAddFailedEvent(reason, (VolleyError) e.getCause());
        }

        if (jsonEncounter.uuid == null) {
            LOG.e(
                "Although the server reported an encounter successfully added, it did not "
                    + "return a UUID for that encounter. This indicates a server error.");

            return new EncounterAddFailedEvent(
                EncounterAddFailedEvent.Reason.FAILED_TO_SAVE_ON_SERVER, null /*exception*/);
        }

        Encounter encounter = Encounter.fromJson(jsonEncounter);
        ContentValues[] values = encounter.toContentValuesArray();
        if (values.length > 0) {
            int inserted = mContentResolver.bulkInsert(Observations.URI, values);
            if (DenormalizeObservationsTask.needsDenormalization(values)) {
                App.getModel().denormalizeObservations(mBus, encounter.patientUuid);
            }
            if (inserted != values.length) {
                LOG.w("Inserted %d observations for encounter. Expected: %d",
                    inserted, encounter.observations.length);
                return new EncounterAddFailedEvent(
                    EncounterAddFailedEvent.Reason.INVALID_NUMBER_OF_OBSERVATIONS_SAVED,
                    null /*exception*/);
            }
        } else {
            LOG.w("Encounter was sent to the server but contained no observations.");
        }

        mUuid = jsonEncounter.uuid;
        return null;
    }

    @Override protected void onPostExecute(EncounterAddFailedEvent event) {
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
        LoadItemTask<Encounter> task = mTaskFactory.newLoadItemTask(
            Observations.URI, ENCOUNTER_PROJECTION, new EncounterUuidFilter(), mUuid, Encounter::load, mBus);
        task.execute();
    }

    // After updating an encounter, we fetch the encounter from the database. The result of the
    // fetch determines if adding a patient was truly successful and propagates a new event to
    // report success/failure.
    @SuppressWarnings("unused") // Called by reflection from EventBus.
    private final class CreationEventSubscriber {
        public void onEventMainThread(ItemLoadedEvent<?> event) {
            if (event.item instanceof Encounter) {
                mBus.post(new ItemCreatedEvent<>((Encounter) event.item));
                mBus.unregister(this);
            }
        }

        public void onEventMainThread(ItemLoadFailedEvent event) {
            mBus.post(new EncounterAddFailedEvent(
                EncounterAddFailedEvent.Reason.FAILED_TO_FETCH_SAVED_OBSERVATION,
                new Exception(event.error)));
            mBus.unregister(this);
        }
    }
}
