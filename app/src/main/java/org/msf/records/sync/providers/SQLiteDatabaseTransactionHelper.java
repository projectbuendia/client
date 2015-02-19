package org.msf.records.sync.providers;

import net.sqlcipher.database.SQLiteStatement;

import org.msf.records.sync.PatientDatabase;

/**
 * Provides helper functions for dealing with savepoints in SQLite databases.
 */
public final class SQLiteDatabaseTransactionHelper {
    private final PatientDatabase mDbHelper;

    SQLiteDatabaseTransactionHelper(PatientDatabase dbHelper) {
        mDbHelper = dbHelper;
    }

    /**
     * Closes the database resources in use by this object.
     */
    public void close() {
        mDbHelper.getWritableDatabase().close();
    }

    /**
     * Starts a named transaction by creating a savepoint with the given name.
     *
     * @see <a>http://www.sqlite.org/lang_savepoint.html</a>.
     */
    public void startNamedTransaction(String savepointName) {
        SQLiteStatement statement =
                mDbHelper.getWritableDatabase().compileStatement("SAVEPOINT " + savepointName);
        statement.execute();
        statement.close();
    }

    /**
     * Rolls back a named transaction by rolling back to a savepoint with the given name.
     *
     * @see <a>http://www.sqlite.org/lang_savepoint.html</a>.
     */
    public void rollbackNamedTransaction(String savepointName) {
        SQLiteStatement statement =
                mDbHelper.getWritableDatabase().compileStatement("ROLLBACK TO " + savepointName);
        statement.execute();
        statement.close();
    }

    /**
     * Releases a named transaction with the given name, removing the savepoint and effectively
     * committing or canceling the transaction, depending on whether or not
     * {@link #rollbackNamedTransaction(String)} was called.
     *
     * @see <a>http://www.sqlite.org/lang_savepoint.html</a>.
     */
    public void releaseNamedTransaction(String savepointName) {
        SQLiteStatement statement =
                mDbHelper.getWritableDatabase().compileStatement("RELEASE " + savepointName);
        statement.execute();
        statement.close();
    }

}
