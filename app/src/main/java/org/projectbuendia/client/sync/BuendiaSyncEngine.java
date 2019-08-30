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

import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.projectbuendia.client.R;
import org.projectbuendia.client.providers.BuendiaProvider;
import org.projectbuendia.client.providers.Contracts.Misc;
import org.projectbuendia.client.providers.Contracts.Bookmarks;
import org.projectbuendia.client.providers.Contracts.Table;
import org.projectbuendia.client.providers.DatabaseTransaction;
import org.projectbuendia.client.sync.SyncManager.SyncStatus;
import org.projectbuendia.client.sync.controllers.ChartsSyncWorker;
import org.projectbuendia.client.sync.controllers.ConceptsSyncWorker;
import org.projectbuendia.client.sync.controllers.FormsSyncWorker;
import org.projectbuendia.client.sync.controllers.LocationsSyncWorker;
import org.projectbuendia.client.sync.controllers.ObservationsSyncWorker;
import org.projectbuendia.client.sync.controllers.OrdersSyncWorker;
import org.projectbuendia.client.sync.controllers.PatientsSyncWorker;
import org.projectbuendia.client.sync.controllers.SyncWorker;
import org.projectbuendia.client.sync.controllers.UsersSyncWorker;
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
        USERS(R.string.syncing_users, new UsersSyncWorker()),
        LOCATIONS(R.string.syncing_locations, new LocationsSyncWorker()),
        PATIENTS(R.string.syncing_patients, new PatientsSyncWorker()),
        OBSERVATIONS(R.string.syncing_observations, new ObservationsSyncWorker()),
        ORDERS(R.string.syncing_orders, new OrdersSyncWorker()),
        CHART_ITEMS(R.string.syncing_charts, new ChartsSyncWorker()),
        FORMS(R.string.syncing_forms, new FormsSyncWorker()),
        CONCEPTS(R.string.syncing_concepts, new ConceptsSyncWorker());

        public final @StringRes int message;
        public final SyncWorker worker;
        public static final Phase[] ALL_PHASES = Phase.values();

        Phase(int message, SyncWorker worker) {
            this.message = message;
            this.worker = worker;
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

        try {
            checkCancellation("before work started");
        } catch (CancellationException e) {
            broadcastSyncStatus(SyncStatus.CANCELLED);
            return;
        }

        List<Phase> phases = getPhases(options);
        boolean fullSync = Sets.newHashSet(phases).equals(Sets.newHashSet(Phase.ALL_PHASES));
        LOG.start("sync", "options = %s", options);

        broadcastSyncProgress(0, 1, R.string.sync_in_progress);

        BuendiaProvider provider = (BuendiaProvider) client.getLocalContentProvider();
        try (DatabaseTransaction tx = provider.startTransaction(SAVEPOINT_NAME)) {
            try {
                if (fullSync) storeFullSyncStartTime(client, Instant.now());
                LOG.elapsed("sync", "Starting phases");
                int completedWork = 0;
                int totalWork = phases.size();

                for (Phase phase : phases) {
                    checkCancellation("before " + phase);
                    broadcastSyncProgress(completedWork, totalWork, phase.message);
                    LOG.i("Start phase: %s", phase);
                    phase.worker.initialize(contentResolver, result, client);
                    boolean done = false;
                    while (!done) {
                        done = phase.worker.sync(contentResolver, result, client);
                        completedWork++;
                        if (!done) {
                            totalWork++;
                            broadcastSyncProgress(completedWork, totalWork, phase.message);
                            checkCancellation("during " + phase);
                        }
                    }
                    phase.worker.finalize(contentResolver, result, client);
                    LOG.elapsed("sync", "Completed phase %s", phase);
                }
                broadcastSyncProgress(1, 1, R.string.completing_sync);
                if (fullSync) storeFullSyncEndTime(client, Instant.now());
            } catch (CancellationException e) {
                LOG.i(e, "Cancelled %s", options);
                tx.rollback();
                // Reset canceled state so that it doesn't interfere with next sync.
                broadcastSyncStatus(SyncStatus.CANCELLED);
                return;
            } catch (OperationApplicationException e) {
                LOG.e(e, "Failed due to database error");
                tx.rollback();
                result.databaseError = true;
                broadcastSyncStatus(SyncStatus.FAILED);
                return;
            } catch (Throwable e) {
                LOG.e(e, "Failed due to exception");
                tx.rollback();
                result.stats.numIoExceptions++;
                broadcastSyncStatus(SyncStatus.FAILED);
                return;
            }
        }
        broadcastSyncStatus(SyncStatus.SUCCEEDED);
        LOG.finish("sync");
        LOG.i("Completed", options);
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
            new Intent(SyncManager.STATUS_ACTION)
                .putExtra(SyncManager.SYNC_STATUS, status)
                .addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        );
    }

    private void broadcastSyncProgress(int numerator, int denominator, @StringRes int messageId) {
        context.sendBroadcast(
            new Intent(SyncManager.STATUS_ACTION)
                .putExtra(SyncManager.SYNC_STATUS, SyncStatus.IN_PROGRESS)
                .putExtra(SyncManager.SYNC_NUMERATOR, numerator)
                .putExtra(SyncManager.SYNC_DENOMINATOR, denominator)
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
        DateTime start = null;
        try (Cursor cursor = provider.query(Misc.URI, null, null, null, null)) {
            if (cursor.moveToNext()) {
                start = Utils.getDateTime(cursor, Misc.FULL_SYNC_START_MILLIS);
            }
        }
        Long value = time.getMillis();
        if (start == null) {
            LOG.e("Database was cleared during sync!");
            value = null;
        }
        LOG.i("Recording full sync end time: " + value);
        ContentValues cv = new ContentValues();
        cv.put(Misc.FULL_SYNC_END_MILLIS, value);
        provider.insert(Misc.URI, cv);
    }

    /** Returns the server timestamp corresponding to the last observation sync. */
    @Nullable
    public static String getBookmark(ContentProviderClient provider, Table table)
            throws RemoteException {
        try (Cursor c = provider.query(
                Bookmarks.URI.buildUpon().appendPath(table.name).build(),
                new String[] {Bookmarks.BOOKMARK}, null, null, null)
        ) {
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

    public static void setBookmark(ContentProviderClient provider, Table table, String bookmark)
            throws RemoteException {
        ContentValues cv = new ContentValues();
        cv.put(Bookmarks.TABLE_NAME, table.name);
        cv.put(Bookmarks.BOOKMARK, bookmark);
        provider.insert(Bookmarks.URI, cv);
    }
}
