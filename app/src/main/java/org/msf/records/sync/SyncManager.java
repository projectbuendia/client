package org.msf.records.sync;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.util.Log;

import org.msf.records.App;
import org.msf.records.events.sync.SyncFailedEvent;
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

    /**
     * Forces a sync to occur immediately.
     */
    public void forceSync() {
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
     * Returns {@code true} if a sync is pending or active.
     */
    public boolean isSyncing() {
        return
                ContentResolver.isSyncActive(
                        GenericAccountService.getAccount(),
                        Contracts.CONTENT_AUTHORITY)
                || ContentResolver.isSyncPending(
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
