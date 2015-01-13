package org.msf.records.ui.patientlist;

import android.support.v4.widget.SwipeRefreshLayout;

import org.msf.records.events.sync.SyncFinishedEvent;
import org.msf.records.sync.SyncManager;
import org.msf.records.utils.Logger;

import de.greenrobot.event.EventBus;

/**
 * Controller for non-inherited parts of {@link org.msf.records.ui.patientlist.PatientListFragment}.
 *
 * TODO(nfortescue): Incremental sync.
 *
 * Avoid adding untestable dependencies to this class.
 */
public class PatientListController {

    private static final Logger LOG = Logger.create();

    private final SwipeRefreshLayout.OnRefreshListener REFRESH_LISTENER =
            new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    if(!isRefreshing){
                        LOG.d("onRefresh");

                        //triggers app wide data refresh
                        mSyncManager.forceSync();
                        isRefreshing = true;
                    }
                }
            };

    private final Ui mUi;
    private final SyncManager mSyncManager;

    private boolean isRefreshing;

    public interface Ui {
        void setRefreshing(boolean refreshing);
    }

    public PatientListController(Ui ui, SyncManager syncManager) {
        mUi = ui;
        mSyncManager = syncManager;
    }

    public void init() {
        EventBus.getDefault().register(this);
    }

    public void suspend() {
        EventBus.getDefault().unregister(this);
    }

    public synchronized void onEvent(SyncFinishedEvent event) {
        stopRefreshing();
    }

    private void stopRefreshing(){
        if (isRefreshing) {
            mUi.setRefreshing(false);
            isRefreshing = false;
        }
    }

    public SwipeRefreshLayout.OnRefreshListener getOnRefreshListener() {
        return REFRESH_LISTENER;
    }
}
