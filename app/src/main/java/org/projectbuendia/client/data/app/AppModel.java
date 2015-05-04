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

package org.projectbuendia.client.data.app;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;

import org.projectbuendia.client.data.app.converters.AppTypeConverter;
import org.projectbuendia.client.data.app.converters.AppTypeConverters;
import org.projectbuendia.client.data.app.tasks.AppAddEncounterAsyncTask;
import org.projectbuendia.client.data.app.tasks.AppAddPatientAsyncTask;
import org.projectbuendia.client.data.app.tasks.AppAsyncTaskFactory;
import org.projectbuendia.client.data.app.tasks.AppUpdatePatientAsyncTask;
import org.projectbuendia.client.data.app.tasks.FetchSingleAsyncTask;
import org.projectbuendia.client.events.CleanupSubscriber;
import org.projectbuendia.client.events.CrudEventBus;
import org.projectbuendia.client.events.data.AppLocationTreeFetchedEvent;
import org.projectbuendia.client.events.data.SingleItemFetchedEvent;
import org.projectbuendia.client.events.data.TypedCursorFetchedEvent;
import org.projectbuendia.client.events.data.TypedCursorFetchedEventFactory;
import org.projectbuendia.client.filter.db.SimpleSelectionFilter;
import org.projectbuendia.client.filter.db.patient.UuidFilter;
import org.projectbuendia.client.net.Server;
import org.projectbuendia.client.sync.PatientProjection;
import org.projectbuendia.client.sync.providers.Contracts;
import org.projectbuendia.client.utils.Logger;

import de.greenrobot.event.NoSubscriberEvent;

/**
 * A model that manages all data access within the application.
 *
 * <p>This model's {@code fetch} methods often provide {@link TypedCursor}s as results, which MUST
 * be closed when the consumer is done with them.
 *
 * <p>Updates done through this model are written through to a backing {@link Server}; callers do
 * not need to worry about the implementation details of this.
 */
public class AppModel {
    private static final Logger LOG = Logger.create();

    private final ContentResolver mContentResolver;
    private final AppTypeConverters mConverters;
    private final AppAsyncTaskFactory mTaskFactory;

    AppModel(
            ContentResolver contentResolver,
            AppTypeConverters converters,
            AppAsyncTaskFactory taskFactory) {
        mContentResolver = contentResolver;
        mConverters = converters;
        mTaskFactory = taskFactory;
    }

    /**
     * Returns true iff the model has previously been fully downloaded from the server--that is, if
     * locations, patients, users, charts, and observations were all downloaded at some point. Note
     * that this data may be out-of-date, but must be present in some form for proper operation of
     * the app.
     */
    public boolean isFullModelAvailable() {
        // The sync process is transactional, but in rare cases, a sync may complete without ever
        // having started--this is the case if user data is cleared midsync, for example. To check
        // that a sync actually completed, we look at the FULL_SYNC_START_TIME and
        // FULL_SYNC_END_TIME columns in the Misc table, which are written to as the first and
        // last operations of a complete sync. If both of these fields are present, and the last
        // end time is greater than the last start time, then a full sync must have completed.
        Cursor c = null;
        try {
            c = mContentResolver.query(
                    Contracts.Misc.CONTENT_URI,
                    new String[]{
                            Contracts.Misc.FULL_SYNC_START_TIME,
                            Contracts.Misc.FULL_SYNC_END_TIME,
                            Contracts.Misc.OBS_SYNC_TIME
                    },
                    null,
                    null,
                    null);
            LOG.d("Sync timing result count: %d", c.getCount());
            if (c.moveToNext()) {
                LOG.d("Sync timings -- FULL_SYNC_START(%d), FULL_SYNC_END(%d), OBS_SYNC_TIME(%d)",
                        c.getLong(0), c.getLong(1), c.getLong(2));
                return !c.isNull(0) && !c.isNull(1) && c.getLong(1) >= c.getLong(0);
            } else {
                return false;
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    /**
     * Asynchronously fetches all locations as a tree, posting an
     * {@link AppLocationTreeFetchedEvent} on the specified event bus when complete.
     */
    public void fetchLocationTree(CrudEventBus bus, String locale) {
        bus.registerCleanupSubscriber(new CrudEventBusCleanupSubscriber(bus));

        FetchLocationTreeAsyncTask task = new FetchLocationTreeAsyncTask(
                mContentResolver,
                locale,
                mConverters.location,
                bus);
        task.execute();
    }

    /**
     * Asynchronously fetches patients, posting a {@link TypedCursorFetchedEvent} with
     * {@link AppPatient}s on the specified event bus when complete.
     */
    public void fetchPatients(CrudEventBus bus, SimpleSelectionFilter filter, String constraint) {
        bus.registerCleanupSubscriber(new CrudEventBusCleanupSubscriber(bus));

        FetchTypedCursorAsyncTask<AppPatient> task = new FetchTypedCursorAsyncTask<>(
                Contracts.Patients.CONTENT_URI,
                PatientProjection.getProjectionColumns(),
                AppPatient.class,
                mContentResolver,
                filter,
                constraint,
                mConverters.patient,
                bus);
        task.execute();
    }

    /**
     * Asynchronously fetches a single patient by UUID, posting a {@link SingleItemFetchedEvent}
     * with the {@link AppPatient} on the specified event bus when complete.
     */
    public void fetchSinglePatient(CrudEventBus bus, String uuid) {
        FetchSingleAsyncTask<AppPatient> task = mTaskFactory.newFetchSingleAsyncTask(
                Contracts.Patients.CONTENT_URI,
                PatientProjection.getProjectionColumns(),
                new UuidFilter(),
                uuid,
                mConverters.patient,
                bus);
        task.execute();
    }

    /**
     * Asynchronously fetches patients, posting a {@link TypedCursorFetchedEvent} with
     * {@link AppUser}s on the specified event bus when complete.
     */
    public void fetchUsers(CrudEventBus bus) {
        // Register for error events so that we can close cursors if we need to.
        bus.registerCleanupSubscriber(new CrudEventBusCleanupSubscriber(bus));

        // TODO: Asynchronously fetch users or delete this function.
    }

    /**
     * Asynchronously adds a patient, posting a
     * {@link org.projectbuendia.client.events.data.SingleItemCreatedEvent} with the newly-added patient on
     * the specified event bus when complete.
     */
    public void addPatient(CrudEventBus bus, AppPatientDelta patientDelta) {
        AppAddPatientAsyncTask task = mTaskFactory.newAddPatientAsyncTask(patientDelta, bus);
        task.execute();
    }

    /**
     * Asynchronously updates a patient, posting a
     * {@link org.projectbuendia.client.events.data.SingleItemUpdatedEvent} with the updated
     * {@link AppPatient} on the specified event bus when complete.
     */
    public void updatePatient(
            CrudEventBus bus, AppPatient originalPatient, AppPatientDelta patientDelta) {
        AppUpdatePatientAsyncTask task =
                mTaskFactory.newUpdatePatientAsyncTask(originalPatient, patientDelta, bus);
        task.execute();
    }

    /**
     * Asynchronously adds an encounter to a patient, posting a
     * {@link org.projectbuendia.client.events.data.SingleItemCreatedEvent}.
     */
    public void addEncounter(CrudEventBus bus, AppPatient appPatient, AppEncounter appEncounter) {
        AppAddEncounterAsyncTask task =
                mTaskFactory.newAddEncounterAsyncTask(appPatient, appEncounter, bus);
        task.execute();
    }

    /** A subscriber that handles error events posted to {@link CrudEventBus}es. */
    private static class CrudEventBusCleanupSubscriber implements CleanupSubscriber {

        private final CrudEventBus mBus;

        public CrudEventBusCleanupSubscriber(CrudEventBus bus) {
            mBus = bus;
        }

        @Override
        @SuppressWarnings("unused") // Called by reflection from event bus.
        public void onEvent(NoSubscriberEvent event) {
            if (event.originalEvent instanceof TypedCursorFetchedEvent<?>) {
                // If no subscribers were registered for a DataFetchedEvent, then the TypedCursor in
                // the event won't be managed by anyone else; therefore, we close it ourselves.
                ((TypedCursorFetchedEvent<?>) event.originalEvent).cursor.close();
            } else if (event.originalEvent instanceof AppLocationTreeFetchedEvent) {
                ((AppLocationTreeFetchedEvent) event.originalEvent).tree.close();
            }

            mBus.unregisterCleanupSubscriber(this);
        }

        @Override
        public void onAllUnregistered() {
            mBus.unregisterCleanupSubscriber(this);
        }
    }

    private static class FetchLocationTreeAsyncTask extends AsyncTask<Void, Void, AppLocationTree> {

        private final ContentResolver mContentResolver;
        private final String mLocale;
        private final AppTypeConverter<AppLocation> mConverter;
        private final CrudEventBus mBus;

        public FetchLocationTreeAsyncTask(
                ContentResolver contentResolver,
                String locale,
                AppTypeConverter<AppLocation> converter,
                CrudEventBus bus) {
            mContentResolver = contentResolver;
            mLocale = locale;
            mConverter = converter;
            mBus = bus;
        }

        @Override
        protected AppLocationTree doInBackground(Void... voids) {
            Cursor cursor = null;
            try {
                cursor = mContentResolver.query(
                        Contracts.LocalizedLocations.getUri(mLocale),
                        null,
                        null,
                        null,
                        null);

                return AppLocationTree
                        .forTypedCursor(new TypedConvertedCursor<>(mConverter, cursor));
            } catch (Exception e) {
                if (cursor != null) {
                    cursor.close();
                }

                throw e;
            }
        }

        @Override
        protected void onPostExecute(AppLocationTree result) {
            mBus.post(new AppLocationTreeFetchedEvent(result));
        }
    }

    private static class FetchTypedCursorAsyncTask<T extends AppTypeBase>
            extends AsyncTask<Void, Void, TypedCursor<T>> {

        private final Uri mContentUri;
        private final String[] mProjection;
        private final Class<T> mClazz;
        private final ContentResolver mContentResolver;
        private final SimpleSelectionFilter mFilter;
        private final String mConstraint;
        private final AppTypeConverter<T> mConverter;
        private final CrudEventBus mBus;

        public FetchTypedCursorAsyncTask(
                Uri contentUri,
                String[] projection,
                Class<T> clazz,
                ContentResolver contentResolver,
                SimpleSelectionFilter<T> filter,
                String constraint,
                AppTypeConverter<T> converter,
                CrudEventBus bus) {
            mContentUri = contentUri;
            mProjection = projection;
            mClazz = clazz;
            mContentResolver = contentResolver;
            mFilter = filter;
            mConstraint = constraint;
            mConverter = converter;
            mBus = bus;
        }

        @Override
        protected TypedCursor<T> doInBackground(Void... voids) {
            Cursor cursor = null;
            try {
                cursor = mContentResolver.query(
                        mContentUri,
                        mProjection,
                        mFilter.getSelectionString(),
                        mFilter.getSelectionArgs(mConstraint),
                        null);

                return new TypedConvertedCursor<>(mConverter, cursor);
            } catch (Exception e) {
                if (cursor != null) {
                    cursor.close();
                }

                throw e;
            }
        }

        @Override
        protected void onPostExecute(TypedCursor<T> result) {
            mBus.post(TypedCursorFetchedEventFactory.createEvent(mClazz, result));
        }
    }
}
