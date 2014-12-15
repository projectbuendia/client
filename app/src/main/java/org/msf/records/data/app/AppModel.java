package org.msf.records.data.app;

import android.os.AsyncTask;

import org.msf.records.data.app.converters.AppTypeConverters;
import org.msf.records.data.app.tasks.AppAddPatientAsyncTask;
import org.msf.records.data.app.tasks.AppAsyncTaskFactory;
import org.msf.records.data.app.tasks.AppUpdatePatientAsyncTask;
import org.msf.records.data.app.tasks.FetchSingleAsyncTask;
import org.msf.records.events.CrudEventBus;
import org.msf.records.events.data.SingleItemFetchedEvent;
import org.msf.records.events.data.TypedCursorFetchedEvent;
import org.msf.records.filter.UuidFilter;
import org.msf.records.net.Server;

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

    private static final String TAG = AppModel.class.getSimpleName();

    private final AppTypeConverters mConverters;
    private final AppAsyncTaskFactory mTaskFactory;

    AppModel(AppTypeConverters converters, AppAsyncTaskFactory taskFactory) {
        mConverters = converters;
        mTaskFactory = taskFactory;
    }

    /**
     * Asynchronously fetches patients, posting a {@link TypedCursorFetchedEvent} with
     * {@link AppPatient}s on the specified event bus when complete.
     */
    public void fetchPatients(CrudEventBus bus) {
        // Register for error events so that we can close cursors if we need to.
        bus.register(new CrudEventBusErrorSubscriber(bus));

        // TODO(dxchen): Asynchronously fetch patients.
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
        bus.register(new CrudEventBusErrorSubscriber(bus));

        // TODO(dxchen): Asynchronously fetch users.
    }

    // TODO(dxchen): Consider defining a special PatientUpdatedEvent.
    /**
     * Asynchronously adds a patient, posting a {@link SingleItemFetchedEvent} with the newly-added
     * patient on the specified event bus when complete.
     */
    public void addPatient(CrudEventBus bus, AppPatientDelta patientDelta) {
        AppAddPatientAsyncTask task = mTaskFactory.newAddPatientAsyncTask(patientDelta, bus);
        task.execute();
    }

    // TODO(dxchen): Consider defining a special PatientUpdatedEvent.
    /**
     * Asynchronously updates a patient, posting a {@link SingleItemFetchedEvent} with the updated
     * {@link AppPatient} on the specified event bus when complete.
     */
    public void updatePatient(CrudEventBus bus, String uuid, AppPatientDelta patientDelta) {
        AppUpdatePatientAsyncTask task =
                mTaskFactory.newUpdatePatientAsyncTask(uuid, patientDelta, bus);
        task.execute();
    }

    /**
     * A subscriber that handles error events posted to {@link CrudEventBus}es.
     */
    @SuppressWarnings("unused") // Called by reflection from event bus.
    private static class CrudEventBusErrorSubscriber {

        private final CrudEventBus mBus;

        public CrudEventBusErrorSubscriber(CrudEventBus bus) {
            mBus = bus;
        }

        /**
         * Handles {@link NoSubscriberEvent}s.
         */
        public void onEvent(NoSubscriberEvent event) {
            if (event.originalEvent instanceof TypedCursorFetchedEvent<?>) {
                // If no subscribers were registered for a DataFetchedEvent, then the TypedCursor in
                // the event won't be managed by anyone else; therefore, we close it ourselves.
                ((TypedCursorFetchedEvent<?>) event.originalEvent).mCursor.close();
            }

            mBus.unregister(this);
        }
    }

    // TODO(dxchen): Implement.
    private abstract static class FetchTypedCursorAsyncTask<T extends AppTypeBase>
            extends AsyncTask<Void, Void, Object> {}
}
