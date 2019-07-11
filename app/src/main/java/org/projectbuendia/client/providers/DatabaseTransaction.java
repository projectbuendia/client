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

package org.projectbuendia.client.providers;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.projectbuendia.client.utils.Logger;

/**
 * An AutoCloseable object representing a SQLite transaction.  Instantiating
 * a DatabaseTransaction starts a transaction.  Closing it with close() finishes
 * the transaction, which causes the operations within the transaction to be
 * rolled back if rollback() was called, or committed otherwise.
 */
public final class DatabaseTransaction implements AutoCloseable { // @nolint
    private final SQLiteDatabase mDatabase;
    private final String mName;

    private static final Logger LOG = Logger.create();

    /** Starts a named transaction by creating a savepoint. */
    public DatabaseTransaction(SQLiteDatabase database, String name) {
        mDatabase = database;
        mName = name;
        LOG.d("Starting transaction with SAVEPOINT " + mName);
        mDatabase.execSQL("SAVEPOINT " + mName);
    }

    public DatabaseTransaction(SQLiteOpenHelper openHelper, String name) {
        this(openHelper.getWritableDatabase(), name);
    }

    /** Rolls back a named transaction to the state just after it started. */
    public void rollback() {
        if (mDatabase.inTransaction()) {
            LOG.d("Rolling back transaction with ROLLBACK TO " + mName);
            mDatabase.execSQL("ROLLBACK TO " + mName);
            LOG.d("Rollback to " + mName + " completed");
        } else {
            LOG.w("There is no current transaction to roll back");
        }
    }

    /**
     * Finishes a named transaction.  The transaction will be cancelled if
     * rollback() was called; otherwise it will be committed.
     */
    @Override public void close() {
        mDatabase.execSQL("RELEASE " + mName);
        LOG.d("Finished transaction " + mName);
    }
}
