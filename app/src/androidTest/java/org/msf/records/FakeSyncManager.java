package org.msf.records;

import org.msf.records.sync.SyncManager;

/**
 * A {@link SyncManager} that does not attempt to sync any content but can be made to appear is if
 * it is syncing.
 */
public class FakeSyncManager extends SyncManager {
    public FakeSyncManager() { }

    private boolean mSyncing;

    /** Sets whether or not syncing should appear to be occurring. */
    public void setSyncing(boolean syncing) {
        mSyncing = syncing;
    }

    @Override
    public boolean isSyncing() {
        return mSyncing;
    }

    @Override
    public void forceSync() {
        mSyncing = true;
    }
}
