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

package org.msf.records.sync.providers;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import org.msf.records.sync.PatientDatabase;
import org.msf.records.sync.SelectionBuilder;

/**
 * A {@link ProviderDelegate} that provides query, delete, and update access to a single item
 * provided directly from the database.
 */
public class ItemProviderDelegate implements ProviderDelegate<PatientDatabase> {

    protected final String mName;
    protected final String mTableName;
    protected final String mType;
    protected final String mIdColumn;

    /**
     * Creates an instance of {@link ItemProviderDelegate}.
     */
    public ItemProviderDelegate(String name, String tableName, String idColumn) {
        mName = name;
        mTableName = tableName;
        mIdColumn = idColumn;
        mType = ContentResolver.CURSOR_ITEM_BASE_TYPE + TYPE_PACKAGE_PREFIX + mName;
    }

    @Override
    public String getType() {
        return mType;
    }

    @Override
    public Cursor query(
            PatientDatabase dbHelper, ContentResolver contentResolver, Uri uri,
            String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SelectionBuilder builder = new SelectionBuilder()
                .table(mTableName)
                .where(mIdColumn + "=?", uri.getLastPathSegment())
                .where(selection, selectionArgs);
        Cursor cursor = builder.query(dbHelper.getReadableDatabase(), projection, sortOrder);
        cursor.setNotificationUri(contentResolver, uri);
        return cursor;
    }

    @Override
    public Uri insert(
            PatientDatabase dbHelper, ContentResolver contentResolver, Uri uri,
            ContentValues values) {
        throw new UnsupportedOperationException("Insert is not supported for URI '" + uri + "'.");
    }

    @Override
    public int bulkInsert(
            PatientDatabase dbHelper, ContentResolver contentResolver, Uri uri,
            ContentValues[] allValues) {
        throw new UnsupportedOperationException(
                "Bulk insert is not supported for URI '" + uri + "'.");
    }

    @Override
    public int delete(
            PatientDatabase dbHelper, ContentResolver contentResolver, Uri uri,
            String selection, String[] selectionArgs) {
        int count = new SelectionBuilder()
                .table(mTableName)
                .where(mIdColumn + "=?", uri.getLastPathSegment())
                .where(selection, selectionArgs)
                .delete(dbHelper.getWritableDatabase());
        contentResolver.notifyChange(uri, null, false);
        return count;
    }

    @Override
    public int update(
            PatientDatabase dbHelper, ContentResolver contentResolver, Uri uri,
            ContentValues values, String selection, String[] selectionArgs) {
        int count = new SelectionBuilder()
                .table(mTableName)
                .where(mIdColumn + "=?", uri.getLastPathSegment())
                .where(selection, selectionArgs)
                .update(dbHelper.getWritableDatabase(), values);
        contentResolver.notifyChange(uri, null, false);
        return count;
    }
}
