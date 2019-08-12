package org.projectbuendia.client.sync;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.PeriodicSync;
import android.os.Bundle;

import org.projectbuendia.client.utils.Logger;

import java.util.ArrayList;
import java.util.List;

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
    private final List<PeriodicSync> periodicSyncs = new ArrayList<>();

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
            periodicSyncs.add(new PeriodicSync(account, authority, options, periodSec));
        } else {
            ContentResolver.removePeriodicSync(account, authority, options);
        }
    }

    @Override public void clearAllPeriodicSyncs() {
        // Even though the same (account, authority, extras) set might appear more
        // than once in the periodicSyncs list, it's okay to call removePeriodicSync
        // on all of them; it has no effect if there is no matching item to remove.
        for (PeriodicSync ps : periodicSyncs) {
            ContentResolver.removePeriodicSync(ps.account, ps.authority, ps.extras);
        }
        periodicSyncs.clear();
    }

    @Override public boolean isRunningOrPending() {
        return ContentResolver.isSyncActive(account, authority) ||
            ContentResolver.isSyncPending(account, authority);
    }
}
