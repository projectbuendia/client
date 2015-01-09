package org.msf.records.data.app;

import android.content.ContentResolver;
import android.database.Cursor;
import android.os.AsyncTask;

import org.msf.records.data.app.converters.AppTypeConverter;
import org.msf.records.data.app.converters.AppTypeConverters;
import org.msf.records.data.app.tasks.AppAddPatientAsyncTask;
import org.msf.records.data.app.tasks.AppAsyncTaskFactory;
import org.msf.records.data.app.tasks.AppUpdatePatientAsyncTask;
import org.msf.records.data.app.tasks.FetchSingleAsyncTask;
import org.msf.records.events.CleanupSubscriber;
import org.msf.records.events.CrudEventBus;
import org.msf.records.events.data.AppLocationTreeFetchedEvent;
import org.msf.records.events.data.SingleItemFetchedEvent;
import org.msf.records.events.data.TypedCursorFetchedEvent;
import org.msf.records.events.data.TypedCursorFetchedEventFactory;
import org.msf.records.filter.SimpleSelectionFilter;
import org.msf.records.filter.UuidFilter;
import org.msf.records.net.Server;
import org.msf.records.sync.LocationProjection;
import org.msf.records.sync.PatientProjection;
import org.msf.records.sync.providers.Contracts;

import de.greenrobot.event.NoSubscriberEvent;

/**
 * A model that manages all data access within the application.
 *
 * <p>This model's {@code fetch} methods often provide {@link TypedCursor}s as results, which MUST
 * be closed when the consumer is done with them.
 *
 * <p>Updates done through this model are written through to a backing {@link Server}; callers do
 * not need to worry about the implementation details of this.
 *
 * <p>You can use
 */
public class AppModel {

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
                new UuidFilter(), uuid, mConverters.patient, bus);
        task.execute();
    }

    /**
     * Asynchronously fetches patients, posting a {@link TypedCursorFetchedEvent} with
     * {@link AppUser}s on the specified event bus when complete.
     */
    public void fetchUsers(CrudEventBus bus) {
        // Register for error events so that we can close cursors if we need to.
        bus.registerCleanupSubscriber(new CrudEventBusCleanupSubscriber(bus));

        // TODO(dxchen): Asynchronously fetch users.
    }

    /**
     * Asynchronously adds a patient, posting a
     * {@link org.msf.records.events.data.SingleItemCreatedEvent} with the newly-added patient on
     * the specified event bus when complete.
     */
    public void addPatient(CrudEventBus bus, AppPatientDelta patientDelta) {
        AppAddPatientAsyncTask task = mTaskFactory.newAddPatientAsyncTask(patientDelta, bus);
        task.execute();
    }

    /**
     * Asynchronously updates a patient, posting a
     * {@link org.msf.records.events.data.SingleItemUpdatedEvent} with the updated
     * {@link AppPatient} on the specified event bus when complete.
     */
    public void updatePatient(
            CrudEventBus bus, AppPatient originalPatient, AppPatientDelta patientDelta) {
        AppUpdatePatientAsyncTask task =
                mTaskFactory.newUpdatePatientAsyncTask(originalPatient, patientDelta, bus);
        task.execute();
    }

    /**
     * A subscriber that handles error events posted to {@link CrudEventBus}es.
     */
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
                        .fromTypedCursor(new TypedConvertedCursor<>(mConverter, cursor));
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

    private static class FetchTypedCursorAsyncTask<T>
            extends AsyncTask<Void, Void, TypedCursor<T>> {

        private final Class<T> mClazz;
        private final ContentResolver mContentResolver;
        private final SimpleSelectionFilter mFilter;
        private final String mConstraint;
        private final AppTypeConverter<T> mConverter;
        private final CrudEventBus mBus;

        public FetchTypedCursorAsyncTask(
                Class<T> clazz,
                ContentResolver contentResolver,
                SimpleSelectionFilter filter,
                String constraint,
                AppTypeConverter<T> converter,
                CrudEventBus bus) {
            mClazz = clazz;
            mContentResolver = contentResolver;
            mFilter = filter;
            mConstraint = constraint;
            mConverter = converter;
            mBus = bus;
        }

        @Override
        protected TypedCursor<T> doInBackground(Void... voids) {
            // TODO(dxchen): Refactor this (and possibly FilterQueryProviderFactory) to support
            // different types of queries.
            Cursor cursor = null;
            try {
                cursor = mContentResolver.query(
                        Contracts.Patients.CONTENT_URI,
                        PatientProjection.getProjectionColumns(),
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
