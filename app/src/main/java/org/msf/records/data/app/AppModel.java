package org.msf.records.data.app;

import android.app.Application;

import org.msf.records.events.CrudEventBus;

import de.greenrobot.event.NoSubscriberEvent;

/**
 * A model that manages all data access within the application.
 *
 * <p>This model's {@code fetch} methods often provide {@link TypedCursor}s as results, which must
 * be closed when the consumer is done with them.
 */
public class AppModel {

    private final Application mApp;

    private final CrudEventBusErrorSubscriber mCrudEventBusErrorSubscriber;

    AppModel(Application app) {
        mApp = app;
        mCrudEventBusErrorSubscriber = new CrudEventBusErrorSubscriber();
    }

    /**
     * Asynchronously fetches patients, posting a {@link DataFetchedEvent}&lt;{@link Patient}&gt;
     * on {@code bus} when complete.
     */
    public void fetchPatients(CrudEventBus bus) {
        bus.register(mCrudEventBusErrorSubscriber);

        // TODO(dxchen): Asynchronously fetch patients.
    }

    /**
     * A subscriber that handles error events posted to {@link CrudEventBus}es.
     */
    private static class CrudEventBusErrorSubscriber {

        /**
         * Handles {@link NoSubscriberEvent}s.
         */
        public void onEvent(NoSubscriberEvent event) {
            if (event.originalEvent instanceof DataFetchedEvent<?>) {
                // If no subscribers were registered for a DataFetchedEvent, then the TypedCursor in
                // the event won't be managed by anyone else; therefore, we close it ourselves.
                ((DataFetchedEvent<?>) event.originalEvent).mCursor.close();
            }
        }
    }

    private static class Patient extends ModelTypeBase {}

    // TODO(dxchen): Move to events package.
    private static class DataFetchedEvent<T extends ModelTypeBase> {

        public TypedCursor<T> mCursor;
    }
}
