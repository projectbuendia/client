package org.msf.records.ui.patientlist;

import android.support.v4.widget.SwipeRefreshLayout;

import org.msf.records.events.sync.SyncFailedEvent;
import org.msf.records.events.sync.SyncFinishedEvent;
import org.msf.records.events.sync.SyncSucceededEvent;
import org.msf.records.sync.SyncManager;
import org.msf.records.utils.EventBusRegistrationInterface;
import org.msf.records.utils.Logger;

import de.greenrobot.event.EventBus;

/**
 * Controller for non-inherited parts of {@link PatientListFragment}.
 *
 * <p>Avoid adding untestable dependencies to this class.
 */
public class PatientListController {

    private static final Logger LOG = Logger.create();

    private final SwipeRefreshLayout.OnRefreshListener mRefreshListener =
            new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    forceSync();
                }
            };

    private final class SyncSubscriber {
        public synchronized void onEventMainThread(SyncSucceededEvent event) {
            stopRefreshing();
        }

        public synchronized void onEventMainThread(SyncFailedEvent event) {
            if (mIsRefreshing) {
                mUi.showSyncError();
            }
            stopRefreshing();
        }
    }

    private final SyncSubscriber mSyncSubscriber = new SyncSubscriber();

    private final Ui mUi;

    private final SyncManager mSyncManager;

    private boolean mIsRefreshing;

    private EventBusRegistrationInterface mEventBus;

    public interface Ui {

        void setRefreshing(boolean refreshing);

        void showSyncError();
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
     * Forces a new sync of all data from server, unless a sync is already in progress.
     */
    public void forceSync() {
        if (!mIsRefreshing) {
            LOG.d("forceSync");

            //triggers app wide data refresh
            mSyncManager.forceSync();
            mIsRefreshing = true;
        }
    }

    private void stopRefreshing() {
        mUi.setRefreshing(false);
        mIsRefreshing = false;
    }

    public SwipeRefreshLayout.OnRefreshListener getOnRefreshListener() {
        return mRefreshListener;
    }
}
