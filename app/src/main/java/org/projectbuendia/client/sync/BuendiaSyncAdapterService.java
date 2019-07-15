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

package org.projectbuendia.client.sync;

import android.accounts.Account;
import android.app.Service;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.IBinder;

import org.projectbuendia.client.utils.Logger;

/** A service that holds a singleton SyncAdapter and provides it to the OS on request. */
public class BuendiaSyncAdapterService extends Service {
    private static final Logger LOG = Logger.create();
    private static final Object lock = new Object();
    private static SyncAdapter adapter = null;

    public static SyncAdapter getSyncAdapter() {
        return adapter;
    }

    @Override public void onCreate() {
        LOG.i("onCreate");
        super.onCreate();
        synchronized (lock) {
            if (adapter == null) {
                Context context = getApplicationContext();
                adapter = new SyncAdapter(context, new BuendiaSyncEngine(context));
            }
        }
    }

    @Override public IBinder onBind(Intent intent) {
        LOG.i("onBind");
        return adapter.getSyncAdapterBinder();
    }

    /** A wrapper around a SyncEngine that fits the SyncAdapter framework. */
    private static class SyncAdapter extends AbstractThreadedSyncAdapter {
        private final SyncEngine engine;

        public SyncAdapter(Context context, SyncEngine engine) {
            super(context, true);
            this.engine = engine;
        }

        @Override public void onSyncCanceled() {
            engine.cancel();
        }

        @Override public void onPerformSync(Account account, Bundle extras,
            String authority, ContentProviderClient client, SyncResult result) {
            engine.sync(SyncAdapterSyncScheduler.getOptions(extras), client, result);
        }
    }
}
