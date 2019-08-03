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

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.database.Cursor;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;

import org.joda.time.Instant;
import org.projectbuendia.client.App;
import org.projectbuendia.client.R;
import org.projectbuendia.client.providers.BuendiaProvider;
import org.projectbuendia.client.providers.Contracts;
import org.projectbuendia.client.providers.Contracts.Misc;
import org.projectbuendia.client.providers.Contracts.SyncTokens;
import org.projectbuendia.client.providers.DatabaseTransaction;
import org.projectbuendia.client.sync.SyncManager.SyncStatus;
import org.projectbuendia.client.sync.controllers.ChartsSyncPhaseRunnable;
import org.projectbuendia.client.sync.controllers.ConceptsSyncPhaseRunnable;
import org.projectbuendia.client.sync.controllers.FormsSyncPhaseRunnable;
import org.projectbuendia.client.sync.controllers.LocationsSyncPhaseRunnable;
import org.projectbuendia.client.sync.controllers.ObservationsSyncPhaseRunnable;
import org.projectbuendia.client.sync.controllers.OrdersSyncPhaseRunnable;
import org.projectbuendia.client.sync.controllers.PatientsSyncPhaseRunnable;
import org.projectbuendia.client.sync.controllers.SyncPhaseRunnable;
import org.projectbuendia.client.sync.controllers.UsersSyncPhaseRunnable;
import org.projectbuendia.client.utils.Logger;
import org.projectbuendia.client.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;

/** Implementation of sync operations for the Buendia database. */
public class BuendiaSyncEngine implements SyncEngine {
    private static final Logger LOG = Logger.create();
    private static final String SAVEPOINT_NAME = "SYNC_SAVEPOINT";
    private static final String KEY_PHASES = "PHASES";

    private final Context context;
    private final ContentResolver contentResolver;
    private boolean isCancelled = false;

    /** The available phases, in the default order in which to run them. */
    public enum Phase {
        USERS(R.string.syncing_users, new UsersSyncPhaseRunnable()),
        OBSERVATIONS(R.string.syncing_observations, new ObservationsSyncPhaseRunnable()),
        ORDERS(R.string.syncing_orders, new OrdersSyncPhaseRunnable()),
        PATIENTS(R.string.syncing_patients, new PatientsSyncPhaseRunnable()),
        LOCATIONS(R.string.syncing_locations, new LocationsSyncPhaseRunnable()),
        CHART_ITEMS(R.string.syncing_charts, new ChartsSyncPhaseRunnable()),
        FORMS(R.string.syncing_forms, new FormsSyncPhaseRunnable()),
        CONCEPTS(R.string.syncing_concepts, new ConceptsSyncPhaseRunnable());

        public final @StringRes int message;
        public final SyncPhaseRunnable runnable;
        public static final Phase[] ALL = Phase.values();

        Phase(int message, SyncPhaseRunnable runnable) {
            this.message = message;
            this.runnable = runnable;
        }
    }

    public BuendiaSyncEngine(Context context) {
        this.context = context;
        this.contentResolver = context.getContentResolver();
    }

    @Override public void cancel() {
        isCancelled = true;
        LOG.i("Received a cancel() request");
    }

    public static Bundle buildOptions(Phase... phases) {
        return Utils.putString(KEY_PHASES, Joiner.on(",").join(phases), new Bundle());
    }

    public static @NonNull List<Phase> getPhases(Bundle options) {
        List<Phase> phases = new ArrayList<>();
        if (options != null) {
            for (String name : options.getString(KEY_PHASES, "").split(",")) {
                try {
                    phases.add(Phase.valueOf(name));
                } catch (IllegalArgumentException e) {
                    LOG.w("Unrecognized phase name: %s (in options: %s)", Utils.repr(name), options);
                }
            }
        }
        return phases;
    }

    /** Not thread-safe but, by default, this will never be called multiple times in parallel. */
    @Override public void sync(Bundle options, ContentProviderClient client, SyncResult result) {
        isCancelled = false;

        // If we can't access the Buendia API, short-circuit. Before this check was added, sync
        // would occasionally hang indefinitely when wifi is unavailable. As a side effect of this
        // change, however, any user-requested sync will instantly fail until the HealthMonitor has
        // made a determination that the server is definitely accessible.
        if (App.getInstance().getHealthMonitor().isApiUnavailable()) {
            LOG.e("Abort sync: Buendia API is unavailable.");
            broadcastSyncStatus(SyncStatus.FAILED);
            return;
        }

        try {
            checkCancellation("before work started");
        } catch (CancellationException e) {
            broadcastSyncStatus(SyncStatus.CANCELLED);
            return;
        }

        List<Phase> phases = getPhases(options);
        boolean fullSync = Sets.newHashSet(phases).equals(Sets.newHashSet(Phase.ALL));
        LOG.start("sync", "options = %s", options);

        broadcastSyncProgress(0, R.string.sync_in_progress);

        BuendiaProvider provider = (BuendiaProvider) client.getLocalContentProvider();
        try (DatabaseTransaction tx = provider.startTransaction(SAVEPOINT_NAME)) {
            try {
                if (fullSync) {
                    storeFullSyncStartTime(client, Instant.now());
                }
                LOG.elapsed("sync", "Starting phases");
                int p = 0;
                for (Phase phase : phases) {
                    checkCancellation("before " + phase);
                    broadcastSyncProgress(p * 100 / phases.size(), phase.message);
                    phase.runnable.sync(contentResolver, result, client);
                    LOG.elapsed("sync", "Completed phase %s", phase);
                    p++;
                }
                broadcastSyncProgress(100, R.string.completing_sync);
                if (fullSync) {
                    storeFullSyncEndTime(client, Instant.now());
                }
            } catch (CancellationException e) {
                LOG.i(e, "Sync canceled");
                tx.rollback();
                // Reset canceled state so that it doesn't interfere with next sync.
                broadcastSyncStatus(SyncStatus.CANCELLED);
                return;
            } catch (OperationApplicationException e) {
                LOG.e(e, "Error updating database during sync");
                tx.rollback();
                result.databaseError = true;
                broadcastSyncStatus(SyncStatus.FAILED);
                return;
            } catch (Throwable e) {
                LOG.e(e, "Error during sync");
                tx.rollback();
                result.stats.numIoExceptions++;
                broadcastSyncStatus(SyncStatus.FAILED);
                return;
            }
        }
        broadcastSyncStatus(SyncStatus.COMPLETED);
        LOG.finish("sync");
    }

    /**
     * Enforces sync cancellation, throwing a {@link CancellationException} if the sync has been
     * canceled. It is the responsibility of the caller to perform any actual cancellation
     * procedures.
     */
    private synchronized void checkCancellation(String when) throws CancellationException {
        if (isCancelled) {
            isCancelled = false;
            String message = "Sync cancelled " + when;
            LOG.w(message);
            throw new CancellationException(message);
        }
    }

    private void broadcastSyncStatus(SyncStatus status) {
        context.sendBroadcast(
            new Intent(context, SyncManager.SyncStatusBroadcastReceiver.class)
                .putExtra(SyncManager.SYNC_STATUS, status)
                .addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        );
    }

    private void broadcastSyncProgress(int progress, @StringRes int messageId) {
        context.sendBroadcast(
            new Intent(context, SyncManager.SyncStatusBroadcastReceiver.class)
                .putExtra(SyncManager.SYNC_STATUS, SyncStatus.IN_PROGRESS)
                .putExtra(SyncManager.SYNC_PROGRESS, progress)
                .putExtra(SyncManager.SYNC_MESSAGE_ID, messageId)
                .addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        );
    }

    private void storeFullSyncStartTime(ContentProviderClient provider, Instant time) throws RemoteException {
        LOG.i("Recording full sync start time: " + time);
        ContentValues cv = new ContentValues();
        cv.put(Misc.FULL_SYNC_START_MILLIS, time.getMillis());
        provider.insert(Misc.URI, cv);
    }

    private void storeFullSyncEndTime(ContentProviderClient provider, Instant time) throws RemoteException {
        LOG.i("Recording full sync end time: " + time);
        ContentValues cv = new ContentValues();
        cv.put(Misc.FULL_SYNC_END_MILLIS, time.getMillis());
        provider.insert(Misc.URI, cv);
    }

    /** Returns the server timestamp corresponding to the last observation sync. */
    @Nullable
    public static String getLastSyncToken(ContentProviderClient provider, Contracts.Table table)
            throws RemoteException {
        try(Cursor c = provider.query(
                SyncTokens.URI.buildUpon().appendPath(table.name).build(),
                new String[] {SyncTokens.SYNC_TOKEN}, null, null, null)) {
            // Make the linter happy, there's no way that the cursor can be null without throwing
            // an exception.
            assert c != null;
            if (c.moveToNext()) {
                // Whether c.getString is null or not is implementation-defined, so we explicitly
                // check for nullness.
                if (c.isNull(0)) {
                    return null;
                }
                return c.getString(0);
            } else {
                return null;
            }
        }
    }

    public static void storeSyncToken(
            ContentProviderClient provider, Contracts.Table table, String syncToken)
            throws RemoteException {
        ContentValues cv = new ContentValues();
        cv.put(SyncTokens.TABLE_NAME, table.name);
        cv.put(SyncTokens.SYNC_TOKEN, syncToken);
        provider.insert(SyncTokens.URI, cv);
    }
}
