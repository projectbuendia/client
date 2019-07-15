package org.projectbuendia.client.sync;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.PeriodicSync;
import android.os.Bundle;

import org.projectbuendia.client.utils.Utils;

/**
 * A SyncScheduler that schedules sync operations by making requests to Android's
 * SyncAdapter framework and praying that it calls the sync engine... someday.
 * See BuendiaSyncAdapterService, through which the Android system invokes the SyncEngine.
 */
public class SyncAdapterSyncScheduler implements SyncScheduler {
    private static final String KEY_OPTIONS = "OPTIONS";

    private final SyncEngine engine;
    private final Account account;
    private final String authority;

    public SyncAdapterSyncScheduler(SyncEngine engine, Account account, String authority) {
        this.engine = engine;
        this.account = account;
        this.authority = authority;
    }

    public static Bundle getOptions(Bundle extras) {
        return extras.getBundle(KEY_OPTIONS);
    }

    private static Bundle buildExtras(Bundle options) {
        return Utils.putBundle(KEY_OPTIONS, options, new Bundle());
    }

    @Override public void requestSync(Bundle options) {
        Bundle extras = buildExtras(options);
        extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        extras.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        ContentResolver.requestSync(account, authority, extras);
    }

    @Override public void stopSyncing() {
        ContentResolver.cancelSync(account, authority);
    }

    @Override public void setPeriodicSync(int periodSec, Bundle options) {
        if (periodSec > 0) {
            ContentResolver.setIsSyncable(account, authority, 1);
            ContentResolver.setSyncAutomatically(account, authority, true);
            ContentResolver.setMasterSyncAutomatically(true);
            ContentResolver.addPeriodicSync(account, authority, buildExtras(options), periodSec);
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
