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

package org.projectbuendia.client.models;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;

import org.joda.time.DateTime;
import org.projectbuendia.client.App;
import org.projectbuendia.client.events.CrudEventBus;
import org.projectbuendia.client.events.data.ItemCreatedEvent;
import org.projectbuendia.client.events.data.ItemLoadedEvent;
import org.projectbuendia.client.events.data.ItemUpdatedEvent;
import org.projectbuendia.client.events.data.TypedCursorLoadedEvent;
import org.projectbuendia.client.events.data.TypedCursorLoadedEventFactory;
import org.projectbuendia.client.filter.db.SimpleSelectionFilter;
import org.projectbuendia.client.filter.db.patient.UuidFilter;
import org.projectbuendia.client.models.Encounter.Observation;
import org.projectbuendia.client.models.tasks.AddPatientTask;
import org.projectbuendia.client.models.tasks.TaskFactory;
import org.projectbuendia.client.models.tasks.UpdatePatientTask;
import org.projectbuendia.client.net.Server;
import org.projectbuendia.client.providers.Contracts;
import org.projectbuendia.client.providers.Contracts.Misc;
import org.projectbuendia.client.providers.Contracts.Observations;
import org.projectbuendia.client.utils.Logger;
import org.projectbuendia.client.utils.Utils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A model that manages all data access within the application.
 * <p/>
 * <p>This model's {@code load} methods often provide {@link TypedCursor}s as results, which MUST
 * be closed when the consumer is done with them.
 * <p/>
 * <p>Updates done through this model are written through to a backing {@link Server}; callers do
 * not need to worry about the implementation details of this.
 */
public class AppModel {

    private static final Logger LOG = Logger.create();
    private final ContentResolver mContentResolver;
    private final TaskFactory mTaskFactory;

    private LocationForestProvider forestProvider = null;

    AppModel(ContentResolver contentResolver, TaskFactory taskFactory) {
        mContentResolver = contentResolver;
        mTaskFactory = taskFactory;
        forestProvider = new LocationForestProvider(mContentResolver);
    }

    /** Clears all in-memory model state. */
    public void reset() {
        forestProvider.dispose();
        forestProvider = new LocationForestProvider(mContentResolver);
    }

    public @Nonnull LocationForest getForest() {
        return getForest(App.getSettings().getLocaleTag());
    }

    public @Nullable Location getDefaultLocation() {
        return getForest().getDefaultLocation();
    }

    private @Nonnull LocationForest getForest(String locale) {
        return forestProvider.getForest(locale);
    }

    public void setOnForestReplacedListener(Runnable listener) {
        forestProvider.setOnForestReplacedListener(listener);
    }

    /** Returns true if the model is ready for use. */
    public boolean isReady() {
        return getLastFullSyncTime() != null;
    }

    public DateTime getLastFullSyncTime() {
        // The FULL_SYNC_END_MILLIS field indicates a successful full sync.
        // It is set to non-null only when the end of a full sync is reached and the
        // database has not been cleared during the sync (see storeFullSyncEndTime).
        DateTime fullSyncEnd = null;
        try (Cursor cursor = mContentResolver.query(Misc.URI, null, null, null, null)) {
            if (cursor.moveToNext()) {
                fullSyncEnd = Utils.getDateTime(cursor, Misc.FULL_SYNC_END_MILLIS);
            }
        }
        return fullSyncEnd;
    }

    public void voidObservation(CrudEventBus bus, VoidObs voidObs) {
        String conditions = Observations.UUID + " = ?";
        ContentValues values = new ContentValues();
        values.put(Observations.VOIDED, 1);
        mTaskFactory.newVoidObsTask(bus, voidObs).execute();
    }

    /** Asynchronously downloads one patient from the server and saves it locally. */
    public void fetchSinglePatient(CrudEventBus bus, String patientId) {
        mTaskFactory.newFetchSinglePatientTask(patientId, bus).execute();
    }

    /**
     * Asynchronously loads patients, posting a {@link TypedCursorLoadedEvent} with
     * {@link Patient}s on the specified event bus when complete.
     */
    public void loadPatients(CrudEventBus bus, SimpleSelectionFilter filter, String constraint) {
        // NOTE: We need to keep the object creation separate from calling #execute() here, because
        // the type inference breaks on Java 8 otherwise, which throws
        // `java.lang.ClassCastException: java.lang.Object[] cannot be cast to java.lang.Void[]`.
        // See http://stackoverflow.com/questions/24136126/fatal-exception-asynctask and
        // https://github.com/projectbuendia/client/issues/7
        LoadTypedCursorAsyncTask<Patient> task = new LoadTypedCursorAsyncTask<>(
            Contracts.Patients.URI,
            // The projection must contain an "_id" column for the ListAdapter as well as all
            // the columns used in Patient.Loader.fromCursor().
            null, //new String[] {"rowid as _id", Patients.UUID, Patients.ID, Patients.GIVEN_NAME,
                //Patients.FAMILY_NAME, Patients.BIRTHDATE, Patients.SEX, Patients.LOCATION_UUID},
            Patient.class, mContentResolver,
            filter, constraint, Patient.LOADER, bus);
        task.execute();
    }

    /**
     * Asynchronously loads a single patient by UUID, posting a {@link ItemLoadedEvent}
     * with the {@link Patient} on the specified event bus when complete.
     */
    public void loadSinglePatient(CrudEventBus bus, String uuid) {
        mTaskFactory.newLoadItemTask(
            Contracts.Patients.URI, null, new UuidFilter(), uuid, Patient.LOADER, bus
        ).execute();
    }

    /**
     * Asynchronously adds a patient, posting a
     * {@link ItemCreatedEvent} with the newly-added patient on
     * the specified event bus when complete.
     */
    public void addPatient(CrudEventBus bus, PatientDelta patientDelta) {
        AddPatientTask task = mTaskFactory.newAddPatientTask(patientDelta, bus);
        task.execute();
    }

    /**
     * Asynchronously updates a patient, posting a
     * {@link ItemUpdatedEvent} with the updated
     * {@link Patient} on the specified event bus when complete.
     */
    public void updatePatient(
        CrudEventBus bus, String patientUuid, PatientDelta patientDelta) {
        UpdatePatientTask task =
            mTaskFactory.newUpdatePatientTask(patientUuid, patientDelta, bus);
        task.execute();
    }

    /**
     * Asynchronously adds or updates an order (depending whether order.uuid is null), posting an
     * {@link ItemCreatedEvent} or {@link ItemUpdatedEvent} when complete.
     */
    public void saveOrder(CrudEventBus bus, Order order) {
        mTaskFactory.newSaveOrderTask(order, bus).execute();
    }

    /** Asynchronously deletes an order. */
    public void deleteOrder(CrudEventBus bus, String orderUuid) {
        mTaskFactory.newDeleteOrderTask(orderUuid, bus).execute();
    }

    /**
     * Asynchronously adds an encounter that records an order as executed, posting a
     * {@link ItemCreatedEvent} when complete.
     */
    public void addOrderExecutedEncounter(CrudEventBus bus, String patientUuid, String orderUuid) {
        mTaskFactory.newAddEncounterTask(new Encounter(
            null, patientUuid, DateTime.now(), null, new String[] {orderUuid}
        ), bus).execute();
    }

    /** Adds a single observation in an encounter, posting ItemCreatedEvent when complete. */
    public void addObservationEncounter(CrudEventBus bus, String patientUuid, Observation obs) {
        mTaskFactory.newAddEncounterTask(new Encounter(
            null, patientUuid, DateTime.now(), new Observation[] {obs}, null
        ), bus).execute();
    }

    /**
     * Asynchronously adds an encounter to a patient, posting a
     * {@link ItemCreatedEvent} when complete.
     */
    public void addEncounter(CrudEventBus bus, Encounter encounter) {
        mTaskFactory.newAddEncounterTask(encounter, bus).execute();
    }

    /**
     * Updates the denormalized observation fields in a row in the patient table
     * with the latest unvoided values in the observations table.
     */
    public void denormalizeObservations(CrudEventBus bus, String patientUuid) {
        mTaskFactory.newDenormalizeObservationsTask(patientUuid, bus).execute();
    }

    private static class LoadTypedCursorAsyncTask<T extends Model>
        extends AsyncTask<Void, Void, TypedCursor<T>> {

        private final Uri mContentUri;
        private final String[] mProjection;
        private final Class<T> mClazz;
        private final ContentResolver mContentResolver;
        private final SimpleSelectionFilter mFilter;
        private final String mConstraint;
        private final CursorLoader<T> mLoader;
        private final CrudEventBus mBus;

        public LoadTypedCursorAsyncTask(
            Uri contentUri,
            String[] projection,
            Class<T> clazz,
            ContentResolver contentResolver,
            SimpleSelectionFilter<T> filter,
            String constraint,
            CursorLoader<T> loader,
            CrudEventBus bus) {
            mContentUri = contentUri;
            mProjection = projection;
            mClazz = clazz;
            mContentResolver = contentResolver;
            mFilter = filter;
            mConstraint = constraint;
            mLoader = loader;
            mBus = bus;
        }

        @Override protected TypedCursor<T> doInBackground(Void... voids) {
            Cursor cursor = null;
            try {
                cursor = mContentResolver.query(
                    mContentUri,
                    mProjection,
                    mFilter.getSelectionString(),
                    mFilter.getSelectionArgs(mConstraint),
                    null);

                return new TypedCursorWithLoader<>(cursor, mLoader);
            } catch (Exception e) {
                if (cursor != null) {
                    cursor.close();
                }

                throw e;
            }
        }

        @Override protected void onPostExecute(TypedCursor<T> result) {
            mBus.post(TypedCursorLoadedEventFactory.createEvent(mClazz, result));
        }
    }
}
