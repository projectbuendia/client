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

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import org.projectbuendia.client.sync.Database;
import org.projectbuendia.client.sync.QueryBuilder;

/**
 * A {@link ProviderDelegate} that provides query, insert, delete, and update access to a group or
 * list of items provided directly from the database.
 */
class GroupProviderDelegate implements ProviderDelegate<Database> {

    private static final String BULK_INSERT_SAVEPOINT = "GROUP_PROVIDER_DELEGATE_BULK_INSERT";
    private final String mType;
    private final Contracts.Table mTable;

    public GroupProviderDelegate(String type, Contracts.Table table) {
        mType = type;
        mTable = table;
    }

    @Override public String getType() {
        return mType;
    }

    @Override public Cursor query(
        Database dbHelper, ContentResolver contentResolver, Uri uri,
        String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Cursor cursor = new QueryBuilder(mTable).where(selection, selectionArgs)
            .orderBy(sortOrder)
            .select(dbHelper.getReadableDatabase(), projection);
        cursor.setNotificationUri(contentResolver, uri);
        return cursor;
    }

    @Override public Uri insert(
        Database dbHelper, ContentResolver contentResolver, Uri uri,
        ContentValues values) {
        long id = dbHelper.getWritableDatabase().replaceOrThrow(mTable.name, null, values);
        contentResolver.notifyChange(uri, null, false);
        return uri.buildUpon().appendPath(Long.toString(id)).build();
    }

    @Override public int bulkInsert(
        Database dbHelper, ContentResolver contentResolver, Uri uri,
        ContentValues[] allValues) {
        if (allValues.length == 0) {
            return 0;
        }
        final SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues first = allValues[0];
        String[] columns = first.keySet().toArray(new String[first.size()]);
        try (DatabaseTransaction tx = new DatabaseTransaction(db, BULK_INSERT_SAVEPOINT)) {
            SQLiteStatement statement = makeInsertStatement(db, mTable.name, columns);
            try {
                Object[] bindings = new Object[first.size()];
                for (ContentValues values : allValues) {
                    statement.clearBindings();
                    if (values.size() != first.size()) {
                        throw new AssertionError();
                    }
                    for (int i = 0; i < bindings.length; i++) {
                        Object value = values.get(columns[i]);
                        // This isn't super safe, but is in our context.
                        int bindingIndex = i + 1;
                        if (value instanceof String) {
                            statement.bindString(bindingIndex, (String) value);
                        } else if ((value instanceof Long) || value instanceof Integer) {
                            statement.bindLong(bindingIndex, ((Number) value).longValue());
                        } else if ((value instanceof Double) || value instanceof Float) {
                            statement.bindDouble(bindingIndex, ((Number) value).doubleValue());
                        }
                        bindings[i] = value;
                    }
                    statement.executeInsert();
                }
            } catch (Throwable t) {
                // If absolutely anything goes wrong, rollback to the savepoint.
                tx.rollback();
            } finally {
                statement.close();
            }
        }
        contentResolver.notifyChange(uri, null, false);
        return allValues.length;
    }

    private SQLiteStatement makeInsertStatement(
        SQLiteDatabase db, String table, String[] columns) {
        // I kind of hoped this would be provided by SQLiteDatabase or DatabaseHelper,
        // But it doesn't seem to be. Innards copied from SQLiteDatabase.insertWithOnConflict
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT OR REPLACE ");
        sql.append(" INTO ");
        sql.append(table);
        sql.append('(');

        int size = (columns != null && columns.length > 0) ? columns.length : 0;
        if (size <= 0) {
            throw new IllegalArgumentException();
        }
        for (int i = 0; i < columns.length; i++) {
            sql.append((i > 0) ? "," : "");
            sql.append(columns[i]);
        }
        sql.append(')');
        sql.append(" VALUES (");
        for (int i = 0; i < size; i++) {
            sql.append((i > 0) ? ",?" : "?");
        }
        sql.append(')');

        return db.compileStatement(sql.toString());
    }

    @Override public int delete(
        Database dbHelper, ContentResolver contentResolver, Uri uri,
        String selection, String[] selectionArgs) {
        int count = new QueryBuilder(mTable)
            .where(selection, selectionArgs)
            .delete(dbHelper.getWritableDatabase());
        contentResolver.notifyChange(uri, null, false);
        return count;
    }

    @Override public int update(
        Database dbHelper, ContentResolver contentResolver, Uri uri,
        ContentValues values, String selection, String[] selectionArgs) {
        int count = new QueryBuilder(mTable)
            .where(selection, selectionArgs)
            .update(dbHelper.getWritableDatabase(), values);
        contentResolver.notifyChange(uri, null, false);
        return count;
    }
}
