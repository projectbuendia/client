package org.msf.records.ui.patientlist;

import android.support.v4.widget.SwipeRefreshLayout;

import org.msf.records.events.sync.SyncFinishedEvent;
import org.msf.records.sync.SyncManager;
import org.msf.records.utils.EventBusRegistrationInterface;
import org.msf.records.utils.Logger;

import de.greenrobot.event.EventBus;

/**
 * Controller for non-inherited parts of {@link org.msf.records.ui.patientlist.PatientListFragment}.
 *
 * <p>Avoid adding untestable dependencies to this class.
 */
public class PatientListController {

    private static final Logger LOG = Logger.create();

    private final SwipeRefreshLayout.OnRefreshListener mRefreshListener =
            new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    if (!mIsRefreshing) {
                        LOG.d("onRefresh");

                        //triggers app wide data refresh
                        // TODO(nfortescue): Incremental sync.
                        mSyncManager.forceSync();
                        mIsRefreshing = true;
                    }
                }
            };

    private final class SyncSubscriber {
        public synchronized void onEventMainThread(SyncFinishedEvent event) {
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
    }

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

    private void stopRefreshing() {
        if (mIsRefreshing) {
            mUi.setRefreshing(false);
            mIsRefreshing = false;
        }
    }

    public SwipeRefreshLayout.OnRefreshListener getOnRefreshListener() {
        return mRefreshListener;
    }
}
