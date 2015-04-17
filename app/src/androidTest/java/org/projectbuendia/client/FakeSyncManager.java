// Copyright 2015 The Project Buendia Authors
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not
// use this file except in compliance with the License.  You may obtain a copy
// of the License at: http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distrib-
// uted under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
// OR CONDITIONS OF ANY KIND, either express or implied.  See the License for
// specific language governing permissions and limitations under the License.

package org.projectbuendia.client;

import org.projectbuendia.client.sync.SyncManager;

/**
 * A {@link SyncManager} that does not attempt to sync any content but can be made to appear is if
 * it is syncing.
 */
public class FakeSyncManager extends SyncManager {
    // TODO/refactor: Create common interface between SyncManager and this class.
    public FakeSyncManager() {
        super(null);
    }

    private boolean mSyncing;

    /** Sets whether or not syncing should appear to be occurring. */
    public void setSyncing(boolean syncing) {
        mSyncing = syncing;
    }

    @Override
    public boolean isSyncPending() {
        return false;
    }

    @Override
    public boolean isSyncActive() {
        return mSyncing;
    }

    @Override
    public void startFullSync() {
        mSyncing = true;
    }
}
