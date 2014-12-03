package org.msf.records.mvcmodels;

import org.msf.records.events.CreatePatientSucceededEvent;
import org.msf.records.events.sync.SyncFailedEvent;
import org.msf.records.events.sync.SyncFinishedEvent;
import org.msf.records.events.sync.SyncSucceededEvent;
import org.msf.records.sync.SyncManager;

import java.util.LinkedHashSet;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A model for the patient chart.
 */
public class PatientChartModel {

    /**
     * A listener that listens for fetch observations results.
     */
    public interface OnObservationsFetchedListener {

        /**
         * Called when the observations have been fetched.
         */
        void onObservationsFetched();

        /**
         * Called when unable to fetch observations.
         */
        void onObservationsFetchFailed();
    }

    // TODO(dxchen): Dagger this!
    public static final PatientChartModel INSTANCE = new PatientChartModel();

    private final LinkedHashSet<OnObservationsFetchedListener> mOnObservationsFetchedListenerQueue =
            new LinkedHashSet<OnObservationsFetchedListener>();

    public synchronized void fetchObservations(OnObservationsFetchedListener listener) {
        if (SyncManager.INSTANCE.isSyncing()) {
            mOnObservationsFetchedListenerQueue.add(listener);
        } else {
            listener.onObservationsFetched();
        }
    }

    public synchronized void unregisterFetchObservations(OnObservationsFetchedListener listener) {
        mOnObservationsFetchedListenerQueue.remove(listener);
    }

    public synchronized void onEvent(CreatePatientSucceededEvent event) {
        // When a new patient is created, sync from the server to get the latest info for
        // everything.
        SyncManager.INSTANCE.forceSync();
    }

    private synchronized void onEvent(SyncSucceededEvent event) {
        try {
            for (OnObservationsFetchedListener listener : mOnObservationsFetchedListenerQueue) {
                listener.onObservationsFetched();
            }
        } finally {
            mOnObservationsFetchedListenerQueue.clear();
        }
    }

    private synchronized void onEvent(SyncFailedEvent event) {
        try {
            for (OnObservationsFetchedListener listener : mOnObservationsFetchedListenerQueue) {
                listener.onObservationsFetchFailed();
            }
        } finally {
            mOnObservationsFetchedListenerQueue.clear();
        }
    }
}
