package org.msf.records.ui.patientlist;

import org.msf.records.App;
import org.msf.records.events.sync.SyncFailedEvent;
import org.msf.records.events.sync.SyncSucceededEvent;
import org.msf.records.sync.SyncManager;
import org.msf.records.utils.EventBusRegistrationInterface;
import org.msf.records.utils.Logger;

/**
 * Controller for non-inherited parts of {@link PatientListFragment}.
 *
 * <p>Avoid adding untestable dependencies to this class.
 */
public class PatientListController {

    private static final Logger LOG = Logger.create();

    private final class SyncSubscriber {
        public synchronized void onEventMainThread(SyncSucceededEvent event) {
            onSyncFinished(true);
        }

        public synchronized void onEventMainThread(SyncFailedEvent event) {
            onSyncFinished(false);
        }
    }

    private final SyncSubscriber mSyncSubscriber = new SyncSubscriber();

    private final Ui mUi;

    private final SyncManager mSyncManager;

    /** True if a full sync initiated by this activity is in progress. */
    private boolean mInitiatedFullSync;

    private EventBusRegistrationInterface mEventBus;

    public interface Ui {
        /** Stops the refresh-in-progress animation. */
        void stopRefreshAnimation();

        /** Notifies the user that the refresh failed. */
        void showRefreshError();

        /** Notifies the user that the API is unhealthy. */
        void showApiHealthProblem();
    }

    /**
     * Initializes this with the given UI, sync manager, and event bus.
     *
     * @param ui {@link Ui} that will respond to list refresh events
     * @param syncManager a {@link SyncManager} for performing sync operations
     * @param eventBus the {@link EventBusRegistrationInterface} that will listen for sync events
     */
    public PatientListController(
            Ui ui, SyncManager syncManager, EventBusRegistrationInterface eventBus) {
        mUi = ui;
        mSyncManager = syncManager;
        mEventBus = eventBus;
    }

    public void init() {
        mEventBus.register(mSyncSubscriber);
    }

    public void suspend() {
        mEventBus.unregister(mSyncSubscriber);
    }

    /**
     * Forces a new sync of all data from server, unless this activity is already
     * waiting for a previously initiated sync.
     */
    public void onRefreshRequested() {
        if (App.getInstance().getHealthMonitor().isApiUnavailable()) {
            mUi.stopRefreshAnimation();
            mUi.showApiHealthProblem();
        } else if (!mInitiatedFullSync) {
            LOG.d("onRefreshRequested");
            mSyncManager.forceSync();
            mInitiatedFullSync = true;
        }
    }

    private void onSyncFinished(boolean success) {
        mUi.stopRefreshAnimation();
        if (mInitiatedFullSync && !success) {
            mUi.showRefreshError();
        }
        mInitiatedFullSync = false;
    }
}
