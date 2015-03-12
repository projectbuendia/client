package org.msf.records.sync;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;

import org.msf.records.App;
import org.msf.records.events.sync.SyncCanceledEvent;
import org.msf.records.events.sync.SyncFailedEvent;
import org.msf.records.events.sync.SyncProgressEvent;
import org.msf.records.events.sync.SyncStartedEvent;
import org.msf.records.events.sync.SyncSucceededEvent;
import org.msf.records.sync.providers.Contracts;
import org.msf.records.utils.Logger;

import de.greenrobot.event.EventBus;

/**
 * An object that provides callers a way of managing the sync process and responding to sync events.
 */
public class SyncManager {

    private static final Logger LOG = Logger.create();

    static final String SYNC_STATUS = "sync-status";
    static final int STARTED = 1;
    static final int COMPLETED = 2;
    static final int FAILED = 3;
    static final int IN_PROGRESS = 4;
    public static final int CANCELED = 5;

    public static final String SYNC_PROGRESS = "sync-progress";
    public static final String SYNC_PROGRESS_LABEL = "sync-progress-label";

    /**
     * Cancels an in-flight, non-periodic sync.
     */
    public void cancelOnDemandSync() {
        ContentResolver.cancelSync(
                GenericAccountService.getAccount(),
                Contracts.CONTENT_AUTHORITY);

        // If sync was pending, it should now be idle and we can consider the sync immediately
        // canceled.
        if (!isSyncPending() && !isSyncing()) {
            LOG.i("Sync was canceled before it began -- immediately firing SyncCanceledEvent.");
            EventBus.getDefault().post(new SyncCanceledEvent());
        }
    }

    /**
     * Forces a sync to occur immediately.
     */
    public void forceSync() {
        LOG.d("Forcing new sync");
        GenericAccountService.triggerRefresh(
                PreferenceManager.getDefaultSharedPreferences(App.getInstance()));
    }

    /**
     * Initiates an incremental sync of observations.  No-op if incremental observation
     * update is disabled.
     */
    public void incrementalObservationSync() {
        GenericAccountService.triggerIncrementalObservationSync(
                PreferenceManager.getDefaultSharedPreferences(App.getInstance()));
    }

    /**
     * Returns {@code true} if a sync is active.
    */
    public boolean isSyncing() {
        return
                ContentResolver.isSyncActive(
                        GenericAccountService.getAccount(),
                        Contracts.CONTENT_AUTHORITY);
    }

    /**
     * Returns {@code true} if a sync is pending.
     */
    public boolean isSyncPending() {
        return ContentResolver.isSyncPending(
                        GenericAccountService.getAccount(),
                        Contracts.CONTENT_AUTHORITY);
    }

    /**
     * A {@link BroadcastReceiver} that listens for sync status broadcasts sent by
     * {@link SyncAdapter}.
     */
    public static class SyncStatusBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            int syncStatus = intent.getIntExtra(SYNC_STATUS, -1 /*defaultValue*/);
            switch (syncStatus) {
                case STARTED:
                    LOG.i("Sync started");
                    EventBus.getDefault().post(new SyncStartedEvent());
                    break;
                case COMPLETED:
                    LOG.i("Sync completed");
                    EventBus.getDefault().post(new SyncSucceededEvent());
                    break;
                case FAILED:
                    LOG.i("Sync failed");
                    EventBus.getDefault().post(new SyncFailedEvent());
                    break;
                case IN_PROGRESS:
                    LOG.i("Sync is continuing");
                    int syncProgress = intent.getIntExtra(SYNC_PROGRESS, 0);
                    String syncLabel = intent.getStringExtra(SYNC_PROGRESS_LABEL);
                    EventBus.getDefault().post(new SyncProgressEvent(syncProgress, syncLabel));
                    break;
                case CANCELED:
                    LOG.i("Sync was canceled.");
                    EventBus.getDefault().post(new SyncCanceledEvent());
                    break;
                case -1:
                    LOG.i("Sync status broadcast intent received without a status code.");
                default:
                    LOG.i(
                            "Sync status broadcast intent received with unknown status %1$d.",
                            syncStatus);
            }
        }
    }
}
