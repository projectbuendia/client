package org.msf.records.data.app;

import android.content.ContentResolver;
import android.database.Cursor;
import android.os.AsyncTask;

import org.msf.records.data.app.converters.AppTypeConverter;
import org.msf.records.data.app.converters.AppTypeConverters;
import org.msf.records.events.DefaultCrudEventBus;
import org.msf.records.events.CrudEventBus;
import org.msf.records.events.data.SingleItemFetchFailedEvent;
import org.msf.records.events.data.SingleItemFetchedEvent;
import org.msf.records.events.data.TypedCursorFetchedEvent;
import org.msf.records.filter.SimpleSelectionFilter;
import org.msf.records.filter.UuidFilter;
import org.msf.records.sync.PatientProjection;
import org.msf.records.sync.PatientProviderContract;

import de.greenrobot.event.NoSubscriberEvent;

/**
 * A model that manages all data access within the application.
 *
 * <p>This model's {@code fetch} methods often provide {@link TypedCursor}s as results, which MUST
 * be closed when the consumer is done with them.
 */
public class AppModel {

    private final ContentResolver mContentResolver;
    private final AppTypeConverters mConverters;

    private final CrudEventBusErrorSubscriber mCrudEventBusErrorSubscriber;

    AppModel(ContentResolver contentResolver, AppTypeConverters converters) {
        mContentResolver = contentResolver;
        mConverters = converters;
        mCrudEventBusErrorSubscriber = new CrudEventBusErrorSubscriber();
    }

    /**
     * Asynchronously fetches patients, posting a {@link TypedCursorFetchedEvent} with
     * {@link AppPatient}s on the specified event bus when complete.
     */
    public void fetchPatients(CrudEventBus bus) {
        bus.register(mCrudEventBusErrorSubscriber);

        // TODO(dxchen): Asynchronously fetch patients.
    }

    /**
     * Asynchronously fetches a single patient by UUID, posting a {@link SingleItemFetchedEvent}
     * with the {@link AppPatient} on the specified event bus when complete.
     */
    public void fetchSinglePatient(CrudEventBus bus, String uuid) {
        FetchSingleAsyncTask<AppPatient> task = new FetchSingleAsyncTask<>(
                mContentResolver, new UuidFilter(), uuid, mConverters.patient, bus);
        task.execute();
    }

    /**
     * Asynchronously fetches patients, posting a {@link TypedCursorFetchedEvent} with
     * {@link AppUser}s on the specified event bus when complete.
     */
    public void fetchUsers(CrudEventBus bus) {
        // Register for error events so that we can close cursors if we need to.
        bus.register(mCrudEventBusErrorSubscriber);

        // TODO(dxchen): Asynchronously fetch users.
    }

    /**
     * A subscriber that handles error events posted to {@link CrudEventBus}es.
     */
    @SuppressWarnings("unused") // Called by reflection from event bus.
    private static class CrudEventBusErrorSubscriber {

        /**
         * Handles {@link NoSubscriberEvent}s.
         */
        public void onEvent(NoSubscriberEvent event) {
            if (event.originalEvent instanceof TypedCursorFetchedEvent<?>) {
                // If no subscribers were registered for a DataFetchedEvent, then the TypedCursor in
                // the event won't be managed by anyone else; therefore, we close it ourselves.
                ((TypedCursorFetchedEvent<?>) event.originalEvent).mCursor.close();
            }
        }
    }

    // TODO(dxchen): Implement.
    private abstract static class FetchTypedCursorAsyncTask<T extends AppTypeBase>
            extends AsyncTask<Void, Void, Object> {}

    /**
     * An {@link AsyncTask} that fetches a single item from the data store.
     */
    private static class FetchSingleAsyncTask<T extends AppTypeBase>
            extends AsyncTask<Void, Void, Object> {

        private final ContentResolver mContentResolver;
        private final SimpleSelectionFilter mFilter;
        private final String mConstraint;
        private final AppTypeConverter<T> mConverter;
        private final CrudEventBus mBus;

        public FetchSingleAsyncTask(
                ContentResolver contentResolver,
                SimpleSelectionFilter filter,
                String constraint,
                AppTypeConverter<T> converter,
                CrudEventBus bus) {
            mContentResolver = contentResolver;
            mFilter = filter;
            mConstraint = constraint;
            mConverter = converter;
            mBus = bus;
        }

        @Override
        protected Object doInBackground(Void... voids) {
            // TODO(dxchen): Refactor this (and possibly FilterQueryProviderFactory) to support
            // different types of queries.
            Cursor cursor = null;
            try {
                cursor = mContentResolver.query(
                        PatientProviderContract.CONTENT_URI,
                        PatientProjection.getProjectionColumns(),
                        mFilter.getSelectionString(),
                        mFilter.getSelectionArgs(mConstraint),
                        null);

                if (cursor == null || !cursor.moveToFirst()) {
                    return new SingleItemFetchFailedEvent();
                }

                return new SingleItemFetchedEvent<>(mConverter.fromCursor(cursor));
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        @Override
        protected void onPostExecute(Object result) {
            mBus.post(result);
        }
    }
}
