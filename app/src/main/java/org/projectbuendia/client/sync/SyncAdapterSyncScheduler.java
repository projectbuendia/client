package org.projectbuendia.client.sync;

import android.accounts.Account;
import android.content.ContentResolver;
import android.os.Bundle;

import org.projectbuendia.client.utils.Logger;

/**
 * A SyncScheduler that schedules sync operations by making requests to Android's
 * SyncAdapter framework and praying that it calls the sync engine... someday.
 * See BuendiaSyncAdapterService, through which the Android system invokes the SyncEngine.
 */
public class SyncAdapterSyncScheduler implements SyncScheduler {
    private static final String KEY_OPTIONS = "OPTIONS";
    private static final Logger LOG = Logger.create();

    private final SyncEngine engine;
    private final Account account;
    private final String authority;

    public SyncAdapterSyncScheduler(SyncEngine engine, Account account, String authority) {
        this.engine = engine;
        this.account = account;
        this.authority = authority;
    }

    @Override public void requestSync(Bundle options) {
        LOG.i("requestSync(%s)", options);
        options.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        options.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        ContentResolver.requestSync(account, authority, options);
    }

    @Override public void stopSyncing() {
        LOG.i("stopSyncing()");
        ContentResolver.cancelSync(account, authority);
    }

    @Override public void setPeriodicSync(int periodSec, Bundle options) {
        LOG.i("setPeriodicSync(%d, %s)", periodSec, options);
        if (periodSec > 0) {
            ContentResolver.setIsSyncable(account, authority, 1);
            ContentResolver.setSyncAutomatically(account, authority, true);
            ContentResolver.setMasterSyncAutomatically(true);
            ContentResolver.addPeriodicSync(account, authority, options, periodSec);
        } else {
            ContentResolver.removePeriodicSync(account, authority, options);
        }
    }

    @Override public boolean isRunningOrPending() {
        return ContentResolver.isSyncActive(account, authority) ||
            ContentResolver.isSyncPending(account, authority);
    }
}
