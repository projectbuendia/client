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
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;

import org.joda.time.DateTime;
import org.projectbuendia.client.events.CrudEventBus;
import org.projectbuendia.client.events.data.ItemCreatedEvent;
import org.projectbuendia.client.events.data.ItemFetchedEvent;
import org.projectbuendia.client.events.data.ItemUpdatedEvent;
import org.projectbuendia.client.events.data.TypedCursorFetchedEvent;
import org.projectbuendia.client.events.data.TypedCursorFetchedEventFactory;
import org.projectbuendia.client.filter.db.SimpleSelectionFilter;
import org.projectbuendia.client.filter.db.patient.UuidFilter;
import org.projectbuendia.client.models.tasks.AddPatientTask;
import org.projectbuendia.client.models.tasks.TaskFactory;
import org.projectbuendia.client.models.tasks.UpdatePatientTask;
import org.projectbuendia.client.net.Server;
import org.projectbuendia.client.providers.Contracts;
import org.projectbuendia.client.utils.Logger;
import org.projectbuendia.client.utils.Receiver;
import org.projectbuendia.client.utils.Utils;

import static org.projectbuendia.client.utils.Utils.eq;

/**
 * A model that manages all data access within the application.
 * <p/>
 * <p>This model's {@code fetch} methods often provide {@link TypedCursor}s as results, which MUST
 * be closed when the consumer is done with them.
 * <p/>
 * <p>Updates done through this model are written through to a backing {@link Server}; callers do
 * not need to worry about the implementation details of this.
 */
public class AppModel {
    public static final String CHART_UUID = "ea43f213-66fb-4af6-8a49-70fd6b9ce5d4";

    // This is a custom Buendia-specific concept to indicate that a treatment order
    // has been carried out (e.g. a prescribed medication has been administered).
    // The timestamp of an observation for this concept should be the time the order
    // was executed, and the value of the observation should be the UUID of the order.
    public static final String ORDER_EXECUTED_CONCEPT_UUID = "buendia-concept-order_executed";

    private static final Logger LOG = Logger.create();
    private final ContentResolver mContentResolver;
    private final TaskFactory mTaskFactory;

    private final Object loadedForestLock = new Object();
    private LocationForest loadedForest = null;
    private String loadedForestLocale = null;
    private boolean forestObserversRegistered = false;
    private Receiver<LocationForest> onForestRebuiltListener = null;
    private Receiver<LocationForest> onForestUpdatedListener = null;

    /**
     * Returns true iff the model has previously been fully downloaded from the server--that is, if
     * locations, patients, users, charts, and observations were all downloaded at some point. Note
     * that this data may be out-of-date, but must be present in some form for proper operation of
     * the app.
     */
    public boolean isFullModelAvailable() {
        return getLastFullSyncTime() != null;
    }

    public DateTime getLastFullSyncTime() {
        // The sync process is transactional, but in rare cases, a sync may complete without ever
        // having started--this is the case if user data is cleared mid-sync, for example. To check
        // that a sync actually completed, we look at the FULL_SYNC_START_MILLIS and
        // FULL_SYNC_END_MILLIS columns in the Misc table, which are written to as the first and
        // last operations of a complete sync. If both of these fields are present, and the last
        // end time is greater than the last start time, then a full sync must have completed.
        try (Cursor c = mContentResolver.query(
                Contracts.Misc.URI, null, null, null, null)) {
            LOG.d("Sync timing result count: %d", c.getCount());
            if (c.moveToNext()) {
                DateTime fullSyncStart = Utils.getDateTime(c, Contracts.Misc.FULL_SYNC_START_MILLIS);
                DateTime fullSyncEnd = Utils.getDateTime(c, Contracts.Misc.FULL_SYNC_END_MILLIS);
                LOG.i("full_sync_start_millis = %s, full_sync_end_millis = %s",
                    fullSyncStart, fullSyncEnd);
                if (fullSyncStart != null && fullSyncEnd != null && fullSyncEnd.isAfter(fullSyncStart)) {
                    return fullSyncEnd;
                }
            }
            return null;
        }
    }

    public void VoidObservation(CrudEventBus bus, VoidObs voidObs) {
        String conditions = Contracts.Observations.UUID + " = ?";
        ContentValues values = new ContentValues();
        values.put(Contracts.Observations.VOIDED,1);
        mContentResolver.update(Contracts.Observations.URI, values, conditions, new String[]{voidObs.Uuid});
        mTaskFactory.voidObsTask(bus, voidObs).execute();
    }

    public LocationForest getForest(String locale) {
        LocationForest forest;
        synchronized (loadedForestLock) {
            if (loadedForest == null || !eq(locale, loadedForestLocale)) {
                loadedForest = loadForest(locale);
                loadedForestLocale = locale;
            }
            forest = loadedForest;
            if (!forestObserversRegistered) {
                registerForestObservers();
                forestObserversRegistered = true;
            }
        }
        return forest;
    }

    public void setForestRebuiltListener(Receiver<LocationForest> listener) {
        synchronized (loadedForestLock) {
            onForestRebuiltListener = listener;
            if (listener != null && loadedForest == null && loadedForestLocale != null) {
                loadedForest = loadForest(loadedForestLocale);
            }
        }
    }

    public void setForestUpdatedListener(Receiver<LocationForest> listener) {
        synchronized (loadedForestLock) {
            onForestUpdatedListener = listener;
            if (listener != null && loadedForest == null && loadedForestLocale != null) {
                loadedForest = loadForest(loadedForestLocale);
            }
        }
    }

    private LocationForest loadForest(String locale) {
        Uri uri = Contracts.getLocalizedLocationsUri(locale);
        try (Cursor cursor = mContentResolver.query(uri, null, null, null, null)) {
            return new LocationForest(
                new TypedCursorWithLoader<>(cursor, LocationQueryResult.LOADER));
        }
    }

    private void updateForest(LocationForest forest) {
        Uri uri = Contracts.getLocalizedLocationsUri("-");
        try (Cursor cursor = mContentResolver.query(uri, null, null, null, null)) {
            forest.updatePatientCounts(
                new TypedCursorWithLoader<>(cursor, LocationQueryResult.LOADER));
        }
    }

    private void registerForestObservers() {
        mContentResolver.registerContentObserver(
            Contracts.LocalizedLocations.URI, true, new ContentObserver(new Handler()) {
                @Override public void onChange(boolean selfChange) {
                    LocationForest forest = null;
                    synchronized (loadedForestLock) {
                        if (onForestRebuiltListener != null) {
                            // Someone is listening, so get a new forest for them now.
                            forest = loadedForest = loadForest(loadedForestLocale);
                        } else {
                            // No one is listening; ensure the forest is reloaded later.
                            loadedForest = null;
                        }
                    }
                    if (onForestRebuiltListener != null) {
                        onForestRebuiltListener.receive(forest);
                    }
                }
            }
        );
        mContentResolver.registerContentObserver(
            Contracts.Patients.URI, true, new ContentObserver(new Handler()) {
                @Override public void onChange(boolean selfChange) {
                    LocationForest forest = null;
                    synchronized (loadedForestLock) {
                        if (onForestUpdatedListener != null) {
                            // Someone is listening, so update the forest for them now.
                            updateForest(loadedForest);
                            forest = loadedForest;
                        } else {
                            // No one is listening; ensure the forest is reloaded later.
                            loadedForest = null;
                        }
                    }
                    if (onForestUpdatedListener != null) {
                        onForestUpdatedListener.receive(forest);
                    }
                }
            }
        );
    }

    /** Asynchronously downloads one patient from the server and saves it locally. */
    public void downloadSinglePatient(CrudEventBus bus, String patientId) {
        mTaskFactory.newDownloadSinglePatientTask(patientId, bus).execute();
    }

    /**
     * Asynchronously fetches patients, posting a {@link TypedCursorFetchedEvent} with
     * {@link Patient}s on the specified event bus when complete.
     */
    public void fetchPatients(CrudEventBus bus, SimpleSelectionFilter filter, String constraint) {
        // NOTE: We need to keep the object creation separate from calling #execute() here, because
        // the type inference breaks on Java 8 otherwise, which throws
        // `java.lang.ClassCastException: java.lang.Object[] cannot be cast to java.lang.Void[]`.
        // See http://stackoverflow.com/questions/24136126/fatal-exception-asynctask and
        // https://github.com/projectbuendia/client/issues/7
        FetchTypedCursorAsyncTask<Patient> task = new FetchTypedCursorAsyncTask<>(
            Contracts.Patients.URI,
            // The projection must contain an "_id" column for the ListAdapter as well as all
            // the columns used in Patient.Loader.fromCursor().
            null, //new String[] {"rowid as _id", Patients.UUID, Patients.ID, Patients.GIVEN_NAME,
                //Patients.FAMILY_NAME, Patients.BIRTHDATE, Patients.GENDER, Patients.LOCATION_UUID},
            Patient.class, mContentResolver,
            filter, constraint, Patient.LOADER, bus);
        task.execute();
    }

    /**
     * Asynchronously fetches a single patient by UUID, posting a {@link ItemFetchedEvent}
     * with the {@link Patient} on the specified event bus when complete.
     */
    public void fetchSinglePatient(CrudEventBus bus, String uuid) {
        mTaskFactory.newFetchItemTask(
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
    public void addOrderExecutedEncounter(CrudEventBus bus, Patient patient, String orderUuid) {
        addEncounter(bus, patient, new Encounter(
                patient.uuid, null, DateTime.now(), null, new String[]{orderUuid}
        ));
    }

    /**
     * Asynchronously adds an encounter to a patient, posting a
     * {@link ItemCreatedEvent} when complete.
     */
    public void addEncounter(CrudEventBus bus, Patient patient, Encounter encounter) {
        mTaskFactory.newAddEncounterTask(patient, encounter, bus).execute();
    }

    AppModel(ContentResolver contentResolver,
             TaskFactory taskFactory) {
        mContentResolver = contentResolver;
        mTaskFactory = taskFactory;
    }

    public void voidObservation(CrudEventBus bus, VoidObs obs) {
        mTaskFactory.newVoidObsAsyncTask(obs, bus).execute();
    }

    private static class FetchTypedCursorAsyncTask<T extends Base>
        extends AsyncTask<Void, Void, TypedCursor<T>> {

        private final Uri mContentUri;
        private final String[] mProjection;
        private final Class<T> mClazz;
        private final ContentResolver mContentResolver;
        private final SimpleSelectionFilter mFilter;
        private final String mConstraint;
        private final CursorLoader<T> mLoader;
        private final CrudEventBus mBus;

        public FetchTypedCursorAsyncTask(
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
            mBus.post(TypedCursorFetchedEventFactory.createEvent(mClazz, result));
        }
    }
}
