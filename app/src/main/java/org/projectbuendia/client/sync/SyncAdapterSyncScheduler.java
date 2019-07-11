package org.projectbuendia.client.sync;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.PeriodicSync;
import android.os.Bundle;

/**
 * A SyncScheduler that schedules sync operations by making requests to Android's
 * SyncAdapter framework and praying that it calls the sync engine... someday.
 * See SyncAdapterService, through which the Android system invokes the SyncEngine.
 */
public class SyncAdapterSyncScheduler implements SyncScheduler {
    private final Account account;
    private final String authority;

    public SyncAdapterSyncScheduler(Account account, String authority) {
        this.account = account;
        this.authority = authority;
    }

    @Override public void requestSync(Bundle options) {
        ContentResolver.requestSync(account, authority, options);
    }

    @Override public void stopSyncing() {
        ContentResolver.cancelSync(account, authority);
    }

    @Override public void setPeriodicSync(Bundle options, int periodSec) {
        if (periodSec > 0) {
            ContentResolver.setIsSyncable(account, authority, 1);
            ContentResolver.setSyncAutomatically(account, authority, true);
            ContentResolver.setMasterSyncAutomatically(true);
            ContentResolver.addPeriodicSync(account, authority, options, periodSec);
        } else {
            ContentResolver.setSyncAutomatically(account, authority, false);
            try {
                for (PeriodicSync ps : ContentResolver.getPeriodicSyncs(account, authority)) {
                    ContentResolver.removePeriodicSync(account, authority, ps.extras);
                }
            } catch (SecurityException e) { /* ignore */ }
        }
    }

    @Override public boolean isRunningOrPending() {
        return ContentResolver.isSyncActive(account, authority) ||
            ContentResolver.isSyncPending(account, authority);
    }
}
